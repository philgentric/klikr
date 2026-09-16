// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.audio.player;

import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.media.EqualizerBand;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import klikr.util.Kontext;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;


//**********************************************************
public class Media_instance_statics
//**********************************************************
{
    private static ConcurrentLinkedQueue<Aborter> aborters = new ConcurrentLinkedQueue<>();
    private static volatile Media_instance instance;

    // **********************************************************
    public static Media_instance get(Kontext context)
    // **********************************************************
    {
        synchronized (Media_instance.class)
        {
            instance = new Media_instance(context);
            return instance;
        }
    }

    //**********************************************************
    public static void play_this(Path new_song, Media_callbacks media_callbacks, Kontext context)
    //**********************************************************
    {
        if ( new_song == null )
        {
            context.log(Stack_trace_getter.get_stack_trace(Logger.error+" FATAL: new_song == null"));
            return;
        }
        for(;;)
        {
            Aborter aborter = aborters.poll();
            if ( aborter == null)
            {
                break;
            }

            //logger.log("aborting older player");;
            aborter.abort("another song to play");
        }

        //logger.log("no more aborters");
        // make sure we pass a valid URI
        String encoded = new_song.toFile().toURI().toString();
        context.log("Media_instance_statics encoded=->"+encoded+"<-");

        // make a (specific-for-playing-this-song) aborter :
        Aborter aborter = new Aborter("playing "+encoded, context.logger());
        aborters.add(aborter);

        instance = get(context);
        Actor_engine.execute(() -> instance.play_this(encoded, media_callbacks, context.owner()),"play song "+encoded,context.logger());
    }

    //**********************************************************
    public static void play()
    //**********************************************************
    {
        if ( instance == null) return;
        instance.play_internal();
    }

    //**********************************************************
    public static void pause()
    //**********************************************************
    {
        if ( instance == null) return;
        instance.pause_internal();
    }

    //**********************************************************
    public static void add_current_time_listener(ChangeListener<Duration> x)
    //**********************************************************
    {
        if ( instance == null) return;
        instance.add_current_time_listener_internal(x);
    }

    //**********************************************************
    public static Duration get_stop_time()
    //**********************************************************
    {
        if ( instance == null) return null;
        return instance.get_stop_time_internal();
    }



    //**********************************************************
    public static void set_balance(double balance)
    //**********************************************************
    {
        if ( instance == null) return;
        instance.set_balance_internal(balance);

    }

    //**********************************************************
    public static boolean toggle_mute()
    //**********************************************************
    {
        if ( instance == null) return true;
        return instance.toggle_mute_internal();
    }

    //**********************************************************
    public static void set_volume(double volume)
    //**********************************************************
    {
        if ( instance == null) return;
        instance.set_volume_internal(volume);
    }

    //**********************************************************
    public static void seek(Duration target)
    //**********************************************************
    {
        if ( instance == null) return;
        instance.seek_internal(target);
    }

    //**********************************************************
    public static void stop()
    //**********************************************************
    {
        if ( instance == null) return;
        instance.stop_internal();
    }

    //**********************************************************
    public static ObservableList<EqualizerBand> get_bands()
    //**********************************************************
    {
        if ( instance == null) return null;
        return instance.get_bands_internal();

    }

    //**********************************************************
    public static void dispose()
    //**********************************************************
    {
        if ( instance == null) return;
        instance.dispose_internal();
        instance = null;

    }


    //**********************************************************
    public static MediaPlayer.Status get_status()
    //**********************************************************
    {
        if ( instance == null) return MediaPlayer.Status.DISPOSED;
        return instance.get_status_internal();
    }

}
