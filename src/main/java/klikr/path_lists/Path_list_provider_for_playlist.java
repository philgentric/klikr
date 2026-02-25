// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.path_lists;

import javafx.stage.Window;
import klikr.browser.virtual_landscape.Image_found;
import klikr.util.execute.actor.Aborter;
import klikr.browser.Move_provider;
import klikr.change.Change_gang;
import klikr.util.files_and_paths.old_and_new.Command;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.util.files_and_paths.old_and_new.Old_and_new_Path;
import klikr.util.files_and_paths.old_and_new.Status;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//**********************************************************
public class Path_list_provider_for_playlist implements Path_list_provider
//**********************************************************
{
    public static final String KLIKR_IMAGE_PLAYLIST_EXTENSION = "klikr_image_playlist";

    public final Path the_playlist_file_path;
    public final List<String> paths = new ArrayList<>();
    public final Logger logger;
    private final Window owner;
    private final Change change;

    //**********************************************************
    public Path_list_provider_for_playlist(Path the_playlist_file_path, Window owner, Aborter aborter, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        change = new Change(logger);
        this.owner = owner;
        this.the_playlist_file_path = the_playlist_file_path;
        if ( the_playlist_file_path == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("❌ FATAL ERROR: the_playlist_file_path is null!"));
            return;
        }
        reload("constructor",aborter);
    }


    @Override
    public void set_cache_creation_time(long cache_creation_time) {

    }

    @Override
    public Path get_cache_save_path() {
        return null;
    }

    @Override
    public boolean is_rescan_needed() {
        return true;
    }

    //**********************************************************
    @Override
    public String to_string()
    //**********************************************************
    {
        return "Path_list_provider_for_playlist "+the_playlist_file_path;
    }

    //**********************************************************
    @Override
    public Optional<Path> get_folder_path()
    //**********************************************************
    {
        // does not have a meaning for a playlist
        return  Optional.empty();
    }


    //**********************************************************
    @Override
    public String get_key()
    //**********************************************************
    {
        return the_playlist_file_path.toAbsolutePath().toString();
    }

    //**********************************************************
    @Override
    public int how_many_files_and_folders(boolean consider_also_hidden_files, boolean consider_also_hidden_folders, Aborter aborter)
    //**********************************************************
    {
        int returned = 0;
        for ( String s : paths)
        {
            if ( aborter.should_abort()) return 0;
            if ( (new File(s)).isDirectory())
            {
                if (! consider_also_hidden_folders)
                {
                    if ( Guess_file_type.should_ignore(Path.of(s),logger)) continue;
                    returned++;
                    continue;
                }
            }
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(Path.of(s),logger)) continue;
            }
            returned++;
        }
        return returned;

    }


    //**********************************************************
    @Override
    public List<Path> only_file_paths(boolean consider_also_hidden_files, Aborter aborter)
    //**********************************************************
    {
        List<Path> returned = new ArrayList<>();
        for ( String s : paths)
        {
            if ( (new File(s)).isDirectory()) continue;
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(Path.of(s),logger)) continue;
            }
            returned.add(Path.of(s));
        }
        return returned;
    }

    //**********************************************************
    @Override
    public List<Path> only_song_paths(boolean consider_also_hidden_files, Aborter aborter)
    //**********************************************************
    {
        List<Path> returned = new ArrayList<>();
        for ( String s : paths)
        {
            if ( (new File(s)).isDirectory()) continue;
            if( !Guess_file_type.is_this_path_a_music(Path.of(s),logger)) continue;
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(Path.of(s),logger)) continue;
            }
            returned.add(Path.of(s));
        }
        return returned;
    }

    //**********************************************************
    @Override
    public List<Path> only_image_paths(boolean consider_also_hidden_files, Aborter aborter)
    //**********************************************************
    {
        List<Path> returned = new ArrayList<>();
        for ( String s : paths)
        {
            if ( (new File(s)).isDirectory()) continue;
            if( !Guess_file_type.is_this_path_an_image(Path.of(s),owner,logger)) continue;
            if (! consider_also_hidden_files)
            {
                if ( Guess_file_type.should_ignore(Path.of(s),logger)) continue;
            }
            returned.add(Path.of(s));
        }
        return returned;
    }



    //**********************************************************
    @Override
    public List<Path> only_folder_paths(boolean consider_also_hidden_folders, Aborter aborter)
    //**********************************************************
    {
        List<Path> returned = new ArrayList<>();
        for ( String s : paths)
        {
            if ( ! (new File(s)).isDirectory()) continue;
            if (! consider_also_hidden_folders)
            {
                if ( Guess_file_type.should_ignore(Path.of(s),logger)) continue;
            }
            returned.add(Path.of(s));
        }
        return returned;
    }
    //**********************************************************
    @Override
    public Optional<Path> resolve(String string)
    //**********************************************************
    {
        return Optional.empty();
    }


    //**********************************************************
    private void save()
    //**********************************************************
    {
        try {
            Files.delete(the_playlist_file_path);
            Files.write(the_playlist_file_path,paths,java.nio.charset.StandardCharsets.UTF_8, StandardOpenOption.CREATE);
            List<String> lines = Files.readAllLines(the_playlist_file_path, StandardCharsets.UTF_8);
            for ( String s : lines)
            {
                logger.log("AFTER SAVE FILE IS: Path_list_provider_for_playlist.save(): " +s);
            }
        }
        catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }

    }


    //**********************************************************
    @Override
    public Move_provider get_move_provider()
    //**********************************************************
    {
        return (destination, destination_is_trash,
                the_list,owner, x, y,  aborter, logger) ->
        {
            for ( File f : the_list)
            {
                String s = f.getAbsolutePath();
                if ( paths.contains(s)) continue;
                logger.log("Path_list_provider_for_playlist.get_move_provider(): adding "+s);
                paths.add(s);
            }
            save();
            report_change(owner);
        };
    }


    //**********************************************************
    private void report_change(Window owner)
    //**********************************************************
    {
        List<Old_and_new_Path> l = new ArrayList<>();
        Old_and_new_Path oanp = new Old_and_new_Path(
                the_playlist_file_path,
                the_playlist_file_path,
                Command.command_edit,
                Status.edition_done,
                false);
        l.add(oanp);
        Change_gang.report_changes(l,owner);
    }

    //**********************************************************
    @Override
    public void delete(Path path, Window owner, double x, double y, Aborter aborter, Logger logger)
    //**********************************************************
    {
        logger.log("Path_list_provider_for_playlist.delete(): "+path.toAbsolutePath().toString());
        dump("paths before delete");
        paths.remove(path.toAbsolutePath().toString());
        dump("paths after delete");
        save();
        dump("paths after save");
        report_change(owner);
    }

    //**********************************************************
    private void dump(String msg)
    //**********************************************************
    {
        logger.log("===== Path_list_provider_for_playlist.paths: "+msg+" =====");
        for ( String s : paths)
        {
            logger.log("   "+s);
        }
        logger.log("=========================================");
    }

    //**********************************************************
    @Override
    public void delete_multiple(List<Path> paths, Window owner, double x, double y, Aborter aborter, Logger logger)
    //**********************************************************
    {
        for ( Path p : paths)
        {
            paths.remove(p.toAbsolutePath().toString());
        }
        save();
        report_change(owner);
    }

    //**********************************************************
    @Override
    public void reload(String origin, Aborter aborter)
    //**********************************************************
    {
        logger.log("Path_list_provider_for_playlist.reload(), reason ="+origin);
        if ( the_playlist_file_path == null)
        {
            logger.log("❌ FATAL ERROR: the_playlist_file_path is null!");
            return;
        }
        try {
            List<String> ss = Files.readAllLines(the_playlist_file_path,StandardCharsets.UTF_8);
            for ( String s : ss)
            {
                if ( aborter.should_abort()) return;
                if ( !paths.contains(s))
                {
                    logger.log("Path_list_provider_for_playlist.reload(): adding "+s);
                    paths.add(s);
                }
            }
        }
        catch (NoSuchFileException e)
        {
            logger.log("No such file: "+ the_playlist_file_path);
        }
        catch (IOException e) {
            logger.log(Stack_trace_getter.get_stack_trace(e.toString()));
        }
        change.call_change_listeners();
    }

    @Override
    public Change get_Change() {
        return change;
    }

    //**********************************************************
    @Override
    public Files_and_folders files_and_folders(Image_found imgfnd, boolean consider_also_hidden_files, boolean consider_also_hidden_folders, Aborter aborter)
    //**********************************************************
    {

        List<Path> files = new ArrayList<>();
        List<Path> folders = new ArrayList<>();
        Files_and_folders returned = new Files_and_folders(files,folders);
        for ( String s : paths)
        {
            if ( (new File(s)).isDirectory())
            {
                if (! consider_also_hidden_folders)
                {
                    if ( Guess_file_type.should_ignore(Path.of(s),logger)) continue;
                }
                folders.add(Path.of(s));
            }
            else
            {
                if (! consider_also_hidden_files)
                {
                    if ( Guess_file_type.should_ignore(Path.of(s),logger)) continue;
                }
                files.add(Path.of(s));
            }
        }
        return returned;
    }
}
