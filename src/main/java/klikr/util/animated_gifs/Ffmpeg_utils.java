// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../../../actor/Job_termination_reporter.java
//SOURCES ../../../util/ui/Progress_window.java
//SOURCES ../../../util/ui/Jfx_batch_injector.java
//SOURCES ../../../util/files_and_paths/Icons_from_disk.java
//SOURCES ./Animated_gif_generation_actor.java

package klikr.util.animated_gifs;

import javafx.stage.Window;
import klikr.settings.String_constants;
import klikr.util.External_application;
import klikr.util.Kontext;
import klikr.util.execute.Execute_result;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.settings.boolean_features.Booleans;
import klikr.util.files_and_paths.*;
import klikr.util.ui.Jfx_batch_injector;
import klikr.util.execute.Execute_command;
import klikr.util.log.Logger;
import klikr.util.ui.Popups;


import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

//**********************************************************
public class Ffmpeg_utils
//**********************************************************
{
    private static final boolean dbg = false;
    static final Locale us_locale = new Locale("en","US");//Locale.of("en","US");

    //**********************************************************
    public static Double get_media_duration(
            Path path,
            Kontext context)
    //**********************************************************
    {
        List<String> list = new ArrayList<>();
        list.add(External_application.Ffprobe.get_command(context));
        list.add("-i");
        list.add(path.getFileName().toString());
        list.add("-show_format");
        StringBuilder sb = new StringBuilder();
        File wd = path.getParent().toFile();
        Execute_result res = Execute_command.execute_command_list(list, wd, 2000, sb, context);
        if ( !res.status())
        {
            List<String> verify = new ArrayList<>();
            verify.add(External_application.Ffmpeg.get_command(context));
            verify.add("-version");
            String home = System.getProperty(String_constants.USER_HOME);
            Execute_result res2 = Execute_command.execute_command_list(verify, new File(home), 20 * 1000, null, context);
            if ( !res2.status())
            {
                Booleans.manage_show_ffmpeg_install_warning(context);
            }
        }
        //logger.log("->"+sb.toString()+"<-");

        String[] x = sb.toString().split("\\R");
        for (String l : x)
        {
            if (l.startsWith("duration="))
            {
                String sub = l.substring(9);

                try {
                    double duration = Double.parseDouble(sub);
                    if (dbg) context.log("Found media DURATION: " + duration + " seconds");
                    return (Double) duration;
                }
                catch(NumberFormatException e)
                {
                    return null;
                }

            }
        }
        return null;
    }


    //**********************************************************
    public static double get_audio_bitrate(
            Path audio_path,
            Kontext context)
    //**********************************************************
    {
        List<String> list = new ArrayList<>();
        list.add(External_application.Ffprobe.get_command(context));
        list.add("-i");
        list.add(audio_path.toAbsolutePath().toString());

        StringBuilder sb = new StringBuilder();
        File wd = audio_path.getParent().toFile();
        Execute_result res = Execute_command.execute_command_list(list, wd, 2000, sb, context);
        if ( !res.status())
        {
            List<String> verify = new ArrayList<>();
            verify.add(External_application.Ffmpeg.get_command(context));
            verify.add("-version");
            String home = System.getProperty(String_constants.USER_HOME);
            Execute_result res2 = Execute_command.execute_command_list(verify, new File(home), 20 * 1000, null, context);
            if ( !res2.status())
            {
                Booleans.manage_show_ffmpeg_install_warning(context);
            }
        }
        //logger.log("->"+sb.toString()+"<-");

        double bitrate = -1;
        String[] x = sb.toString().split("\\s+");//split on spaces
        boolean get_next = false;
        for (String l : x)
        {
            //logger.log("FOUND ->" + l +"<-");
            if ( get_next)
            {
                try {
                    bitrate = Double.parseDouble(l.trim());
                }
                catch (NumberFormatException e)
                {
                    break;
                }
                if (dbg) context.log("FOUND bitrate: " + bitrate + "kb/s");
                break;
            }
            if (l.equals("bitrate:"))
            {
                get_next = true;
            }
        }
        return bitrate;
    }

    //**********************************************************
    public static void video_to_mp4_in_a_thread(
            Path video_path,
            AtomicBoolean aborted_reported,
            Kontext context)
    //**********************************************************
    {
        Runnable r = () -> video_to_mp4(video_path, aborted_reported,context);
        Actor_engine.execute(r,"make mp4 from video",context.logger());
    }

    //**********************************************************
    public static void video_to_mp4(
            Path video_path,
            AtomicBoolean aborted_reported,
            Kontext context)
    //**********************************************************
    {
        List<String> list = new ArrayList<>();
        list.add(External_application.Ffmpeg.get_command(context));
        list.add("-i");
        list.add(video_path.getFileName().toString());
        list.add("-codec");
        list.add("copy");
        String new_name = Extensions.get_base_name(video_path.getFileName().toString())+".mp4";
        list.add(new_name);

        File wd = video_path.getParent().toFile();
        if (context.should_abort())
        {
            context.log("video_to_gif aborted");
            if ( !aborted_reported.get())
            {
                aborted_reported.set(true);
                context.log(Logger.warning+" video_to_gif abort reported");
                Jfx_batch_injector.inject(() -> Popups.popup_warning(Logger.warning+" ABORTING MASSIVE GIF GENERATION for " + video_path, "Did you change dir ?", false,context), context);
            }
            return;
        }
        // Output file is empty
        StringBuilder sb = new StringBuilder();
        Execute_result res = Execute_command.execute_command_list(list, wd, 2000, sb, context);
        if ( !res.status())
        {
            List<String> verify = new ArrayList<>();
            verify.add(External_application.Ffmpeg.get_command(context));
            verify.add("-version");
            String home = System.getProperty(String_constants.USER_HOME);
            Execute_result res2 = Execute_command.execute_command_list(verify, new File(home), 20 * 1000, null, context);
            if ( !res2.status())
            {
                Booleans.manage_show_ffmpeg_install_warning(context);
            }
            return;
        }
        context.log("\n\n\n ffmpeg output :\n"+ sb +"\n\n\n");

    }

    //**********************************************************
    public static boolean video_to_gif(
            Path video_path,
            int height,
            int fps,
            Path destination_gif_full_path,
            double clip_duration_in_seconds,
            double start_time_in_seconds,
            int retry_safety_count,
            Kontext context)
    //**********************************************************
    {
        if (retry_safety_count > 5) return false;
        List<String> list = new ArrayList<>();
        list.add(External_application.Ffmpeg.get_command(context));
        list.add("-y"); // force overwrite of output without asking
        // skip some time at the beginning
        if (start_time_in_seconds >= 0)
        {
            list.add("-ss");
            list.add(convert_to_video_time_string(start_time_in_seconds));
        }
        //list.add("00:02:36.000");

        list.add("-i");
        list.add(video_path.getFileName().toString());
        list.add("-r");
        if (fps < 1) fps = 1;
        if (fps > 60) fps = 60;
        //logger.log("calling ffmpeg with:"+fps);
        list.add(""+fps); // -r 10 ==> 10 fps
        list.add("-vf");
        list.add("scale="+height+":-1,setsar=1.1");
        list.add("-t");
        list.add(convert_to_video_time_string(clip_duration_in_seconds));
        list.add(destination_gif_full_path.toAbsolutePath().toString());
        File wd = video_path.getParent().toFile();
        if (context.should_abort())
        {
            context.log("video_to_gif aborted");
            return false;
        }
        // Output file is empty
        StringBuilder sb = new StringBuilder();
        Execute_result res = Execute_command.execute_command_list(list, wd, 2000, sb, context);
        if ( !res.status())
        {
            context.log("ffmpeg command failed! let us retry using a working folder that the user owns");
            List<String> verify = new ArrayList<>();
            verify.add(External_application.Ffmpeg.get_command(context));
            verify.add("-version");
            String home = System.getProperty(String_constants.USER_HOME);
            Execute_result res2 = Execute_command.execute_command_list(verify, new File(home), 20 * 1000, null, context);
            if ( !res2.status())
            {
                Booleans.manage_show_ffmpeg_install_warning(context);
            }
        }
        context.log("\n\n\n ffmpeg output :\n"+ sb +"\n\n\n");



        if (sb.toString().contains("Output file is empty"))
        {
            retry_safety_count++;
            //retry without delay
            return video_to_gif(video_path, height,fps,destination_gif_full_path, clip_duration_in_seconds, 0, retry_safety_count, context);
        }
        return true;
    }

    //**********************************************************
    public static String convert_to_video_time_string(double seconds)
    //**********************************************************
    {
        if ( seconds < 60)
        {
            String f = String.format(us_locale,"%2.3f",seconds);
            return "00:00:" + f;//seconds + ".000";
        }
        else
        {
            int minutes = (int)(seconds /60.0);
            double remaining_seconds = seconds -minutes*60;
            String f = String.format(us_locale,"%2.3f",remaining_seconds);
            if ( minutes < 60)
            {
                return "00:"+minutes+":" + f;//seconds + ".000";
            }
            else
            {
                int hours = minutes/60;
                minutes = minutes-hours*60;
                return hours+":"+minutes+":" + f;//seconds + ".000";
            }
        }
    }




}
