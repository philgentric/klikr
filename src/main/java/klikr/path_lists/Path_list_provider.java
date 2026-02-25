// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.path_lists;
//SOURCES ../Move_provider.java
import javafx.stage.Window;
import klikr.browser.virtual_landscape.Image_found;
import klikr.util.execute.actor.Aborter;
import klikr.browser.Move_provider;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//**********************************************************
public interface Path_list_provider
//**********************************************************
{
    // an abstract interface to provide a list of paths (files)
    // can be
    // (1) a disk folder
    // OR
    // (2) a 'playlist' = a list of paths that may not be in the same folder

    void set_cache_creation_time(long cache_creation_time);

    Path get_cache_save_path();

    boolean is_rescan_needed();

    String to_string();

    String get_key(); // never null (absolute path of *folder*) or (absolute path of *playlist file*)

    Optional<Path> get_folder_path();
    Optional<Path> resolve(String string);

    Change get_Change();

    Files_and_folders files_and_folders(Image_found imgfnd, boolean consider_also_hidden_files, boolean consider_also_hidden_folders, Aborter aborter);
    void reload(String origin, Aborter aborter);

    List<Path> only_file_paths(boolean consider_also_hidden_files, Aborter aborter);
    List<Path> only_image_paths(boolean consider_also_hidden_files, Aborter aborter);
    List<Path> only_song_paths(boolean consider_also_hidden_files, Aborter aborter);
    List<Path> only_folder_paths(boolean consider_also_hidden_folders, Aborter aborter);

    Move_provider get_move_provider();

    void delete(Path path, Window owner, double x, double y, Aborter aborter, Logger logger);
    void delete_multiple(List<Path> paths, Window owner, double x, double y, Aborter aborter, Logger logger);

    int how_many_files_and_folders(boolean consider_also_hidden_files, boolean consider_also_hidden_folders, Aborter aborter);


    //**********************************************************
    default boolean has_parent()
    //**********************************************************
    {
        Optional<Path> op = get_folder_path();
        if (op.isEmpty())
        {
            System.out.println(Stack_trace_getter.get_stack_trace(""));
            return false;
        }

        Path parent = op.get().getParent();
        return parent != null;
    }

}
