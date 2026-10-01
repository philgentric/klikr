// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.images;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Window;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.Check_remaining_RAM;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.util.log.Logger;

import java.nio.file.Path;

import static klikr.images.Image_window.dbg;

//**********************************************************
public class Keyboard_handling_for_Image_window
//**********************************************************
{
    private static final boolean keyboard_dbg = false;



    //**********************************************************
    static void handle_keyboard(
            Image_window image_window,
            final KeyEvent key_event,
            Logger logger)
    //**********************************************************
    {
        if ( image_window.image_display_handler == null)
        {
            logger.log("Image_window.image_display_handler is null, cannot handle keyboard event");
            return;
        }

        boolean exit_on_escape_preference = Feature_cache.get(Feature.Use_escape_to_close_windows);

        Window owner = image_window.stage;
        if (keyboard_dbg) logger.log("Image_stage KeyEvent="+key_event);


        if (key_event.getCode() == KeyCode.ESCAPE)
        {
            key_event.consume();
            if (!exit_on_escape_preference)
            {
                logger.log("Image_stage : ignoring escape by user preference");
                return;
            }
            if (image_window.is_full_screen)
            {
                logger.log("Image_stage : ignoring escape as a way to close the image window, because we were in full screen");
                // normally javafx will exit fullscreen ...
                image_window.is_full_screen = false;
            }
            else
            {
                if ( dbg) logger.log("image_window : closing image window by user escape");
                image_window.stage.close();
                image_window.my_close();

                int i = image_window.stage_group.indexOf(image_window);
                if ( i >=0) image_window.stage_group.remove(image_window);
                if ( !image_window.stage_group.isEmpty())
                {
                    Image_window previous = image_window.stage_group.get(image_window.stage_group.size()-1);
                    if (previous != null)
                    {
                        previous.stage.requestFocus();
                        previous.stage.toFront();
                    }

                }
            }

            return;
        }


        if (keyboard_dbg) logger.log("keyboard : KeyEvent="+key_event.getCode());

        switch (key_event.getCode())
        {


            case UP:
                if (Feature_cache.get(Feature.Enable_alternate_image_scaling))
                {
                    if (!Check_remaining_RAM.low_memory.get()) {
                        if (keyboard_dbg) logger.log("UP = previous rescaler");
                        Path p = image_window.image_display_handler.get_image_context().get().path;
                        if (!Guess_file_type.is_this_path_extension_a_gif(p, logger)) {
                            // JAVAFX Image for GIF does not support PixelReader
                            image_window.rescaler = image_window.rescaler.previous();
                            image_window.redisplay(true);
                        }
                    }
                }
                break;

            case DOWN:
                if (Feature_cache.get(Feature.Enable_alternate_image_scaling))
                {
                    if (!Check_remaining_RAM.low_memory.get())
                    {
                        if (keyboard_dbg) logger.log("DOWN = next rescaler");
                        Path p = image_window.image_display_handler.get_image_context().get().path;
                        if (!Guess_file_type.is_this_path_extension_a_gif(p,logger)) {
                            // JAVAFX Image for GIF does not support PixelReader
                            image_window.rescaler = image_window.rescaler.next();
                            image_window.redisplay(true);
                        }
                    }
                }
            break;

        }
        key_event.consume();

    }




}
