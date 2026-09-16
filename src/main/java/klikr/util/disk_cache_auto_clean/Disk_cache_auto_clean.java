// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.disk_cache_auto_clean;

import javafx.stage.Window;
import klikr.util.Kontext;
import klikr.util.cache.Cache_folder;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// makes sure disk cache files are not older than X days
//**********************************************************
public class Disk_cache_auto_clean
//**********************************************************
{
    private static final boolean dbg = false;
    private final int age_limit_in_days;
    public final Kontext context;
    private volatile boolean warning_issued = false;

    record Monitored_folder(String name, Path path){}

    List<Monitored_folder> monitored_folders = new ArrayList<>();


    //**********************************************************
    public Disk_cache_auto_clean(int age_limit_in_days, Kontext context)
    //**********************************************************
    {
        this.context = context;
        this.age_limit_in_days = age_limit_in_days;

        for(Cache_folder cache_folder : Cache_folder.values())
        {
            monitored_folders.add(new Monitored_folder(cache_folder.name(), Static_files_and_paths_utilities.get_cache_folder(cache_folder,context)));
        }
    }

    //**********************************************************
    public boolean monitor()
    //**********************************************************
    {
        for( Monitored_folder monitored_folder : monitored_folders)
        {
            File[] files = monitored_folder.path.toFile().listFiles();
            if ( files==null)
            {
                if ( !warning_issued)
                {
                    context.log("WARNING: Disk_cache_auto_clean not able to list files in "+monitored_folder.path);
                    warning_issued = true;
                }
                continue;
            }
            for ( File f : files)
            {
                if ( f.isDirectory())
                {
                    context.log("WARNING: Disk_cache_auto_clean not erasing folders "+f);
                    continue;
                }
                delete_if_too_old(f);
            }
        }
        return true;
    }

    //**********************************************************
    private void delete_if_too_old(File f)
    //**********************************************************
    {
        long age = Static_files_and_paths_utilities.get_file_age_in_days(f,context);
        //context.log(f.toPath().toAbsolutePath()+ " age = "+age+ " days");
        if ( age > age_limit_in_days)
        {
            if ( dbg) context.log(f.toPath().toAbsolutePath()+ " is too old at "+age+" days, deleting");
            try {
                Files.delete(f.toPath());
            } catch (NoSuchFileException e) {
                context.log(("delete_if_too_old: "+e.toString()));
                //context.log(Stack_trace_getter.get_stack_trace("delete_if_too_old: "+e.toString()));
            }
            catch (DirectoryNotEmptyException e) {
                context.log(Stack_trace_getter.get_stack_trace("delete_if_too_old: "+e.toString()));
            }
            catch (IOException e) {
                context.log(Stack_trace_getter.get_stack_trace("delete_if_too_old: "+e.toString()));
            }
        }
    }

}
