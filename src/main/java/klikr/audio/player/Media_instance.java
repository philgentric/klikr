// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.audio.player;

import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.media.*;
import javafx.stage.Window;
import javafx.util.Duration;
import klikr.settings.File_storage;
import klikr.util.Kontext;
import klikr.util.Shared_services;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;


//**********************************************************
public class Media_instance
//**********************************************************
{
    private final Kontext context;
    private MediaPlayer the_media_player;
    ChangeListener<Duration> the_change_listener;
    public static final String AUDIO_PLAYER_CURRENT_TIME = "AUDIO_PLAYER_CURRENT_TIME";


    //**********************************************************
    Media_instance(Kontext context)
    //**********************************************************
    {
        this.context = context;
    }


    //**********************************************************
    void pause_internal()
    //**********************************************************
    {
        if ( the_media_player == null) return;
        the_media_player.pause();
    }

    //**********************************************************
    MediaPlayer.Status get_status_internal()
    //**********************************************************
    {
        if ( the_media_player == null) return null;
        return  the_media_player.getStatus();
    }

    //**********************************************************
    void play_internal()
    //**********************************************************
    {
        if ( the_media_player == null) return;
        //context.log(Stack_trace_getter.get_stack_trace("play_internal"));
        the_media_player.play();
    }


    //**********************************************************
    Duration get_stop_time_internal()
    //**********************************************************
    {
        if ( the_media_player == null ) return null;
        return the_media_player.getStopTime();
    }

    //**********************************************************
    void add_current_time_listener_internal(ChangeListener<Duration> change_listener)
    //**********************************************************
    {
        if ( the_media_player == null) return;

        the_change_listener = change_listener;
        the_media_player.currentTimeProperty().addListener(the_change_listener);
    }


    //**********************************************************
    ObservableList<EqualizerBand> get_bands_internal()
    //**********************************************************
    {
        if ( the_media_player == null) return null;
        AudioEqualizer audio_equalizer = the_media_player.getAudioEqualizer();
        if ( audio_equalizer == null) return null;
        return audio_equalizer.getBands();
    }

    //**********************************************************
    void stop_internal()
    //**********************************************************
    {
        if ( the_media_player == null) return;
        //context.log("Media_instance stop_internal");
        if ( the_change_listener != null) the_media_player.currentTimeProperty().removeListener(the_change_listener);

        if ( the_media_player != null) the_media_player.stop();
    }

    //**********************************************************
    void dispose_internal()
    //**********************************************************
    {
        if ( the_media_player == null) return;
        //context.log("Media_instance dispose_internal");
        the_media_player.dispose();
    }

    //**********************************************************
    void seek_internal(Duration target)
    //**********************************************************
    {
        if ( the_media_player == null) return;
        //context.log(Stack_trace_getter.get_stack_trace("seek_internal"));

        the_media_player.seek(target);
    }

    //**********************************************************
    void set_volume_internal(double volume)
    //**********************************************************
    {
        if ( the_media_player == null) return;
        the_media_player.setVolume(volume);
    }

    //**********************************************************
    boolean toggle_mute_internal()
    //**********************************************************
    {
        if ( the_media_player == null) return true;

        if ( the_media_player.isMute())
        {
            the_media_player.setMute(false);
            return false;
        }
        else
        {
            the_media_player.setMute(true);
            return true;
        }
    }

    //**********************************************************
    void set_balance_internal(double balance)
    //**********************************************************
    {
        if ( the_media_player != null) the_media_player.setBalance(balance);
    }


    // String song MUST be a URL
    //**********************************************************
    Song_play_status play_this(String song_URL, Media_callbacks media_callbacks, Window owner)
    //**********************************************************
    {
        //context.log("\n\nplay_this : "+song);
        Media sound;
        try
        {
            sound = new Media(song_URL);
        }
        catch (IllegalArgumentException e)
        {
            context.log("invalid media URL: "+song_URL);
            context.log(""+e);
            //playlist.remove(new_song);
            return Song_play_status.song_should_be_removed_from_playlist_as_path_is_invalid;
        }
        catch (MediaException e)
        {
            context.log(Stack_trace_getter.get_stack_trace("\n\nInvalid media, unlisted: "+song_URL+"\n\n"));
            //playlist.remove(new_song);
            return Song_play_status.song_should_be_removed_from_playlist_as_path_is_invalid;
        }
        try
        {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            context.log(""+e);
            return Song_play_status.aborted;
        }
        if ( context.should_abort())
        {
            context.log("player aborted before previous MediaPlayer dispose REASON: "+ context.abort_reason());
            return Song_play_status.aborted;
        }
        if ( the_media_player !=null)
        {
            the_media_player.stop();
            the_media_player.dispose();
        }
        if ( context.should_abort())
        {
            context.log("player aborted after previous MediaPlayer dispose "+ context.abort_reason());
            return Song_play_status.aborted;
        }
        the_media_player = new MediaPlayer(sound);
        if ( context.should_abort())
        {
            dispose_internal();
            context.log(Logger.warning+" player aborted after new MediaPlayer "+ context.abort_reason());
            return Song_play_status.aborted;
        }
        the_media_player.setCycleCount(1);
        the_media_player.setOnStalled(() -> context.log("\n\n❗ WARNING player is stalling !!"));
        the_media_player.setOnReady(() -> {
            if ( context.should_abort())
            {
                dispose_internal();
                context.log(Logger.warning+" player aborted in setOnReady "+ context.abort_reason());
                return;
            }
            media_callbacks.on_player_ready();
        });
        the_media_player.setOnEndOfMedia(() -> media_callbacks.on_end_of_media());
        the_media_player.setOnPlaying(() -> {
            if ( context.should_abort())
            {
                dispose_internal();
                context.log(Logger.warning+" player aborted in setOnPlaying "+ context.abort_reason());
                return;
            }

            {
                Integer current_time_s = get_current_time(owner);
                //if ( dbg)
                context.log(Logger.ok+" seeking song to " + current_time_s + " s");
                Duration target = Duration.seconds(current_time_s);
                the_media_player.seek(target);
            }
        });
        return Song_play_status.ok;
    }

    //**********************************************************
    Integer get_current_time(Window owner)
    //**********************************************************
    {
        Integer current_time_s = get_current_time_in_song(context);
        if ( current_time_s != null) return current_time_s;
        return 0;
    }


    //**********************************************************
    public static Integer get_current_time_in_song(Kontext context)
    //**********************************************************
    {
        File_storage pm = Shared_services.main_properties();
        String s = pm.get(AUDIO_PLAYER_CURRENT_TIME);
        if (s == null)
        {
            context.log("WARNING: cannot find player current time");
            return 0;
        }
        int returned = 0;
        try {
            returned = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            context.log("WARNING: cannot parse player current time->" + s + "<-");
        }
        //context.log("player current time = "+returned);
        return returned;
    }



}
