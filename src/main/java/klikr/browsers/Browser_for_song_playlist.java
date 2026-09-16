// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browsers;

import javafx.scene.paint.Color;
import klikr.Window_builder;
import klikr.audio.player.The_audio_player;
import klikr.browsers.browser_core.Abstract_browser;
import klikr.browsers.browser_core.Window_manager;
import klikr.path_lists.Path_list_provider;
import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.path_lists.Path_list_provider_for_playlist;
import klikr.util.Kontext;
import klikr.change.old_and_new.Old_and_new_Path;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

//**********************************************************
public class Browser_for_song_playlist extends Abstract_browser
//**********************************************************
{
    public Path_list_provider_for_playlist path_list_provider;

    //**********************************************************
    public Browser_for_song_playlist(Window_builder window_builder, Kontext k)
    //**********************************************************
    {
        super(window_builder,"song playlist","song playlist"+window_builder.path_list_provider.get_key(),Color.PINK, k);
        k.logger().log("Browser_for_song_playlist\n");
        init_base(this);
        if (window_builder.path_list_provider instanceof Path_list_provider_for_file_system)
        {
            k.logger().log("Browser_for_song_playlist FATAL, need a Path_list_provider_for_playlist\n");
            return;
        }
         path_list_provider = (Path_list_provider_for_playlist) window_builder.path_list_provider;

        context.log("Browser_for_song_playlist created with path_list_provider: " + path_list_provider.get_key());


        my_Stage.context.get_Stage().setOnCloseRequest(event ->
            {
                Window_manager.unregister(ID,k);
                The_audio_player.set_browser_is_null();
            });
    }


    //**********************************************************    @Override
    protected String get_name()
    //**********************************************************
    {
        return "Browser_for_song_playlist" ;
    }

    //**********************************************************    @Override
    protected String get_path_for_history()
    //**********************************************************
    {
        return get_Path_list_provider().get_key();
    }


    //*******************************************************
    @Override // File_comparator_provider
    public Comparator<? super Path> get_file_comparator()
    //*******************************************************
    {
        return virtual_landscape.other_file_comparator;
    }

    //**********************************************************
    @Override
    protected Path_list_provider get_Path_list_provider()
    //**********************************************************
    {
        return path_list_provider;
    }

    //**********************************************************
    @Override
    protected String signature()
    //**********************************************************
    {
        return "";
    }

    //**********************************************************
    @Override
    protected void monitor_current_path_list_source()
    //**********************************************************
    {
        context.log("Browser_for_song_playlist monitor_current_path_list_source NOT IMPLEMENTED");
    }

    //**********************************************************
    @Override
    public void set_title()
    //**********************************************************
    {
        context.setTitle("SONG PLAYLIST:" + path_list_provider.the_playlist_file_path.getFileName().toString()+"(this is NOT a folder!)");

    }

    //**********************************************************
    @Override // Change_receiver
    public void you_receive_this_because_a_file_event_occurred_somewhere(List<Old_and_new_Path> l, Kontext context)
    //**********************************************************
    {
        context.log("Browser_for_song_playlist you_receive_this_because_a_file_event_occurred_somewhere "+ l);
        virtual_landscape.redraw_fx(true,"change received",false);
    }

    //**********************************************************
    @Override
    public String get_Change_receiver_string()
    //**********************************************************
    {
        return "";
    }

}
