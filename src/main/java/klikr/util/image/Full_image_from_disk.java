// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.image;

import javafx.application.Platform;
import javafx.scene.image.Image;
import klikr.browsers.browser_core.Image_and_properties;
import klikr.util.External_application;
import klikr.util.Kontext;
import klikr.util.execute.Execute_result;
import klikr.util.execute.actor.Actor_engine;
import klikr.fusk.Fusk_static_core;
import klikr.util.cache.Cache_folder;
import klikr.settings.boolean_features.Booleans;
import klikr.util.Check_remaining_RAM;
import klikr.util.execute.Execute_command;
import klikr.util.files_and_paths.Extensions;
//import klik.util.image.decoding.FITS;
import klikr.util.image.decoding.Fast_aspect_ratio_from_exif_metadata_extractor;
import klikr.util.image.decoding.Fast_width_from_exif_metadata_extractor;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.ui.Popups;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

//**********************************************************
public class Full_image_from_disk
//**********************************************************
{
    public static final boolean dbg = false;

    static boolean user_warned_about_slow_disk = false;

    //**********************************************************
    public static InputStream get_image_InputStream(Path original_image_file, boolean try_fusked, boolean report_if_not_found, Kontext context)
    //**********************************************************
    {
        //context.log("get_image_InputStream");
        if (try_fusked)
        {
            long start = System.currentTimeMillis();
            byte[] buf= Fusk_static_core.defusk_file_to_bytes(original_image_file, context);
            if ( buf == null)
            {
                context.log("WARNING: defusk_file_to_bytes failed");

                if ( System.currentTimeMillis()-start > 1000)
                {
                    if ( !user_warned_about_slow_disk)
                    {
                        user_warned_about_slow_disk = true;
                        Actor_engine.execute(()-> Platform.runLater(
                                ()-> Popups.popup_warning(
                                        "Reading file "+original_image_file+ "\nwas ridiculously slow...",
                                        "\nMaybe it is a bad USB drive\nor a network drive with a slow network connection?",
                                        false,context)),"Warm user about slow disk",context.logger());

                    }
                }
                return null;
            }
            else if ( buf.length == 0)
            {
                // not fusked, fall back to standard
            }
            else
            {
                if ( dbg) context.log("fusked image detected "+original_image_file);
                // was fusked !
                return new ByteArrayInputStream(buf);
            }
        }
        // "standard"
        try
        {
            return new FileInputStream(original_image_file.toFile());
        }
        catch(FileNotFoundException e)
        {
            /* when the file system is under strain, this can fail, reporting "file not found", but the file is there */
            if (Files.isDirectory(original_image_file))
            {
                context.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN (try to file-open a directory!) get_image_InputStream:"+e));
                return null;
            }
            //context.log(Stack_trace_getter.get_stack_trace(e.toString()));
            if ( report_if_not_found)
            {
                context.log(Stack_trace_getter.get_stack_trace("get_image_InputStream:"+e));
            }
            return null;
        }
    }

    //**********************************************************
    @Deprecated
    public static Double determine_width(Path path, boolean report_if_not_found, Kontext context)
    //**********************************************************
    {
        if (dbg) context.log("\n\nIcons_from_disk determine_width "+path);
        double returned = Fast_width_from_exif_metadata_extractor.get_width(path,report_if_not_found, null,context).orElse(0.0);
        // the only other way is to load the image!
        if ( returned > 0) return returned;
        if (context.should_abort())
        {
            //context.log("determine_width aborting");
            return null;
        }
        if(Guess_file_type.is_this_file_extension_an_image(path.toFile(),context))
        {
            Image_and_properties iap = load_native_resolution_image_from_disk( path,  true, context);
            if ( iap == null)
            {
                context.log("cannot load image to get aspect ratio(1)"+path);
                return null;
            }
            Image i = iap.image();
            if (i.isError())
            {
                context.log("cannot load image to get aspect ratio(2)"+path);
                return null;
            }
            return i.getWidth();
        }
        return null;
    }
    //**********************************************************
    @Deprecated
    public static Double determine_aspect_ratio(Path path, boolean report_if_not_found, Kontext context)
    //**********************************************************
    {
        if (dbg) context.log("\n\nIcons_from_disk get_aspect_ratio "+path);
        double returned = Fast_aspect_ratio_from_exif_metadata_extractor.get_aspect_ratio(path,report_if_not_found, null, context).orElse(1.0);
        // the only other way is to load the image!
        if ( returned > 0) return returned;
        if (context.should_abort())
        {
            //context.log("get_aspect_ratio aborting");
            return null;
        }
        if(Guess_file_type.is_this_file_extension_an_image(path.toFile(),context))
        {
            Image_and_properties iap = load_native_resolution_image_from_disk( path,  true, context);
            if ( iap== null)
            {
                context.log("cannot load image to get aspect ratio(1)"+path);
                return null;
            }

            Image i = iap.image();
            if (i.isError())
            {
                context.log("cannot load image to get aspect ratio(2)"+path);
                return 1.0;
            }
            return i.getWidth()/i.getHeight();
        }
        return 1.0;//default
    }


    //**********************************************************
    public static Image_and_properties load_native_resolution_image_from_disk(Path original_image_file, boolean report_if_not_found, Kontext context)
    //**********************************************************
    {
        //context.log("load_native_resolution_image_from_disk");
        if (Check_remaining_RAM.RAM_running_low("running low",context))
        {
            context.log("load_native_resolution_image_from_disk NOT DONE because running low on memory ! ");
            return Image_and_properties.broken(context.logger());
        }
        /*
        if ( Guess_file_type.use_nasa_fits_java_lib)
        {
            if ( Guess_file_type.is_this_extension_a_fits(Extensions.get_extension(original_image_file.getFileName().toString())))
            {
                context.log("image extension is FITS");

                return FITS.load_FITS_image(original_image_file, aborter, context);
            }
        }*/
        if ( Guess_file_type.is_this_extension_a_non_javafx_type(Extensions.get_extension(original_image_file.getFileName().toString())))
        {
            context.log("image extension indicates type cannot be loaded by javafx, using GraphicsMagick for "+original_image_file);
            return use_GraphicsMagick_for_full_image(original_image_file, context);
        }

        // use javafx Image

        InputStream input_stream = get_image_InputStream(original_image_file, Feature_cache.get(Feature.Fusk_is_on), report_if_not_found, context);
        if ( input_stream == null) return null;
        Image image = null;
        try
        {

            image =new Image(input_stream);
        }
        catch (OutOfMemoryError e)
        {
            Check_remaining_RAM.RAM_running_low(""+e,context);
            context.log("OutOfMemoryError when loading image from disk: "+original_image_file.toAbsolutePath()+" : "+e);
            return Image_and_properties.broken(context.logger());
        }
        catch (Exception e)
        {
            context.log(Stack_trace_getter.get_stack_trace(e.toString()));
            Popups.popup_Exception(e,100,"An error occurred while loading an image from disk",context);
            return Image_and_properties.broken(context.logger());
        }
        try {
            input_stream.close();
        } catch (IOException e) {
            context.log(Stack_trace_getter.get_stack_trace(e.toString()));
            e.printStackTrace();
        }
        if ( image.isError())
        {
            if( image.getException().toString().contains("OutOfMemoryError"))
            {
                Check_remaining_RAM.RAM_running_low("image decode error", context);
            }
            else if( image.getException().toString().contains("No loader for image data"))
            {
                context.log(Stack_trace_getter.get_stack_trace(Logger.error+"IMAGE decode failed :"+image.getException()+" "+original_image_file.toAbsolutePath()));
                // this occurs on damaged images like download not finished, or fusk wrong pin code
                // Popups.popup_Exception(image.getException(),100,"If this image was fusked, maybe the pin code is wrong?",logger);
            }
            else
            {
                context.log(Logger.warning+" IMAGE ERROR :"+original_image_file.toAbsolutePath()+" : "+image.getException());
            }
        }
        return Image_and_properties.build(image,false);

    }



    //**********************************************************
    private static Image_and_properties use_GraphicsMagick_for_full_image(Path original_image_file, Kontext context)
    //**********************************************************
    {
        //context.log("using GraphicsMagick_for_full_image");
        Path icon_cache_dir = Cache_folder.get_cache_dir(Cache_folder.icon_cache, context);
        Path png_path = icon_cache_dir.resolve(original_image_file.getFileName().toString()+"_full.png");

        if ( !png_path.toFile().exists())
        {
            context.log("png (converted image) does not exist, creating "+png_path);
            // use GraphicsMagick to convert to png
            List<String> list = List.of(
                    External_application.GraphicsMagick.get_command(context),
                    "convert",
                    original_image_file.toAbsolutePath().toString(),
                    png_path.toAbsolutePath().toString());
            Execute_command.execute_command_list(list, new File("."), 20_000,null, context);
        }
        else
        {
            context.log("png (converted image) exists:  "+png_path);
        }

        if ( context.should_abort()) return null;

        try ( InputStream is = new FileInputStream(png_path.toFile())) {
            return Image_and_properties.build(new Image(is),false);
        }
        catch (IOException e)
        {
            context.log(Stack_trace_getter.get_stack_trace(e.toString()));
            // GraphicsMagick failed, let us try the same with imageMagick
            return  use_ImageMagick_for_full_image(original_image_file, context);
        }
    }

    //**********************************************************
    private static Image_and_properties use_ImageMagick_for_full_image(Path original_image_file, Kontext context)
    //**********************************************************
    {
        context.log("using ImageMagick (fallback!) to load image by converting it");
        Path icon_cache_dir = Cache_folder.get_cache_dir(Cache_folder.icon_cache, context);
        Path png_path = icon_cache_dir.resolve(original_image_file.getFileName().toString()+"_full.png");

        if ( !png_path.toFile().exists())
        {
            context.log("png (converted image) does not exist, creating "+png_path);
            // use ImageMagick to convert to png
            List<String> list = List.of(External_application.ImageMagick.get_command(context), original_image_file.toAbsolutePath().toString(), png_path.toAbsolutePath().toString());
            Execute_result x = Execute_command.execute_command_list(list, new File("."), 20_000, null, context);
            if (!x.status())
            {
                Booleans.manage_show_imagemagick_install_warning(context);
            }
        }
        else
        {
            context.log("SHOULD NOT HAPPEN ! png (converted image) exists:  "+png_path);
        }

        if ( context.should_abort()) return null;

        try ( InputStream is = new FileInputStream(png_path.toFile())) {
            return Image_and_properties.build(new Image(is),false);
        }
        catch (IOException e)
        {
            context.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
        return Image_and_properties.broken(context.logger());
    }



}
