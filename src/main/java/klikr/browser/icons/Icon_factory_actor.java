// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES animated_gifs/Ffmpeg_utils.java
//SOURCES ../Image_and_properties.java
//SOURCES ../items/Iconifiable_item_type.java
//SOURCES ../../util/files_and_paths/Static_files_and_paths_utilities.java
//SOURCES ../../images/decoding/Fast_image_property_from_exif_metadata_extractor.java
//SOURCES ../../util/execute/Execute_command.java
//SOURCES animated_gifs/Animated_gif_from_folder_content.java

package klikr.browser.icons;

import javafx.scene.image.Image;
import javafx.stage.Window;
import klikr.properties.String_constants;
import klikr.properties.boolean_features.Feature;
import klikr.properties.boolean_features.Feature_cache;
import klikr.util.External_application;
import klikr.look.Jar_utils;
import klikr.util.cache.Klikr_cache;
import klikr.util.execute.Execute_result;
import klikr.util.execute.actor.*;
import klikr.util.animated_gifs.Ffmpeg_utils;
import klikr.browser.Image_and_properties;
import klikr.browser.icons.image_properties_cache.*;
import klikr.browser.items.Iconifiable_item_type;
import klikr.properties.boolean_features.Booleans;
import klikr.util.cache.Cache_folder;
import klikr.properties.Non_booleans_properties;
import klikr.util.image.Full_image_from_disk;
import klikr.util.image.Icons_from_disk;
import klikr.util.execute.Execute_command;
import klikr.util.image.Static_image_utilities;
import klikr.util.image.icon_cache.Icon_caching;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.mmap.Mmap;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


// an actor-style asynchronous icon factory
//**********************************************************
public class Icon_factory_actor implements Actor
//**********************************************************
{

    private static final boolean cache_png_in_mmap = Feature_cache.get(Feature.Enable_mmap_caching);
    // there are 3 possibilities for caching PNG icons (of JPGs):
    // - if cache_png_in_mmap is false,
    // for jpg, png and pdf, we store the icons in a cache folder:
    // - if cache_png_in_mmap is true
    // for jpg, png and pdf, we store the icons in mmap
    // - if use_pixels_in_mmap is true, we store PIXELS in mmap
    // (takes more RAM/file but speed is unbeatable)
    // - if use_pixels_in_mmap is false, we store the PNG files in mmap
    // ... we do the job the OS should normally do i.e. for optimal disk reading
    // speed the OS maps the file in virtual memory too, but here we bypass
    // the (complex!) OS decision process about when to map or not
    private static final boolean use_pixels_in_mmap = true;
    // not meaningful if cache_png_in_mmap is false
    // when use_pixels_in_mmap = true, png icon read is 10 times FASTER (pixels versus file)

    // for GIF, and especially animated gifs, which can be quite large...
    // if cache_gifs_as_files_in_mmap is false, we do not cache GIFS at all,
    // the icon is made from the original file ...which is slow
    // if cache_gifs_as_files_in_mmap is  true, we store the icon FILE in mmap,
    // and then animated gifs are loaded much faster ... but the RAM+file cost is high
    private static final boolean cache_gifs_as_files_in_mmap = Feature_cache.get(Feature.Enable_mmap_caching);


    private static final boolean dbg = false;
    private static final boolean verbose_dbg = false;
    private static final boolean pdf_dbg = false;
    private static final boolean aborting_dbg = false;

    Logger logger;
    Icon_writer_actor writer;
    private final Window owner;
    private final Aborter aborter;
    private final Klikr_cache<Path, Image_properties> image_properties_cache;

    private final Path icon_cache_dir;

    //**********************************************************
    public Icon_factory_actor(
            Klikr_cache<Path, Image_properties> image_properties_cache,
            Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.image_properties_cache = image_properties_cache;
        this.aborter = aborter;
        this.owner = owner;
        this.logger = logger;
        if (dbg) logger.log("✅ Icon_factory actor created");
        icon_cache_dir = Cache_folder.get_cache_dir( Cache_folder.icon_cache,owner,logger);
        writer = new Icon_writer_actor(icon_cache_dir, owner, logger);
    }


    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Icon_factory_actor";
    }


    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Icon_factory_request icon_factory_request = (Icon_factory_request) m;
        if (dbg) logger.log("✅ Icon caching, icon request processing starts ");

        Icon_destination destination = icon_factory_request.destination;
        if (destination == null) {
            logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN icon factory : cancel! destination==null"));
            return null;
        }

        Optional<Image_and_properties> image_and_properties = Optional.empty();
        for(;;)
        {
            image_and_properties = do_it(icon_factory_request, destination);
            if ( image_and_properties.isEmpty())
            {
                // = no retry
                return "no icon";
            }
            if (image_and_properties.get().image() != null) {
                double w = image_and_properties.get().image().getWidth();
                double h = image_and_properties.get().image().getHeight();
                if ((w > 0.0)&&(h > 0.0)) {
                    // SUCCESS!!
                    break;
                }
            }
            logger.log("Icon caching, something went wrong, image w = "+image_and_properties.get().image().getWidth()+", h = "+image_and_properties.get().image().getHeight());

            // retry a few times
            if (icon_factory_request.retry_count < Icon_factory_request.max_retry)
            {
                icon_factory_request.retry_count++;
                logger.log("❗ Icon caching, RETRYING : " + icon_factory_request.retry_count + " times, after empty icon for : " + destination.get_item_path() );
                try {
                    Thread.sleep(100*icon_factory_request.retry_count);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
            logger.log("❗ Icon caching, too many retries after empty icon for : " + destination.get_item_path() );
            return "icon failed";
        }

        if ( dbg) check(image_and_properties.get());

        destination.receive_icon(image_and_properties.get());
        Image_properties image_properties = image_and_properties.get().properties();
        if ( image_properties != null)
        {
            image_properties_cache.inject(destination.get_item_path(),image_properties,false);
        }
        //logger.log("Icon_factory_actor: "+ instance.decrementAndGet());

        return "icon done";
    }

    //**********************************************************
    private void check(Image_and_properties image_and_properties)
    //**********************************************************
    {
        if ( image_and_properties.image() == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❗ Icon caching, image_and_properties.image() == null"));
            return;
        }
        if ( image_and_properties.properties() == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❗ Icon caching, image_and_properties.properties() == null"));
            return;
        }
        if ( image_and_properties.properties().rotation() == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❗ Icon caching, image_and_properties.properties().rotation() == null"));
        }
    }


    /*
    we cache icons to make folder navigation faster
    - if cache_png_in_mmap is false : cache to file
    - if cache_png_in_mmap is true: cache to mmap 
    generally much faster ...
    if use_pixels_in_mmap is true, we cache (decoded image) PIXELS directly
    => blazing fast load, at a high RAM/file cost

    javafx Image is capable of loading ONLY jpeg, gif, animated gif, png
    ... good news: it can resize (e.g. to icon size)

    javafx does not have an image-writing capability, the easy way is to use ImageIO,
    but it involves AWT dependencies, which is not supported with GraalVM/native

    so the ONLY image writing capability in Klikr is a PNG writer,
    used only for caching JPG icons on disk, when caching on disk

    The situation is frankly complex:

    for JPG, we make an icon using read_original_image_from_disk_and_return_icon
        So it is 'directly' a javaFX Image for the icon
        If cache_png_in_mmap is false, we WRITE a .PNG file in the cache folder
        If cache_png_in_mmap is true, we write PIXELS in Mmap

    for the other image formats we use GraphicsMagick, or ImageMagick
    if GraphicsMagick failed, to convert file-to-file to PNG, that javaFX can load
    (slow ...) so the icon is also PNG

    for PDF, we use GraphicsMagick to WRITE a PNG of the first page using
        So we then need to load that PNG into a javaFX Image for the icon
        If cache_png_in_mmap is false, nothing else to do, 
            since we already have the file in the cache folder
        If cache_png_in_mmap is true, we can write either the PIXELS or
            the .PNG AS A FILE in Mmap
            and we delete the .PNG in the cache folder

    for video, we use FFMPEG to WRITE an animated GIF
        So we then need to load that GIF into a javaFX Image for the icon
        If cache_png_in_mmap is false, nothing else to do, 
            since we already have the file in the cache folder
        If cache_png_in_mmap is true, we write the .GIF file in Mmap
            we do not have the option to write PIXELS because javaFX does not
            support PixelReader for multi-frame GIFs
            and we delete the .GIF in the cache folder

    for GIF we make an icon just like for JPG, but we produce a GIF (not a PNG) so that
    for animated GIFs the icon is also animated !

        If cache_png_in_mmap is false, we do nothing
            - for single GIF the gain is too small,
            - for animated, we cannot write the icon
            this is why browsing folders with many animated GIFS used to be slow
        If cache_png_in_mmap is true,
            if cache_gifs_as_files_in_mmap is true, we copy the whole .GIF file in Mmap
            however, this means that we cannot use a resized version!
            (for which we cannot make a file)
            this is why browsing folders with many animated GIFS is fast
            but the RAM+file cost is high


     */

    //**********************************************************
    private Optional<Image_and_properties> do_it(
            Icon_factory_request icon_factory_request,
            Icon_destination destination)
    //**********************************************************
    {
        if (verbose_dbg) logger.log("✅ Icon caching, " + destination.get_item_path().toAbsolutePath());

        String icon_filename_adder = "";
        Iconifiable_item_type item_type = destination.get_item_type();
        if ( item_type == Iconifiable_item_type.video)
        {
            double length = Non_booleans_properties.get_animated_gif_duration_for_a_video(icon_factory_request.owner);
            icon_filename_adder = "_"+length;
        }
        String extension;
        switch(item_type)
        {
            case gif, video:
                extension = Icon_caching.gif_extension;
                break;
            default:
                extension = Icon_caching.png_extension;
                break;
        }
        Path cache_key = Icon_caching.path_for_icon_caching(destination.get_path_for_display_icon_destination(), String.valueOf(icon_factory_request.icon_size)+icon_filename_adder, extension, owner, logger);
        //logger.log("cache_key= "+cache_key);


        // try to read the icon from the cache

        Image image_from_cache = null;
        switch (item_type)
        {
            case gif:
                {
                    if (cache_gifs_as_files_in_mmap)
                    {
                        image_from_cache = Mmap.instance.read_image_as_file(destination.get_path_for_display_icon_destination());
                        if( dbg) logger.log("Icon caching, READ of GIF full image from mmap, as file");
                    }
                    else
                    {
                        // read it directly!
                        Optional<Image> op = Icons_from_disk.read_original_image_from_disk_and_return_icon(destination.get_path_for_display_icon_destination(), item_type, icon_factory_request.icon_size, true, icon_factory_request.owner, icon_factory_request.aborter, logger);
                        if ( op.isPresent()) image_from_cache = op.get();
                        if( dbg) logger.log("Icon caching, READ of GIF full image from disk");
                    }
                }
                break;
            case video:
                {
                    image_from_cache = Mmap.instance.read_image_as_file(cache_key);
                    if( dbg) logger.log("Icon caching, READ of icon from mmap, as file");
                }
                break;
            default:
                if (cache_png_in_mmap)
                {
                    if (use_pixels_in_mmap) {
                        image_from_cache = Mmap.instance.read_image_as_pixels(cache_key.toAbsolutePath().toString());
                        if( dbg) logger.log("Icon caching, READ of icon from mmap, as pixels");
                    }
                    else
                    {
                        image_from_cache = Mmap.instance.read_image_as_file(cache_key);
                        if( dbg) logger.log("Icon caching, READ of icon from mmap, as file");
                    }
                }
                else
                {
                    image_from_cache = Icons_from_disk.load_icon_from_disk_cache(
                            destination.get_path_for_display_icon_destination(),
                            icon_factory_request.icon_size,
                            String.valueOf(icon_factory_request.icon_size),
                            Icon_caching.png_extension,
                            false, icon_factory_request.owner,logger);
                    if( dbg) logger.log("Icon caching, READ of icon from disk");
                }
                break;
        }

        if (icon_factory_request.aborter.should_abort()) {
            if (aborting_dbg) logger.log("❗ Icon_factory_actor do_it aborting4");
            return Optional.empty();
        }
        if (image_from_cache != null)
        {
            if (dbg)
                logger.log("✅ Icon caching, READ icon from cache(): " + destination.get_item_path()+ " w="+image_from_cache.getWidth()+" h="+image_from_cache.getHeight());

            Image_properties properties = new Image_properties(image_from_cache.getWidth(), image_from_cache.getHeight(), Rotation.normal);
            return Optional.of(new Image_and_properties(image_from_cache, properties));
        }


        // the icon was not in the cache, let us MAKE one


        if (dbg)
            logger.log("❗ Icon caching, attempt to READ icon from cache FAILED for " + destination.get_item_path());

        if (icon_factory_request.aborter.should_abort()) {
            if (aborting_dbg) logger.log("❗ Icon caching, aborting5");
            return Optional.empty();
        }


        switch ( item_type )
        {
            case video: {
                // we are going to create an animated gif using ffmpeg
                double length = Non_booleans_properties.get_animated_gif_duration_for_a_video(icon_factory_request.owner);
                double skip = 0;
                {
                    Double duration_in_seconds = Ffmpeg_utils.get_media_duration(destination.get_item_path(), icon_factory_request.owner, logger);
                    if (duration_in_seconds != null) {
                        if (duration_in_seconds > 3 * 3600) {
                            logger.log("❗ WARNING: Icon caching, ffprobe reports duration that looks wrong");
                            duration_in_seconds = 1800.0;
                        }

                        if (duration_in_seconds < 0) {
                            duration_in_seconds = length;
                        }

                        if (duration_in_seconds < length) {
                            length = duration_in_seconds;
                        } else {
                            // jump to the middle of the movie
                            skip = duration_in_seconds / 2 - length;
                        }
                    }
                }


                Ffmpeg_utils.video_to_gif(
                        destination.get_item_path(),
                        icon_factory_request.icon_size,
                        10,
                        cache_key,
                        length,
                        skip,
                        0,
                        icon_factory_request.get_aborter(), icon_factory_request.owner, logger);
                if (cache_key.toFile().length() == 0) {
                    logger.log("❗Icon caching, icon file empty " + cache_key.toAbsolutePath());
                    return Optional.empty();
                }
            }
            break;

            case pdf:
            {
                // we are going to create a png of the front page using GraphicsMagick (gm)
                File file_in = destination.get_item_path().toFile();
                if (icon_factory_request.aborter.should_abort())
                {
                    if ( aborting_dbg) logger.log("❗ Icon_factory_actor aborting9");
                    return Optional.empty();
                }
                {
                    // gm convert -density 300 -resize 256x256 -quality 90 input.pdf output.png
                    List<String> command_line_for_GraphicsMagic = new ArrayList<>();
                    command_line_for_GraphicsMagic.add(External_application.GraphicsMagick.get_command(owner,logger));
                    command_line_for_GraphicsMagic.add("convert");
                    command_line_for_GraphicsMagic.add("-density");
                    command_line_for_GraphicsMagic.add("300");
                    command_line_for_GraphicsMagic.add("-resize");
                    command_line_for_GraphicsMagic.add(""+icon_factory_request.icon_size+"x"+icon_factory_request.icon_size);
                    command_line_for_GraphicsMagic.add("-quality");
                    command_line_for_GraphicsMagic.add("90");
                    command_line_for_GraphicsMagic.add(file_in.getAbsolutePath().toString());
                    command_line_for_GraphicsMagic.add(cache_key.toAbsolutePath().toString());
                    StringBuilder sb = null;
                    if ( pdf_dbg) sb = new StringBuilder();
                    File wd = file_in.getParentFile();
                    Execute_result res = Execute_command.execute_command_list(command_line_for_GraphicsMagic, wd, 2000, sb,logger);
                    if ( !res.status())
                    {
                        List<String> verify = new ArrayList<>();
                        verify.add(External_application.GraphicsMagick.get_command(owner,logger));
                        verify.add("--version");
                        String home = System.getProperty(String_constants.USER_HOME);
                        Execute_result res2 = Execute_command.execute_command_list(verify, new File(home), 20 * 1000, null, logger);
                        if ( !res2.status())
                        {
                            Booleans.manage_show_graphicsmagick_install_warning(owner,logger);
                        }
                        return Optional.empty();
                    }
                    if ( pdf_dbg) logger.log(sb.toString());
                    if (cache_key.toFile().length() == 0) {
                        logger.log("❗ icon file empty " + cache_key.toAbsolutePath());
                        return Optional.empty();
                    }
                }

                if (pdf_dbg) logger.log("✅ Icon caching, png icon for PDF write done (2)");
            }
            break;

            case gif:
            {
                if (cache_gifs_as_files_in_mmap)
                {
                    // dont resize, to get the full icon ALSO the first time
                    // since we write the original file in mmap
                    // => fast rich solution, dangerous for machines with little RAM
                    Optional<Image> op = Full_image_from_disk.load_native_resolution_image_from_disk(destination.get_path_for_display_icon_destination(),true,owner,aborter,logger);
                    if ( op.isPresent()) image_from_cache = op.get();
                }
                else
                {
                    // resize to icon target size (saves RAM)
                    Optional<Image> op = Icons_from_disk.read_original_image_from_disk_and_return_icon(destination.get_path_for_display_icon_destination(), item_type, icon_factory_request.icon_size, true, icon_factory_request.owner, icon_factory_request.aborter, logger);
                    if ( op.isPresent()) image_from_cache = op.get();
                }
                if ( dbg) logger.log("Icon caching, MADE icon for "+destination.get_path_for_display_icon_destination()+" w="+image_from_cache.getWidth()+" h="+image_from_cache.getHeight());
            }
            break;

            default: {
                Optional<Image> op = Icons_from_disk.read_original_image_from_disk_and_return_icon(destination.get_path_for_display_icon_destination(), item_type, icon_factory_request.icon_size, true, icon_factory_request.owner, icon_factory_request.aborter, logger);
                if ( op.isPresent()) image_from_cache = op.get();
                if ( dbg) logger.log("Icon caching, MADE icon for:"+ destination.get_path_for_display_icon_destination()+" w="+image_from_cache.getWidth()+" h="+image_from_cache.getHeight());
            }
            break;
        }

        if (icon_factory_request.aborter.should_abort()) {
            if (aborting_dbg) logger.log("❗ Icon caching, aborting6");
            return Optional.empty();
        }

        if ( image_from_cache == null) {
            // if needed i.e for pdf=>png and video=>animated-gif, load the generated image
            image_from_cache = Icons_from_disk.load_icon(cache_key, logger);
            if (image_from_cache == null) {
                logger.log("❗ Icon caching, load from file FAILED (5) for " + destination.get_item_path());
                return Optional.empty();
            }
        }

        if ((image_from_cache.getHeight() == 0) && (image_from_cache.getWidth() == 0)) {
            logger.log("❗ Icon caching, load from file FAILED (6) for " + destination.get_item_path());
            return Optional.empty();
        }


        if (icon_factory_request.aborter.should_abort()) {
            if (aborting_dbg) logger.log("❗ Icon_factory_actor do_it aborting7");
            return Optional.empty();
        }

        Runnable delete_on_end = ()->{
            try
            {
                Files.delete(cache_key);
            }
            catch(IOException e)
            {
                logger.log(""+e);
            }
        };
        // write the icon in the cache, using another thread
        Image finalImage_from_cache = image_from_cache;
        Runnable icon_writer = () -> {
            switch (item_type) {
                case video:
                    {
                        Mmap.instance.write_image_as_file(cache_key, true, delete_on_end);
                        if ( dbg) logger.log("Icon caching, WROTE icon as file to disk :" + cache_key + " w=" + finalImage_from_cache.getWidth() + " h=" + finalImage_from_cache.getHeight());
                    }
                    break;

                case gif:
                    if (cache_gifs_as_files_in_mmap)
                    {
                        Mmap.instance.write_image_as_file(destination.get_path_for_display_icon_destination(), true, null);
                        if ( dbg) logger.log("Icon caching, WROTE GIF as file to disk :" + destination.get_path_for_display_icon_destination() + " w=" + finalImage_from_cache.getWidth() + " h=" + finalImage_from_cache.getHeight());
                    }
                    break;

                default:
                    if (destination.get_path_for_display_icon_destination().getParent().toAbsolutePath().toString().equals(icon_cache_dir.toAbsolutePath().toString()))
                    {
                        // the user is browsing the icon cache. if we save a file for the icon, it will trigger the change detector ...
                        // so a new icon request...  ad nauseam ! ==> storm avoidance = dont save the icon.
                        if (dbg)
                            logger.log("✅ Icon_factory thread: (storm avoidance) not saving the icon for a file which is in the icons' cache folder " + cache_key);
                        break;
                    }
                    if ( cache_png_in_mmap)
                    {
                        if (use_pixels_in_mmap) 
                        {
                            Runnable on_end = null;
                            if ( item_type == Iconifiable_item_type.pdf)
                            {
                                // for pdf, the png has been made on disk, it must be deleted to save disk space
                                on_end = delete_on_end;
                            }
                            Mmap.instance.write_image_as_pixels(cache_key.toAbsolutePath().toString(), finalImage_from_cache, true, on_end);
                            if ( dbg) logger.log("Icon caching, WROTE pixels to mmap :" + cache_key.toAbsolutePath().toString() + " w=" + finalImage_from_cache.getWidth() + " h=" + finalImage_from_cache.getHeight());
                        } 
                        else
                        {
                            // for pdf, the png is already on disk
                            if ( item_type != Iconifiable_item_type.pdf)
                            {
                                // we need to save the icon (Image) to disk
                                Static_image_utilities.write_png_to_disk(finalImage_from_cache, cache_key, logger);
                            }
                            Mmap.instance.write_image_as_file(cache_key, true, delete_on_end);
                            if ( dbg) logger.log("Icon caching, WROTE image as file to cache :" + cache_key + " w=" + finalImage_from_cache.getWidth() + " h=" + finalImage_from_cache.getHeight());
                        }
                    }
                    else 
                    {
                        if ( item_type != Iconifiable_item_type.pdf)
                        {
                            Static_image_utilities.write_png_to_disk(finalImage_from_cache, cache_key, logger);
                            if (dbg)
                                logger.log("Icon caching, WROTE file to disk :" + cache_key.toAbsolutePath().toString() + " w=" + finalImage_from_cache.getWidth() + " h=" + finalImage_from_cache.getHeight());
                        }
                    }
                    break;
            }
        };
        Actor_engine.execute(icon_writer,"icon writer",logger);

        if ( dbg) logger.log("Icon caching, returning icon for :" + destination.get_item_path()+ " w="+image_from_cache.getWidth()+" h="+image_from_cache.getHeight());
        Image_properties properties = new Image_properties(image_from_cache.getWidth(), image_from_cache.getHeight(), Rotation.normal);
        return Optional.of(new Image_and_properties(image_from_cache, properties));
    }
}
