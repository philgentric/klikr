// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.audio;

import klikr.settings.String_constants;
import klikr.util.External_application;
import klikr.settings.boolean_features.Booleans;
import klikr.util.Kontext;
import klikr.util.execute.Execute_command;
import klikr.util.execute.Execute_result;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class MediaInfo
//**********************************************************
{

    //**********************************************************
    public static List<String> get(Path path, Kontext context)
    //**********************************************************
    {
        List<String> command_line_for_media_info = new ArrayList<>();
        command_line_for_media_info.add(External_application.MediaInfo.get_command(context));
        command_line_for_media_info.add(path.toAbsolutePath().toString());

        StringBuilder sb = new StringBuilder();
        Execute_result res = Execute_command.execute_command_list(command_line_for_media_info, path.getParent().toFile(), 2000, sb,context);
        if ( !res.status())
        {
            List<String> verify = new ArrayList<>();
            verify.add(External_application.MediaInfo.get_command(context));
            verify.add("--version");
            String home = System.getProperty(String_constants.USER_HOME);
            Execute_result res2 = Execute_command.execute_command_list(verify, new File(home), 20 * 1000, null, context);
            if ( !res2.status())
            {
                Booleans.manage_show_mediainfo_install_warning(context);
            }
            return List.of();
        }

        String[] lines = res.output().split("\\R");

        return List.of(lines);
    }

    //**********************************************************
    public static String extract_release(List<String> l,Kontext context)
    //**********************************************************
    {
        String release = null;
        for(String s : l)
        {
            String marker = "Track name";
            if (s.contains(marker))
            {
                if (!s.contains("Track name/Position"))
                {
                    if (!s.contains("Track name/Total"))
                    {
                        context.log("looking for:"+marker+" found:"+s);
                        int i = s.indexOf(marker);
                        if (i >= 0) {
                            release = s.substring(i + marker.length());
                            release = release.replace(":", "");
                            release = release.trim();
                            context.log("looking for:"+marker+" found:"+release);
                        }
                    }
                }
            }
        }
        if ( release != null) return release;
        for(String s : l)
        {
            String marker = "Title";
            if (s.contains(marker))
            {
                context.log("found ->"+s+"<-");
                int i = s.indexOf(marker);
                if (i >= 0) {
                    release = s.substring(i + marker.length());
                    release = release.replace(":", "");
                    release = release.trim();
                    context.log("looking for:"+marker+" found:"+release);
                }
            }
        }
        return release;

    }
    //**********************************************************
    public static String extract_performer(List<String> l, Kontext context)
    //**********************************************************
    {
        String performer = null;
        for(String s : l)
        {
            String marker = "Performer";
            if (s.contains(marker)) {
                if (!s.contains("Album/Performer"))
                {
                    int i = s.indexOf(marker);
                    if (i >= 0) {
                        performer = s.substring(i + marker.length());
                        performer = performer.replace(":", "");
                        performer = performer.trim();
                    }
                }
            }
        }
        return performer;
    }
}
