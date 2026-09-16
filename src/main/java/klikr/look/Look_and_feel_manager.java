// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../browser/Drag_and_drop.java
//SOURCES ./styles/*.java
package klikr.look;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import klikr.zexperimental.Launcher;
import klikr.settings.String_constants;
import klikr.util.Kontext;
import klikr.util.Shared_services;
import klikr.browsers.browser_core.Drag_and_drop;
import klikr.look.styles.Look_and_feel_materiol;
import klikr.look.styles.Look_and_feel_light;
import klikr.look.styles.Look_and_feel_modena;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;


//**********************************************************
public class Look_and_feel_manager
//**********************************************************
{
    // https://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html
    private static final boolean dbg = false;


    public static final boolean look_dbg = false;

    //private static Look_and_feel instance = null;
    //public static List<Look_and_feel> registered = new ArrayList<>();
    private static Image default_icon = null;
    private static Image music_icon = null;
    public static Image denied_icon = null;
    public static Image folder_icon = null;
    public static Image sky_icon = null;
    public static Image floor_icon = null;
    public static Image trash_icon = null;
    public static Image bookmarks_icon = null;
    public static Image view_icon = null;
    public static Image up_icon = null;
    public static Image preferences_icon = null;
    public static Image not_found_icon = null;
    public static Image unknown_error_icon = null;
    public static Image back_icon = null;

    private static volatile Look_and_feel instance;

    //**********************************************************
    public static Look_and_feel get_instance(Logger logger)
    //**********************************************************
    {
        if (instance == null)
        {
            synchronized (Look_and_feel.class)
            {
                if (instance == null)
                {
                    instance = read_look_and_feel_from_properties_file(logger);
                }
            }
        }

        return instance;
    }

    //**********************************************************
    public static Look_and_feel read_look_and_feel_from_properties_file(Logger logger)
    //**********************************************************
    {
        Look_and_feel_style look_and_feel_style = null;
        String style_s = Shared_services.main_properties().get(String_constants.STYLE_KEY);
        boolean and_save = false;
        if (style_s == null)
        {
            // DEFAULT STYLE, first time klik is launched on the platform
            look_and_feel_style = Look_and_feel_style.light;
            and_save = true;
        }
        else
        {
            for (Look_and_feel_style laf : Look_and_feel_style.values()) {
                if (laf.name().equals(style_s)) {
                    look_and_feel_style = laf;
                    break;
                }
            }
            if ( look_and_feel_style == null)
            {
                // the style is not known, so we set it to light
                look_and_feel_style = Look_and_feel_style.light;
                and_save = true;
            }
        }

        if ( and_save) Shared_services.main_properties().set_and_save(String_constants.STYLE_KEY, look_and_feel_style.name());
        if (dbg) logger.log("read_look_and_feel_from_properties_file: using style " + look_and_feel_style.name());
        return  switch (look_and_feel_style)
        {
            default -> new Look_and_feel_light(logger);
            case dark -> new klikr.look.styles.Look_and_feel_dark(logger);
            case wood ->new klikr.look.styles.Look_and_feel_wood(logger);
            case materiol -> new Look_and_feel_materiol(logger);
            case modena -> new Look_and_feel_modena(logger);
        };
    }

    //**********************************************************
    public static void set_drag_look_for_pane(Region pane, Kontext context)
    //**********************************************************
    {
        Look_and_feel i = get_instance(context.logger());
        pane.setBackground(new Background(i.get_drag_fill()));

    }


    //**********************************************************
    public static void set_look_and_feel(Look_and_feel_style style,  Kontext context)
    //**********************************************************
    {
        //context.log(Stack_trace_getter.get_stack_trace("setting style = " + style.name));
        context.log(("setting style = " + style.name()));
        Feature_cache.update_string(String_constants.STYLE_KEY,style.name(),context);
        reset();
    }

    //**********************************************************
    public static void reset()
    //**********************************************************
    {
        instance = null;
        default_icon = null;
        music_icon = null;
        Jar_utils.broken_icon = null;
        denied_icon = null;
        trash_icon = null;
        folder_icon = null;
        up_icon = null;
        preferences_icon = null;
        back_icon = null;
    }

    //**********************************************************
    public static Image get_folder_icon(double icon_size,Logger logger)
    //**********************************************************
    {
        if (folder_icon!= null)
        {
            if ( folder_icon.getHeight() == icon_size) return folder_icon;
        }
        Look_and_feel local_instance = get_instance(logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_folder_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get folder icon path"));
            return null;
        }
        folder_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
        return folder_icon;
    }

    //**********************************************************
    public static Image get_sky_icon(double icon_size, Logger logger)
    //**********************************************************
    {
        if (sky_icon!= null)
        {
            if ( sky_icon.getHeight() == icon_size) return sky_icon;
        }
        Look_and_feel local_instance = get_instance(logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_sky_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get folder icon path"));
            return null;
        }
        sky_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
        return sky_icon;
    }


    //**********************************************************
    public static Image get_floor_icon(double icon_size, Logger logger)
    //**********************************************************
    {
        if (floor_icon!= null)
        {
            if ( floor_icon.getHeight() == icon_size) return floor_icon;
        }
        Look_and_feel local_instance = get_instance(logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_floor_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get folder icon path"));
            return null;
        }
        floor_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
        return floor_icon;
    }




    //**********************************************************
    public static Image get_speaker_on_icon(Logger logger)
    //**********************************************************
    {
        Look_and_feel local_instance = get_instance(logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_speaker_on_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get folder icon path"));
            return null;
        }
        return Jar_utils.load_jfx_image_from_jar(path, 256, logger);
    }
    //**********************************************************
    public static Image get_speaker_off_icon(Logger logger)
    //**********************************************************
    {
        Look_and_feel local_instance = get_instance(logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_speaker_off_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get folder icon path"));
            return null;
        }
        return Jar_utils.load_jfx_image_from_jar(path, 256, logger);
    }


    //**********************************************************
    public static Image get_default_icon(double icon_size, Logger logger)
    //**********************************************************
    {
        if (default_icon!= null)
        {
            if ( default_icon.getHeight() == icon_size) return default_icon;
        }
        Look_and_feel local_instance = get_instance(logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_default_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get default icon path"));
            return null;
        }
        default_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
        return default_icon;
    }


    //**********************************************************
    public static Image get_denied_icon(double icon_size, Logger logger)
    //**********************************************************
    {
        if (denied_icon!= null)
        {
            if ( denied_icon.getHeight() == icon_size) return denied_icon;
        }
        Look_and_feel local_instance = get_instance(logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_denied_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get denied icon path"));
            return null;
        }
        denied_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
        return denied_icon;
    }


    //**********************************************************
    public static Image get_trash_icon(double icon_size, Logger logger)
    //**********************************************************
    {
        if (trash_icon!= null)
        {
            if ( trash_icon.getHeight() == icon_size) return trash_icon;
        }
        Look_and_feel local_instance = get_instance(logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_trash_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get trash icon path"));
            return null;
        }
        trash_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
        return trash_icon;
    }


    //**********************************************************
    public static Image get_up_icon(double icon_size, Logger logger)
    //**********************************************************
    {
        if (up_icon!= null)
        {
            if ( up_icon.getHeight() == icon_size) return up_icon;
        }
        Look_and_feel local_instance = get_instance(logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_up_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get up icon path"));
            return null;
        }
        up_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
        return up_icon;
    }

    //**********************************************************
    public static Image get_back_icon(double icon_size, Logger logger)
    //**********************************************************
    {
        if (back_icon!= null)
        {
            if ( back_icon.getHeight() == icon_size) return back_icon;
        }
        Look_and_feel local_instance = get_instance(logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_back_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get back icon path"));
            return null;
        }
        back_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
        return back_icon;
    }



    //**********************************************************
    public static Image get_bookmarks_icon(double icon_size, Logger logger)
    //**********************************************************
    {
        if (bookmarks_icon!= null)
        {
            if ( bookmarks_icon.getHeight() == icon_size) return bookmarks_icon;
        }
        Look_and_feel local_instance = get_instance(logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_bookmarks_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get up bookmarks path"));
            return null;
        }
        bookmarks_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size,logger);
        return bookmarks_icon;
    }

    //**********************************************************
    public static Image get_view_icon(double icon_size, Logger logger)
    //**********************************************************
    {
        if (view_icon!= null)
        {
            if ( view_icon.getHeight() == icon_size) return view_icon;
        }
        Look_and_feel local_instance = get_instance(logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_view_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get up view icon path"));
            return null;
        }
        view_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
        return view_icon;
    }

    //**********************************************************
    public static Image get_preferences_icon(double icon_size, Logger logger)
    //**********************************************************
    {
        if (preferences_icon!= null)
        {
            if ( preferences_icon.getHeight() == icon_size) return preferences_icon;
        }
        Look_and_feel local_instance = get_instance(logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_preferences_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get up preferences icon path"));
            return null;
        }
        preferences_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
        return preferences_icon;
    }




    //**********************************************************
    public static Image get_not_found_icon(double icon_size, Logger logger)
    //**********************************************************
    {
        if (not_found_icon!= null)
        {
            if ( not_found_icon.getHeight() == icon_size) return not_found_icon;
        }
        Look_and_feel local_instance = get_instance(logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_not_found_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get not_found icon path"));
            return null;
        }
        not_found_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
        return not_found_icon;
    }


    //**********************************************************
    public static Image get_unknown_error_icon(double icon_size, Logger logger)
    //**********************************************************
    {
        if (unknown_error_icon!= null)
        {
            if ( unknown_error_icon.getHeight() == icon_size) return unknown_error_icon;
        }
        Look_and_feel local_instance = get_instance(logger);
        if (local_instance == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = local_instance.get_unknown_error_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get unknown_error icon path"));
            return null;
        }
        unknown_error_icon = Jar_utils.load_jfx_image_from_jar(path, icon_size, logger);
        return unknown_error_icon;
    }











    /**********************************************************



                        CSS STYLE SECTION




     *///**********************************************************



    //**********************************************************
    public static void set_dialog_look(Dialog dialog, Logger logger) // Dialog is NOT a node, it is completely appart
    //**********************************************************
    {
        DialogPane dialog_pane = dialog.getDialogPane();
        Look_and_feel laf = get_instance(logger);
        if (laf.style_sheet_url_string != null) {
            dialog_pane.getStylesheets().clear();
            dialog_pane.getStylesheets().add(laf.style_sheet_url_string);
            dialog_pane.getStyleClass().add("my_dialog");
        }
        //Font_size.set_preferred_font_size(dialog_pane,logger);
        Font_size.apply_global_font_size_to_Node(dialog_pane,logger);
    }
    //**********************************************************
    public static void set_scene_look(Scene scene, Logger logger) // Dialog is NOT a node, it is completely appart
    //**********************************************************
    {
        Look_and_feel laf = get_instance(logger);
        if (laf.style_sheet_url_string != null) {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(laf.style_sheet_url_string);
        }
    }
    /*

                    NODE

     */


    //**********************************************************
    public static void give_button_a_directory_style(Node node, Logger logger)
    //**********************************************************
    {
        if (node instanceof Button button)
        {
            button.setAlignment(Pos.BASELINE_LEFT);
        }
        (get_instance(logger)).set_directory_style(node);
    }
    //**********************************************************
    public static void give_button_a_file_style(Node node, Logger logger)
    //**********************************************************
    {
        if (node instanceof Button button)
        {
            button.setAlignment(Pos.BASELINE_LEFT);
        }
        (get_instance(logger)).set_file_style(node);
    }
    //**********************************************************
    public static void give_button_a_selected_file_style(Node node, Logger logger)
    //**********************************************************
    {
        if ( node == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("node is null"));
            return;
        }
        if (node instanceof Button button)
        {
            button.setAlignment(Pos.BASELINE_LEFT);
        }
        (get_instance(logger)).set_selected_file_style(node);
    }



    /*

                    REGION

     */


    // some regions are not affected by the global CSS
    // this is the case for sub windows and dialogs
    // but maybe also others? unclear
    //**********************************************************
    public static void set_region_look(Region region, Logger logger) // Region is a Node via Parent
    //**********************************************************
    {
        Look_and_feel laf = get_instance(logger);
        if (laf.style_sheet_url_string != null) {
            region.getStylesheets().clear();
            region.getStylesheets().add(laf.style_sheet_url_string);
            //region.getStyleClass().clear();
            region.getStyleClass().add("image-window");
        }
        Font_size.apply_global_font_size_to_Node(region,logger);
    }


    //**********************************************************
    public static void set_label_look(Label label, Logger logger)
    //**********************************************************
    {
        Look_and_feel laf = get_instance(logger);
        if (laf.style_sheet_url_string != null)
        {
            label.getStylesheets().clear();
            label.getStylesheets().add(laf.style_sheet_url_string);
            //label.getStyleClass().clear();
            label.getStyleClass().add(Look_and_feel.LOOK_AND_FEEL_MY_BUTTON);
        }
        Font_size.apply_global_font_size_to_Node(label,logger);
    }


    /*

                    Button

     */


    //**********************************************************
    public static void set_button_look_as_folder(Button button, double icon_height, Color color, Logger logger) // Button is a region
    //**********************************************************
    {
        if ( folder_icon== null)
        {
            folder_icon = get_folder_icon(icon_height,logger);
        }
        set_button_and_image_look(button, folder_icon, icon_height, color,true,logger);
    }


    //**********************************************************
    public static void set_menu_item_look(MenuItem item, Logger logger)
    //**********************************************************
    {
        /*context.log("set_menu_item_look() menu item "+item.getText()
                +"\n   "+item.getStyle()
                +"\n   "+item.getStyleClass());
         */
        //item.getStyleClass().clear();
        item.getStyleClass().add("my_context_menu");
        Font_size.apply_global_font_size_to_MenuItem(item,logger);
    }

    //**********************************************************
    public static void set_context_menu_look(ContextMenu context_menu, Logger logger)
    //**********************************************************
    {

        /*
        does not do anything?
        context_menu.getStyleClass().clear();
        context_menu.getStyleClass().add("my_context_menu");
        Font_size.apply_global_font_size_to_PopupControl(context_menu,context);
        */

    }

    //**********************************************************
    public static void set_CheckBox_look(CheckBox check_box, Logger logger)
    //**********************************************************
    {
        Look_and_feel laf = get_instance(logger);
        if (laf.style_sheet_url_string != null)
        {
            check_box.getStylesheets().clear();
            check_box.getStylesheets().add(laf.style_sheet_url_string);
            //check_box.getStyleClass().clear();
            check_box.getStyleClass().add("check-box");
        }
        Font_size.apply_global_font_size_to_Node(check_box,logger);
    }


    //**********************************************************
    public static void set_TextField_look(TextField text_field, boolean button_look, Logger logger   )
    //**********************************************************
    {
        Font_size.apply_global_font_size_to_Node(text_field,logger);

        Look_and_feel laf = get_instance(logger);
        if (laf.style_sheet_url_string != null)
        {
            if ( button_look ) text_field.getStyleClass().add(Look_and_feel.LOOK_AND_FEEL_MY_BUTTON);

            text_field.setBorder(new Border(new BorderStroke(laf.get_foreground_color(), BorderStrokeStyle.SOLID,new CornerRadii(5),new BorderWidths(1))));
        }

    }


    //**********************************************************
    public static void set_button_and_image_look(Button button,
                                                 Image icon,
                                                 double height,
                                                 Color color,
                                                 boolean is_dir,
                                                 Logger logger) // Button is a Region
    //**********************************************************
    {
        Look_and_feel laf = get_instance(logger);
        if (laf.style_sheet_url_string != null)
        {
            button.getStylesheets().clear();
            button.getStylesheets().add(laf.style_sheet_url_string);
            //button.getStyleClass().clear();
            button.getStyleClass().add(Look_and_feel.LOOK_AND_FEEL_MY_BUTTON);
        }

        if ( icon != null)
        {
            ImageView image_view = new ImageView(icon);
            image_view.setPreserveRatio(true);
            {
                //if (H < Non_booleans_properties.get_font_size()) H = Non_booleans_properties.get_font_size();
                image_view.setFitHeight(height);
            }
            if (color == null) {
                button.setGraphic(image_view);
            }
            else
            {
                HBox hbox = new HBox();
                Circle dot = new Circle(height/4,color);
                dot.setTranslateY(height/4);
                hbox.getChildren().add(dot);
                hbox.getChildren().add(image_view);
                button.setGraphic(hbox);
            }
        }

        if (look_dbg) logger.log(Stack_trace_getter.get_stack_trace("set_button_look"));
        if (is_dir)
        {
            give_button_a_directory_style(button,logger);
        }
        else
        {
            give_button_a_file_style(button,logger);
        }
    }


    //**********************************************************
    public static void set_menubutton_and_image_look(MenuButton button,
                                                 Image icon,
                                                 double height,
                                                 Color color,
                                                 boolean is_dir,
                                                 Logger logger) // Button is a Region
    //**********************************************************
    {
        Look_and_feel laf = get_instance(logger);
        if (laf.style_sheet_url_string != null)
        {
            button.getStylesheets().clear();
            button.getStylesheets().add(laf.style_sheet_url_string);
            //button.getStyleClass().clear();
            button.getStyleClass().add(Look_and_feel.LOOK_AND_FEEL_MY_BUTTON);
        }

        if ( icon != null)
        {
            ImageView image_view = new ImageView(icon);
            image_view.setPreserveRatio(true);
            {
                //if (H < Non_booleans_properties.get_font_size()) H = Non_booleans_properties.get_font_size();
                image_view.setFitHeight(height);
            }
            if (color == null) {
                button.setGraphic(image_view);
            }
            else
            {
                HBox hbox = new HBox();
                Circle dot = new Circle(height/4,color);
                dot.setTranslateY(height/4);
                hbox.getChildren().add(dot);
                hbox.getChildren().add(image_view);
                button.setGraphic(hbox);
            }
        }

        if (look_dbg) logger.log(Stack_trace_getter.get_stack_trace("set_button_look"));
        if (is_dir)
        {
            give_button_a_directory_style(button,logger);
        }
        else
        {
            give_button_a_file_style(button,logger);
        }
    }


    //**********************************************************
    public static void set_region_look(Region region, boolean with_border, Logger logger) // Button is a Region
    //**********************************************************
    {
        region.setPickOnBounds(true);
        Look_and_feel laf = get_instance(logger);
        if ( laf.style_sheet_url_string !=null)
        {
            region.getStylesheets().clear();
            region.getStylesheets().add(laf.style_sheet_url_string);
            //region.getStyleClass().clear();
            if ( with_border)
            {
                region.getStyleClass().add(Look_and_feel.LOOK_AND_FEEL_MY_BUTTON_with_border);
                //region.setBorder(get_border(context));
                //region.setStyle("-fx-padding: 0 2 0 2;");
            }
            else
            {
                region.getStyleClass().add(Look_and_feel.LOOK_AND_FEEL_MY_BUTTON);
            }
            //Font_size.set_preferred_font_size(button,logger);
            Font_size.apply_global_font_size_to_Node(region,logger);

        }
    }
    //**********************************************************
    public static void set_background_for_setOnDragEntered(Node node, Logger logger)
    //**********************************************************
    {
        BackgroundFill background_fill = Look_and_feel_manager.get_drag_fill(logger);
        
        Look_and_feel_manager.set_background(node, background_fill,logger);
    }

    //**********************************************************
    public static void set_background_for_setOnDragOver(Node node, Logger logger)
    //**********************************************************
    {
        set_background_for_setOnDragEntered(node, logger);
    }

    //**********************************************************
    public static void set_background_for_setOnDragExited(Node node, Logger logger)
    //**********************************************************
    {
        Look_and_feel i = get_instance(logger);
        BackgroundFill color = i.get_background_fill();
        if (Drag_and_drop.drag_and_drop_dbg) logger.log("Item_folder_with_icon setOnDragExited color = "+color);
        Look_and_feel_manager.set_background(node, color,logger);

    }
    //**********************************************************
    public static void set_background(Node n, BackgroundFill background_fill, Logger logger)
    //**********************************************************
    {
        if ( n instanceof Button button)
        {
            button.setBackground(new Background(background_fill));
            Node node = button.getGraphic();
            if (node instanceof Label label)
            {
                Look_and_feel_manager.set_label_look(label,logger);
            }
        }
        else if ( n instanceof FlowPane flow_pane)
        {
            flow_pane.setBackground(new Background(background_fill));
        }
    }


    //**********************************************************
    public static BackgroundFill get_drag_fill(Logger logger)
    //**********************************************************
    {
        Look_and_feel laf = get_instance(logger);
        return laf.get_drag_fill();
    }


    //**********************************************************
    public static Border get_border(Logger logger)
    //**********************************************************
    {
        Look_and_feel laf = get_instance(logger);
        return new Border(new BorderStroke(laf.get_foreground_color(), BorderStrokeStyle.SOLID,new CornerRadii(5),new BorderWidths(1)));
    }

    //**********************************************************
    public static Image get_running_film_icon(Logger logger)
    //**********************************************************
    {
        Look_and_feel i = get_instance(logger);
        if (i == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = i.get_running_film_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get running man icon path"));
            return null;
        }
        return Jar_utils.load_jfx_image_from_jar(path, 600, logger);
    }


    //**********************************************************
    public static Image get_the_end_icon(Logger logger)
    //**********************************************************
    {
        Look_and_feel i = get_instance(logger);
        if (i == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get look and feel instance"));
            return null;
        }
        String path = i.get_sleeping_man_icon_path();
        if (path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING: cannot get slipping_man icon path"));
            return null;
        }
        return Jar_utils.load_jfx_image_from_jar(path, 600, logger);
    }


    public enum Icon_type {KLIK, MUSIC, IMAGE,LAUNCHER};

    //**********************************************************
    public static String get_main_window_icon_path(Look_and_feel look_and_feel,Icon_type icon_type)
    //**********************************************************
    {
        switch (icon_type)
        {
            case KLIK:
                return look_and_feel.get_klik_icon_path();
            case MUSIC:
                return look_and_feel.get_music_icon_path();
            case LAUNCHER:
                return look_and_feel.get_slingshot_icon_path();
            case IMAGE:
                return look_and_feel.get_default_icon_path();

        }
        return null;
    }
     //**********************************************************
    public static void set_icon_for_main_window(String badge_text, Icon_type icon_type, Kontext context)
    //**********************************************************
    {
        Look_and_feel look_and_feel = get_instance(context.logger());
        if (look_and_feel == null)
        {
            context.log(Logger.error+"BAD WARNING: cannot get look and feel instance");
        }
        else
        {
            context.log(Logger.ok+" OK: look and feel instance made");
        }

        Stage stage = (Stage)context.owner();
        stage.getIcons().clear();
        Image taskbar_icon = null;
        int[] icon_sizes = {16, 32, 64, 128};
        String icon_path = get_main_window_icon_path(look_and_feel, icon_type);
        for (int s : icon_sizes)
        {
            Image icon = Jar_utils.load_jfx_image_from_jar(icon_path, s, context.logger());
            if (icon!= null)
            {
                stage.getIcons().add(icon);
                taskbar_icon = icon;
            }
            else
            {
                context.log("WARNING: cannot load icon for length " + s + " from path: " + icon_path);
            }
        }

        if (Launcher.gluon)
        {
            // WARNING: trick for native builds on Mac
            // uses JNI to set the Mac dock icon and badge
            context.log("loading icon bytes for Mac dock");
            byte[] icon_bytes = Jar_utils.load_image_bytes_from_jar(icon_path, context);
            context.log(" icon bytes =" +icon_bytes.length);
            Macdock.setup_ext(badge_text,icon_bytes, context.logger());
        }
        else
        {
            My_taskbar_icon.set(taskbar_icon,badge_text,context.logger());
        }
    }

}
