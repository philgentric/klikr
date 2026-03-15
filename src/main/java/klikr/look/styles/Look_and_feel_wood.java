// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.look.styles;

import javafx.scene.paint.Color;
import javafx.stage.Window;
import klikr.look.Jar_utils;
import klikr.look.Look_and_feel;
import klikr.look.Look_and_feel_style;
import klikr.util.log.Logger;

import java.net.URL;

//**********************************************************
public class Look_and_feel_wood extends Look_and_feel
//**********************************************************
{
    public Look_and_feel_style get_look_and_feel_style(){return Look_and_feel_style.wood;}

    public Look_and_feel_wood(Window owner, Logger logger) {
        super("Wood",owner,logger);
    }

    @Override
    public String get_sleeping_man_icon_path() {
        return "icons/lazy.png";
    }

    @Override
    public URL get_CSS_URL(Window owner) {
        return Jar_utils.get_URL_by_name("css/wood.css");
    }

    @Override
    public String get_view_icon_path() {
        return "icons/wood/view.png";
    }

       @Override
    public String get_bookmarks_icon_path() {
        return "icons/wood/bookmarks.png";
    }

    @Override
    public String get_preferences_icon_path() {
        return "icons/wood/preferences.png";
    }

    @Override
    public String get_trash_icon_path()
    {
        return "icons/wood/trash.png";
    }

    @Override
    public String get_up_icon_path()
    {
        return "icons/wood/up.png";
    }

    @Override
    public String get_klik_icon_path() {
        return "icons/wood/camera.png";
    }

    @Override
    public String get_back_icon_path() {return "icons/wood/back.png";}

    @Override
    public String get_broken_icon_path()
    {
        return "icons/light/broken_image.png";
    }

    @Override
    public String get_default_icon_path()
    {
        return "icons/wood/camera.png";
    }

    @Override
    public String get_music_icon_path()
    {
        return "icons/wood/music.png";
    }

    @Override
    public String get_slingshot_icon_path()
    {
        return "icons/slingshot.png";
    }

    @Override
    public String get_folder_icon_path() {return "icons/wood/folder.png";}

    @Override
    public String get_text_color() {return "-fx-text-fill: #FFFFFF;";}

    @Override
    public String get_selected_text_color() {return "-fx-text-fill: #0000FF;";}

    @Override
    public Color get_background_color() {
        return Color.valueOf("#3B3B3B");
    }

    @Override
    public Color get_foreground_color() {
        return Color.WHITE;
    }

    @Override
    public Color get_selection_box_color() {return Color.RED;}

}
