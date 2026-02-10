// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../../util/ui/Text_frame_with_labels.java
//SOURCES ../../util/ui/Text_frame.java

package klikr.browser.items;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Window;
import klikr.Window_builder;
import klikr.Window_type;
import klikr.audio.simple_player.Basic_audio_player;
import klikr.audio.simple_player.Folder_navigator;
import klikr.audio.simple_player.Navigator_auto;
import klikr.browser.icons.image_properties_cache.Image_properties;
import klikr.path_lists.Path_list_provider_for_playlist;
import klikr.properties.boolean_features.Feature_change_target;
import klikr.util.cache.Klikr_cache;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.browser.Drag_and_drop;
import klikr.browser.Image_and_properties;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.browser.icons.Icon_destination;
import klikr.browser.icons.Icon_factory_actor;
import klikr.util.animated_gifs.Animated_gif_from_folder_content;
import klikr.browser.virtual_landscape.*;
import klikr.look.Font_size;
import klikr.look.Look_and_feel_manager;
import klikr.path_lists.Path_list_provider;
import klikr.properties.Non_booleans_properties;
import klikr.properties.boolean_features.Feature;
import klikr.properties.boolean_features.Feature_cache;
import klikr.util.execute.System_open_actor;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.util.files_and_paths.Sizes;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import klikr.util.ui.Jfx_batch_injector;
import klikr.util.ui.Text_frame;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.LongAdder;


//**********************************************************
public class Item_file_no_icon extends Item_file implements Icon_destination
//**********************************************************
{
    public static final boolean dbg = false;

    private static final boolean make_animated_gif = true;

    public Button button;
    public Label label;
    public String text;
    private static DateTimeFormatter date_time_formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Klikr_cache<Path, Image_properties> image_properties_cache;

    //private final Shutdown_target shutdown_target;
    //private final Top_left_provider top_left_provider;
    private final Path_comparator_source path_comparator_source;
    private final Feature_change_target feature_change_target;

    //**********************************************************
    public Item_file_no_icon(
            Application application,
            Scene scene,
            Selection_handler selection_handler,
            Feature_change_target feature_change_target,
            Icon_factory_actor icon_factory_actor,
            Color color,
            String text_,
            Klikr_cache<Path, Image_properties> image_properties_cache,
            Shutdown_target shutdown_target,
            Path path,
            Path_list_provider path_list_provider,
            Top_left_provider top_left_provider,
            Path_comparator_source path_comparator_source,
            Window owner,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        super(application,scene,selection_handler,icon_factory_actor,color, path, path_list_provider,path_comparator_source,owner,aborter, logger);
        this.feature_change_target = feature_change_target;
        this.image_properties_cache = image_properties_cache;
        //this.shutdown_target = shutdown_target;
        //this.top_left_provider = top_left_provider;
        this.path_comparator_source = path_comparator_source;
        text = text_;
        if (path == null) {
            logger.log(Stack_trace_getter.get_stack_trace("❌ FATAL: path is null"));
            return;
        }

        double button_width = Non_booleans_properties.get_column_width(owner);
        if ( button_width < Virtual_landscape.MIN_COLUMN_WIDTH) button_width = Virtual_landscape.MIN_COLUMN_WIDTH;

        button_for_a_non_image_file( text,button_width);

        Look_and_feel_manager.set_button_look(button,false,owner,logger);
        button.setManaged(true); // means the parent tells the button its layout
        button.setMnemonicParsing(false);// avoid suppression of first underscore in names
        button.setTextOverrun(OverrunStyle.ELLIPSIS);
        if (Feature_cache.get(Feature.Show_file_names_as_tooltips))
        {
            Tooltip.install(button, new Tooltip(path.toString()));
        }
        Drag_and_drop.init_drag_and_drop_sender_side(get_Node(),selection_handler,path,logger);
    }

    //**********************************************************
    @Override
    void set_new_path(Path newPath) {
        path = newPath;
    }
    //**********************************************************

    //**********************************************************
    @Override
    public Optional<Path> get_item_path()
    //**********************************************************
    {
        if (path == null) return Optional.empty();
        return Optional.of(path);
    }

    //**********************************************************
    @Override // Item
    public void you_are_visible_specific()
    //**********************************************************
    {

    }

    //**********************************************************
    @Override // Item
    public void you_are_invisible_specific()
    //**********************************************************
    {

    }


    //**********************************************************
    @Override // Item
    public int get_icon_size()
    //**********************************************************
    {
        return 0;
    }


    //**********************************************************
    @Override
    public boolean has_icon()
    //**********************************************************
    {
        return false;
    }
    //**********************************************************
    @Override
    public void receive_icon(Image_and_properties image_and_rotation)
    //**********************************************************
    {
        logger.log(Stack_trace_getter.get_stack_trace("SHOULD NOT HAPPEN"));
    }


    //**********************************************************
    public Optional<Path> get_true_path()
    //**********************************************************
    {
        return get_item_path();
    }

    @Override // Icon_destination
    public Optional<Path> get_path_for_display_icon_destination()
    {
        logger.log("Item_button get_path_for_display_icon_destination DEEP !???");
        return get_path_for_display(true);
    }

    // this call is intended only from a working thread
    // in the icon factory as
    //**********************************************************
    @Override // Item
    public Optional<Path> get_path_for_display(boolean try_deep)
    //**********************************************************
    {
        Optional<Path> optional_of_item_path = get_item_path();
        if ( optional_of_item_path.isEmpty())
        {
            return Optional.empty();
        }
        // for a file the displayed icon is built from the file itself, if supported:
        if ( !optional_of_item_path.get().toFile().isDirectory())
        {
            return get_item_path();
        }

        if ( !try_deep) return Optional.empty();

        // for a folder we have 2 ways to provide an icon
        // 1) an image is taken from the folder and used as icon
        // 2) multiple images are taken from the folder to form an animated gif icon

        // try to find an icon for the folder
        return get_an_image_down_in_the_tree_files(optional_of_item_path.get());
        /*
        no recursive madness please!
        if ( returned != null) return returned;
        // ok, so we did not find an image file in the folder
        // let us go down sub directories (if any)
        return get_an_image_down_in_the_tree_folders(path);
        */

    }



    //**********************************************************
    Optional<Path> get_an_image_down_in_the_tree_files(Path local_path)
    //**********************************************************
    {
        if ( Files.isSymbolicLink(local_path)) return null;
        File dir = local_path.toFile();
        File[] files = dir.listFiles();
        if ( files == null)
        {
            if ( dbg) logger.log("❗ WARNING: dir is access denied: "+local_path);
            return Optional.empty();
        }
        if ( files.length == 0)
        {
            if ( dbg) logger.log("❗ dir is empty: "+local_path);
            return Optional.empty();
        }
        Arrays.sort(files);
        List<File> images_in_folder = null;
        if( make_animated_gif)
        {
            images_in_folder = new ArrayList<>();
        }
        for ( File f : files)
        {
            if (f.isDirectory()) continue; // ignore folders
            if (!Guess_file_type.is_this_file_an_image(f,owner,logger)) continue; // ignore non images
            if( make_animated_gif)
            {
                images_in_folder.add(f);
            }
            else
            {
                // use the first image as icon
                return Optional.of(f.toPath());
            }
        }
        if( make_animated_gif)
        {
            logger.log("✅ make_animated_gif");

            if ( images_in_folder.isEmpty())
            {
                return Optional.empty();
            }

            Optional<Path> returned = Animated_gif_from_folder_content.make_animated_gif_from_images_in_folder(
                    owner,
                    new Path_list_provider_for_file_system(local_path,owner,logger),
                    path_comparator_source,
                    images_in_folder,
                    image_properties_cache,
                    aborter, logger);
            if ( returned.isEmpty())
            {
                if (dbg) logger.log("❗ make_animated_gif_from_all_images_in_folder fails");
                // use the first image as icon, if any
                if (!images_in_folder.isEmpty()) return Optional.of(images_in_folder.get(0).toPath());
            }
            else
            {
                if (dbg) logger.log("✅ make_animated_gif_from_all_images_in_folder OK");
                return returned;
            }
        }

        return Optional.empty(); // no image found
    }


    //**********************************************************
    @Override // Item
    public void set_is_unselected_internal()
    {
        Look_and_feel_manager.give_button_a_file_style(button,owner,logger);
    }


    //**********************************************************
    @Override // Item
    public void set_is_selected_internal()
    //**********************************************************
    {
        Look_and_feel_manager.give_button_a_selected_file_style(button,owner,logger);
    }


    public Button get_button(){ return button;}

    //**********************************************************
    private void button_for_a_non_image_file(String text, double width)
    //**********************************************************
    {
        Optional<Path> optional_of_item_path = get_item_path();
        if ( optional_of_item_path.isEmpty())
        {
            logger.log(Stack_trace_getter.get_stack_trace(""));
            return;
        }
        if ( Feature_cache.get(Feature.Show_single_column_with_details))
        {
            StringBuilder sb = new StringBuilder();
            try {

                FileTime x = Files.readAttributes(optional_of_item_path.get(), BasicFileAttributes.class).creationTime();
                LocalDateTime ldt = x.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                sb.append(ldt.format(date_time_formatter));
                sb.append("                 ");
                sb.append(Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(optional_of_item_path.get().toFile().length(),owner,logger));
                sb.append("                 ");
                if (!optional_of_item_path.get().toFile().canWrite())
                {
                    sb.append("❗ Not Writable!                 ");
                }
            } catch (IOException e) {
                logger.log_exception("",e);
            }
            label = new Label(sb.toString());
            //Font_size.set_preferred_font_size(label,logger);
            Font_size.apply_global_font_size_to_Node(label,owner,logger);
            button = new Button(text,label);
        }
        else
        {
            button = new Button(text);
        }

        button.setMinWidth(width);
        button.setPrefWidth(width);
        //Font_size.set_preferred_font_size(button,logger);
        Font_size.apply_global_font_size_to_Node(button,owner,logger);

        Look_and_feel_manager.give_button_a_file_style(button,owner,logger);
        button.setTextAlignment(TextAlignment.RIGHT);

        button.setOnAction(event -> {

            logger.log("✅ ON ACTION " + optional_of_item_path.get().toAbsolutePath());

            if ( Guess_file_type.is_this_path_a_text(optional_of_item_path.get(),owner,logger))
            {
                logger.log("✅ opening text: " + optional_of_item_path.get().toAbsolutePath());
                Text_frame.show(optional_of_item_path.get(),logger);
                return;
            }
            if ( Guess_file_type.is_this_path_an_audio_playlist(optional_of_item_path.get(),logger))
            {
                logger.log("✅ opening audio playlist: " + optional_of_item_path.get().toAbsolutePath());
                Window_builder.additional_no_past(application,Window_type.Song_playlist_browser,new Path_list_provider_for_playlist(path,  owner, logger),owner,logger);
                return;
            }

            if ( Guess_file_type.is_this_path_a_music(optional_of_item_path.get(),logger))
            {
                if ( Guess_file_type.does_this_file_contain_an_audio_track(optional_of_item_path.get(),owner,logger))
                {
                    logger.log("✅ Item_file_no_icn, opening audio file: " + optional_of_item_path.get().toAbsolutePath());
                    logger.log("path_list_provider="+path_list_provider.to_string());

                    path_list_provider.get_Change().add_change_listener(() -> feature_change_target.update(null,true));

                    Basic_audio_player.get(new Navigator_auto(optional_of_item_path.get(),path_list_provider,logger),aborter,logger);
                    Basic_audio_player.play_song(optional_of_item_path.get().toAbsolutePath().toString(),true);
                    return;
                }
            }
            logger.log("✅ asking the system to open: " + optional_of_item_path.get().toAbsolutePath());
            System_open_actor.open_with_system(application, optional_of_item_path.get(), owner,aborter,logger);
        });

        give_a_menu_to_the_button(button,label);
    }

    /*

    //**********************************************************
    public void button_for_a_directory(String text, double width, double height, Color color)
    //**********************************************************
    {
        String extended_text = text;
        if ( get_item_path() != null)
        {
            if (Files.isSymbolicLink(get_item_path())) {
                extended_text += " **Symbolic link** ";
            }
        }
        button = new Button(extended_text);
        button.setMnemonicParsing(false);// avoid suppression of first underscore in names

        Look_and_feel_manager.set_button_look_as_folder(button, height, color,owner,logger);
        button.setTextAlignment(TextAlignment.RIGHT);
        //double computed_text_width = icons_width + estimate_text_width(text2);

        if (get_item_path() == null)
        {
            // protect crash when going up: root has no parent
            logger.log("WARNING no action for folder:"+text);

            // TODO: this work ONLY if the user-selected language is English
            if ( text.equals("Trash")) {
                button.setOnAction(event -> {
                    Popups.popup_warning("WARNING","NO trash on this media: probably it is read only",true,owner,logger);
                });
            }
            return;
        }

        button.setOnAction(event -> {
            logger.log(Stack_trace_getter.get_stack_trace("BUTTON PRESSED for item file no icon :"+text));

        });

        Drag_and_drop.init_drag_and_drop_receiver_side(path_list_provider.get_move_provider(),get_Node(),owner,get_item_path(),is_trash(),logger);

        give_a_menu_to_the_button(button,label);

        //if ( Non_booleans_properties.get_show_folder_size(logger)) show_how_many_files_deep_folder(button,text,path,aborter,logger);

    }
*/

    //**********************************************************
    public void add_how_many_files_deep_folder(
            LongAdder count,
            Button button,
            String text,
            Path path,
            Map<Path, Long> folder_file_count_cache,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        count.increment();

        Runnable r = () -> {
            Long how_many_files_deep = folder_file_count_cache.get(path);
            if ( how_many_files_deep == null)
            {
                how_many_files_deep = (Long) Static_files_and_paths_utilities.get_how_many_files_deep(path, aborter,  owner, logger);
                folder_file_count_cache.put(path,how_many_files_deep);
            }
            count.decrement();
            String extended_text =  text + " (" + how_many_files_deep + " files)";

            String finalExtended_text = extended_text;
            Jfx_batch_injector.inject(() -> {
                button.setText(finalExtended_text);
                //browser.scene_geometry_changed("number of files in button", true);
            },logger);
        };
        Actor_engine.execute(r, "Compute and display how many files deep",logger);
    }


    //**********************************************************
    public void add_total_size_deep_folder(LongAdder count, Button button, String text, Path path,
                                           Map<Path, Long> folder_total_sizes,
                                           Aborter aborter, Logger logger)
    //**********************************************************
    {
        count.increment();
        Runnable r = () -> {

            Long bytes = folder_total_sizes.get(path);
            if ( bytes == null)
            {
                //logger.log(path+" length not found in cache");
                Sizes sizes = Static_files_and_paths_utilities.get_sizes_on_disk_deep(path, aborter, owner, logger);
                bytes = (Long) sizes.bytes();
                //logger.log(path+" not found in cache, length is "+bytes+ "bytes");
                folder_total_sizes.put(path,bytes);
            }
            else
            {
                logger.log("✅ "+path+" length found in cache "+bytes);
            }
            count.decrement();

            StringBuilder sb =  new StringBuilder();
            sb.append(text);
            sb.append("       ");
            sb.append(Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(bytes,owner,logger));

            //sb.append(", ");
            //sb.append(sizes.files());
            //sb.append(" ");
            //sb.append(My_I18n.get_I18n_string("Files",logger));
            String extended_text = sb.toString();
            Jfx_batch_injector.inject(() -> {
                button.setText(extended_text);
                //browser.scene_geometry_changed("number of files in button", true);
            },logger);
        };
        Actor_engine.execute(r, "Compute and display length deep",logger);
    }

    @Override
    public Node get_Node() {
        return button;
    }


    @Override
    public double get_Width() {
        return button.getWidth();
    }


    //**********************************************************
    @Override
    public double get_Height()
    //**********************************************************
    {
        if ( button.getHeight() == 0)
        {
            // until it is laid out, the button height is zero
            // so this entity CANNOT be used for "layout"... unless...
            // one cheats
            //logger.log("implausible button.getHeight() == 0");
            return 40;
        }
        return button.getHeight();
    }

    //**********************************************************
    @Override
    public boolean is_trash()
    //**********************************************************
    {
        return false;
    }


    //**********************************************************
    @Override
    public Optional<Path> is_parent_of()
    //**********************************************************
    {
        return Optional.empty();
    }

    //**********************************************************
    @Override
    public String get_string()
    //**********************************************************
    {

        Optional<Path> op = get_item_path();
        if ( op.isEmpty())
        {
            logger.log(Stack_trace_getter.get_stack_trace(""));
            return "Item_file_no_icon no path ?" ;
        }
        return "Item_file_no_icon, file: " + op.get().toAbsolutePath();
    }


}
