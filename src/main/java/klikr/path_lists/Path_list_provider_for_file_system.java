// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.path_lists;

import javafx.stage.Window;
import klikr.browser.virtual_landscape.Image_found;
import klikr.util.cache.Cache_folder;
import klikr.util.execute.actor.Aborter;
import klikr.browser.Move_provider;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.util.files_and_paths.Moving_files;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

//**********************************************************
public class Path_list_provider_for_file_system implements Path_list_provider
//**********************************************************
{
    private final Path folder_path;
    private final Logger logger;
    private final Window owner;
    Change change = new Change();
    long timestamp = -1;
    private Files_and_folders cached;
    private volatile long cache_creation_time;

    //**********************************************************
    public Path_list_provider_for_file_system(Path folder_path, Window owner, Logger logger)
    //**********************************************************
    {
        this.folder_path = folder_path;
        if( folder_path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""));
        }
        this.logger = logger;
        this.owner = owner;
    }

    //**********************************************************
    @Override
    public void set_cache_creation_time(long cache_creation_time)     //**********************************************************
    //**********************************************************
    {
        this.cache_creation_time = cache_creation_time;
    }

    //**********************************************************
    @Override
    public Path get_cache_save_path()
    //**********************************************************
    {
        Path folder_cache_dir = Cache_folder.get_cache_dir( Cache_folder.folder_cache,owner,logger);
        logger.log("folder_cache_dir="+folder_cache_dir);
        String local = folder_path.toAbsolutePath().toString();
        local = local.replace(File.separator,"_");
        return Path.of(folder_cache_dir.toAbsolutePath().toString(),local+".cache");
    }


    //**********************************************************
    @Override
    public boolean is_rescan_needed()
    //**********************************************************
    {
        Optional<Path> op = get_folder_path();
        if ( op.isEmpty()) return  false;
        if ( timestamp < 0) return true;
        try {
            long as_of_now = Files.getLastModifiedTime(op.get()).toMillis();
            boolean returned = false;
            if ( as_of_now > timestamp) returned = true;
            timestamp = as_of_now;
            return  returned;
        } catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return  true;
        }
    }

    //**********************************************************
    @Override
    public Optional<Path> get_folder_path()
    //**********************************************************
    {
        if ( folder_path == null) return Optional.empty();
        return Optional.of(folder_path);
    }


    //**********************************************************
    @Override
    public int how_many_files_and_folders(boolean consider_also_hidden_files, boolean consider_also_hidden_folders)
    //**********************************************************
    {
        File dir = folder_path.toFile();
        File[] files = dir.listFiles();
        if ( files == null) return 0;
        int returned = 0;
        for (File file : files)
        {
            if ( file.isDirectory() )
            {
                if (! consider_also_hidden_folders)
                {
                    if ( Guess_file_type.should_ignore(file.toPath(),logger)) continue;
                }
                returned++;
                continue;
            }
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(file.toPath(),logger)) continue;
            }
            returned++;
        }
        return returned;
    }


    //**********************************************************
    @Override
    public Files_and_folders files_and_folders(Image_found imgfnd, boolean consider_also_hidden_files, boolean consider_also_hidden_folders, Aborter aborter)
    //**********************************************************
    {
        Files_and_folders faf = get_faf(aborter);

        // let us perform a real disk scan
        List<Path> file_paths = new ArrayList<>();
        List<Path> folder_paths = new ArrayList<>();
        for (Path file : faf.files())
        {
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(file,logger)) continue;
                if ( Guess_file_type.is_this_path_an_image(file,owner,logger))
                {
                    imgfnd.image_found();
                }
            }
            file_paths.add(file);
            if ( aborter.should_abort()) break;
        }
        for (Path folder : faf.folders())
        {
            if ( ! consider_also_hidden_folders)
            {
                if ( Guess_file_type.should_ignore(folder,logger)) continue;
            }
            folder_paths.add(folder);
            if ( aborter.should_abort()) break;
        }
        Files_and_folders returned = new Files_and_folders(file_paths,folder_paths);
        save_cache_to_disk(returned,aborter,logger);
        return returned;
    }

    private Files_and_folders get_faf(Aborter aborter)
    {
        Files_and_folders faf = null;
        if ( cached != null)
        {
            try
            {
                if (cache_creation_time > Files.getLastModifiedTime(folder_path).toMillis())
                {
                    faf = cached;
                }
            }
            catch (IOException e)
            {
                logger.log(Stack_trace_getter.get_stack_trace(""+e));
                return new Files_and_folders(new ArrayList<>(),new ArrayList<>());
            }
        }
        if ( faf == null)
        {
            faf = load_cache_from_disk(aborter,logger);
        }
        if ( faf == null )
        {
            faf = scan(aborter);
        }
        return faf;
    }

    //**********************************************************
    private Files_and_folders scan(Aborter aborter)
    //**********************************************************
    {
        List<Path> file_paths = new ArrayList<>();
        List<Path> folder_paths = new ArrayList<>();
        File dir = folder_path.toFile();
        File[] files = dir.listFiles();
        if ( files == null) return new Files_and_folders(file_paths,folder_paths);
        for (File file : files)
        {
            if ( aborter.should_abort()) break;
            if ( file.isDirectory() )
            {
                folder_paths.add(file.toPath());
            }
            else
            {
                file_paths.add(file.toPath());
            }
        }
        Files_and_folders returned = new Files_and_folders(file_paths,folder_paths);
        save_cache_to_disk(returned,aborter,logger);
        return returned;
    }


    //**********************************************************
    @Override
    public List<Path> only_folder_paths(boolean consider_also_hidden_folders, Aborter aborter)
    //**********************************************************
    {
        Files_and_folders faf = get_faf(aborter);
        List<Path> returned = new ArrayList<>();
        for (Path folder : faf.folders())
        {
            if (! consider_also_hidden_folders)
            {
                if ( Guess_file_type.should_ignore(folder,logger)) continue;
            }
            returned.add(folder);
        }
        return returned;
    }

    //**********************************************************
    @Override
    public List<Path> only_file_paths(boolean consider_also_hidden_files, Aborter aborter)
    //**********************************************************
    {
        Files_and_folders faf = get_faf(aborter);
        List<Path> returned = new ArrayList<>();
        for (Path file : faf.files())
        {
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(file,logger)) continue;
            }
            returned.add(file);
        }
        return returned;
    }

    //**********************************************************
    @Override
    public List<Path> only_image_paths(boolean consider_also_hidden_files, Aborter aborter)
    //**********************************************************
    {
        Files_and_folders faf = get_faf(aborter);
        List<Path> returned = new ArrayList<>();
        for (Path file : faf.files())
        {
            if ( !Guess_file_type.is_this_path_an_image(file,owner,logger)) continue;
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(file,logger)) continue;
            }
            returned.add(file);
        }
        return returned;
    }


    //**********************************************************
    @Override
    public List<Path> only_song_paths(boolean consider_also_hidden_files, Aborter aborter)
    //**********************************************************
    {
        Files_and_folders faf = get_faf(aborter);
        List<Path> returned = new ArrayList<>();
        for (Path file : faf.files())
        {
            if ( !Guess_file_type.is_this_path_a_music(file,logger)) continue;
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(file,logger)) continue;
            }
            returned.add(file);
        }
        return returned;
    }

    //**********************************************************
    @Override
    public String to_string()
    //**********************************************************
    {
        return "Path_list_provider_for_file_system, "+how_many_files_and_folders(true,true);
    }

    //**********************************************************
    @Override
    public String get_key()
    //**********************************************************
    {
        return folder_path.toAbsolutePath().toString();
    }


    //**********************************************************
    @Override
    public void reload()
    //**********************************************************
    {
        // we dont keep an internal state, so nothing to do here
        // just notify listeners
        change.call_change_listeners();
    }

    //**********************************************************
    @Override
    public Optional<Path> resolve(String string)
    //**********************************************************
    {
        if (folder_path == null) return Optional.empty();
        return Optional.of(folder_path.resolve(string));
    }

    @Override
    public Change get_Change() {
        return change;
    }


/*
    //**********************************************************
    @Override
    public List<File> only_files(boolean hidden_files)
    //**********************************************************
    {
        File f = folder_path.toFile();
        File[] files = f.listFiles();
        if ( files == null) return new ArrayList<>();
        List<File> returned = new ArrayList<>();
        for (File file : files)
        {
            if( file.isDirectory() ) continue;
            if (!hidden_files) if ( Guess_file_type.should_ignore(file.toPath(),logger)) continue;
            returned.add(file);
        }
        return returned;
    }

    //**********************************************************
    @Override
    public List<File> only_folders(boolean consider_also_hidden_folders)
    //**********************************************************
    {
        File f = folder_path.toFile();
        File[] files = f.listFiles();
        if ( files == null) return new ArrayList<>();
        List<File> returned = new ArrayList<>();
        for (File file : files)
        {
            if( !file.isDirectory() ) continue;
            if (!consider_also_hidden_folders) if ( Guess_file_type.should_ignore(file.toPath(),logger)) continue;
            returned.add(file);
        }
        return returned;
    }
*/
    //**********************************************************
    @Override
    public Move_provider get_move_provider()
    //**********************************************************
    {
        return ( destination_folder, destination_is_trash, the_list, owner, x, y,aborter, logger) -> Moving_files.safe_move_files_or_dirs(
                destination_folder,
                destination_is_trash,
                the_list,
                owner, x, y,
                aborter,
                logger);
    }

    //**********************************************************
    @Override
    public void delete(Path path, Window owner, double x, double y, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Static_files_and_paths_utilities.move_to_trash(path,owner,x,y, null, aborter, logger);
    }

    //**********************************************************
    @Override
    public void delete_multiple(List<Path> paths, Window owner, double x, double y, Aborter aborter, Logger logger)
    //**********************************************************
    {
        Static_files_and_paths_utilities.move_to_trash_multiple(paths,owner,x,y, null, aborter, logger);
    }


}
