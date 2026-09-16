// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.animated_gifs;

import klikr.settings.String_constants;
import klikr.util.External_application;
import klikr.util.Kontext;
import klikr.util.execute.Execute_result;
import klikr.settings.boolean_features.Booleans;
import klikr.util.files_and_paths.Moving_files;
import klikr.images.Image_context;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Stack_trace_getter;
import klikr.util.execute.Execute_command;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/*
animated-GIF repairing utility: recover the frames and re-assemble them in a "more standard" format
often works on animated-gifs that are behaving strangely like playing to fats or too slow
because the format is weird/wrong
 */
// TODO: consider using Gifsicle?

//**********************************************************
public class Gif_repair
//**********************************************************
{
    private static final boolean dbg = false;
    private static final boolean cleanup = false;

    //**********************************************************
    public static Path extract_all_frames_in_animated_gif(Image_context image_context,
                                                          String uuid,
                                                          Kontext context)
    //**********************************************************
    {
        Path target = image_context.path;
        Path this_dir = target.getParent();

        Path tmp_dir = Static_files_and_paths_utilities.get_trash_dir_of(this_dir,context);
        if ( tmp_dir == null)
        {
            context.log(Stack_trace_getter.get_stack_trace("Weird! could not use tmp directory:"));
            return null;
        }

        Path old_path_for_restore = target;

        // move with a unique name into the trash folder
        Path new_path = Path.of(tmp_dir.toAbsolutePath().toString(),uuid+"_"+target.getFileName().toString());
        Moving_files.safe_move_a_file_or_dir_NOT_in_a_thread(new_path, target.toFile(), context);


        List<String> graphicsMagick_command_line = new ArrayList<>();
        // user GraphicsMagick to extract all gif frames
        graphicsMagick_command_line.add(External_application.GraphicsMagick.get_command(context));
        graphicsMagick_command_line.add("convert");
        graphicsMagick_command_line.add(new_path.toAbsolutePath().toString());//+"[0--1]");
        //l.add("-scene");
        //l.add("1");
        graphicsMagick_command_line.add("+adjoin");
        graphicsMagick_command_line.add(uuid+"_frame_%03d.gif");
        StringBuilder sb = null;
        if ( dbg) sb = new StringBuilder();
        Execute_result res = Execute_command.execute_command_list(graphicsMagick_command_line, tmp_dir.toFile(), 2000, sb, context);
        if ( !res.status())
        {
            List<String> verify = new ArrayList<>();
            verify.add(External_application.GraphicsMagick.get_command(context));
            verify.add("--version");
            String home = System.getProperty(String_constants.USER_HOME);
            Execute_result res2 = Execute_command.execute_command_list(verify, new File(home), 20 * 1000, null, context);
            if ( !res2.status())
            {
                Booleans.manage_show_graphicsmagick_install_warning(context);
            }
            Moving_files.safe_move_a_file_or_dir_NOT_in_a_thread(old_path_for_restore,new_path.toFile(), context);
            return null;
        }
        if ( dbg) context.log(sb.toString());
        return tmp_dir;
    }

    //**********************************************************
    // new_delay is inter-frame delay in hundredths of second : 10 means 10fps, 1 means 100 fps, 50 means 2fps
    public static Path reassemble_all_frames(double new_delay, Image_context image_context, Path tmp_dir, Path final_dest, String uuid)
    //**********************************************************
    {
        Path target = image_context.path;

        {
            List<String> graphicsMagick_command_line = new ArrayList<>();
            graphicsMagick_command_line.add(External_application.GraphicsMagick.get_command(image_context.context));
            graphicsMagick_command_line.add("convert");
            graphicsMagick_command_line.add("-delay");
            graphicsMagick_command_line.add(""+new_delay);
            // -delay is in hundredths of second : 10 means 10fps, 1 means 100 fps, 50 means 2fps
            graphicsMagick_command_line.add(uuid+"_frame_*?.gif");
            graphicsMagick_command_line.add(final_dest.toAbsolutePath().toString());
            StringBuilder sb = null;
            if ( dbg) sb = new StringBuilder();
            Execute_result res = Execute_command.execute_command_list(graphicsMagick_command_line, tmp_dir.toFile(), 2000, sb,image_context.context);
            if ( !res.status())
            {
                List<String> verify = new ArrayList<>();
                verify.add(External_application.GraphicsMagick.get_command(image_context.context));
                verify.add("--version");
                String home = System.getProperty(String_constants.USER_HOME);
                Execute_result res2 = Execute_command.execute_command_list(verify, new File(home), 20 * 1000, null, image_context.context);
                if ( !res2.status())
                {
                    Booleans.manage_show_graphicsmagick_install_warning(image_context.context);
                }
                return null;
            }
            if ( dbg) image_context.context.log(sb.toString());
        }
        if (cleanup)
        {
            List<String> l = new ArrayList<>();
            // rm frame_0*
            l.add("rm");
            l.add("frame_0*.gif");
            l.add(image_context.path.getFileName().toString());
            Execute_result res = Execute_command.execute_command_list(l, tmp_dir.toFile(), 2000, null, image_context.context);
            if ( !res.status())
            {
                return null;
            }

        }
        return target;
    }


}
