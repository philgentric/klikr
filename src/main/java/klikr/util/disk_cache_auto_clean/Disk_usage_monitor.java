// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.disk_cache_auto_clean;

import klikr.util.Kontext;
import klikr.util.Shared_services;
import klikr.look.my_i18n.My_I18n;
import klikr.settings.Non_booleans_properties;
import klikr.util.cache.Cache_folder;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.ui.Popups;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// this monitors the footprint in MB of given folders
//**********************************************************
public class Disk_usage_monitor
//**********************************************************
{
    private static final boolean dbg = false;
    public static final String TRASH_FOLDER = "Trash folder";
    public final Kontext context;
    private volatile boolean warning_issued = false;

    record Monitored_folder(String name, Path path, boolean auto_delete){}

    List<Monitored_folder> monitored_folders = new ArrayList<>();

    public final int warning_limit_bytes;

    //**********************************************************
    public Disk_usage_monitor(Kontext context)
    //**********************************************************
    {
        this.context = context;

        for (Cache_folder cache_folder : Cache_folder.values())
        {
            if (dbg) context.log("starting Disk_usage_monitor for :" + cache_folder);
            Path ff = Static_files_and_paths_utilities.get_cache_folder(cache_folder, context);
            Monitored_folder tt = new Monitored_folder(cache_folder.name(), ff, true);
            monitored_folders.add(tt);
        }
        for ( Path t : Static_files_and_paths_utilities.get_existing_trash_dirs(context))
        {
            monitored_folders.add(new Monitored_folder(TRASH_FOLDER, t, false));
        }

        warning_limit_bytes = Non_booleans_properties.get_folder_warning_size();


    }

    //**********************************************************
    public boolean monitor()
    //**********************************************************
    {
        if ( warning_limit_bytes <= 0)
        {
            context.log("WARNING: "+     My_I18n.get_I18n_string("Cache_Size_Warning_Limit",context)+" is zero = no limit, no monitoring!");
            return false;
        }
        //long total = 0;
        for( Monitored_folder monitored_folder : monitored_folders)
        {
            long tmp = Static_files_and_paths_utilities.get_size_on_disk_concurrent(monitored_folder.path, context);

            if ( Shared_services.aborter().should_abort())
            {
                context.log("Disk_usage_monitor aborted");
                return false;
            }

            tmp = tmp/1_000_000; // mega bytes!


            if ( tmp > warning_limit_bytes)
            {
                if ( !monitored_folder.auto_delete)
                {
                    Popups.popup_warning(Logger.warning+""+monitored_folder.name+" is getting very large: "+tmp+" Mbytes",
                            "Consider clearing it...\n" +
                                    "or change this limit Using the dedicated item in the preferences menu",
                            false,context);
                    continue;
                }
                boolean cleared = false;
                {
                    for (Cache_folder cache_folder : Cache_folder.values())
                    {
                        if (monitored_folder.name.equals(cache_folder.name()))
                        {
                            Cache_folder.clear_disk_cache(cache_folder, false, context);
                            cleared = true;
                        }
                    }
                }
                if ( cleared) continue;
                if ( !warning_issued)
                {
                    Popups.popup_warning(Logger.warning+" "+monitored_folder.name+" is getting very large: "+tmp+" Mbytes",
                            "Consider clearing it...\n" +
                                    "or change this limit Using the dedicated item in the preferences menu",
                            false,context);
                    warning_issued = true;
                }
            }
        }

        return true;
    }

}
