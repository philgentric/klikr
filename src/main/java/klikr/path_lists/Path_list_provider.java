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

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

//**********************************************************
public interface Path_list_provider
//**********************************************************
{
    // an abstract interface to provide a list of paths (files)
    // could be in a disk folder OR in a 'playlist'
    String get_key(); // never null (absolute path of *folder*) or (absolute path of *playlist file*)

    Optional<Path> get_folder_path();
    Optional<Path> resolve(String string);

    Change get_Change();

    Files_and_folders files_and_folders(Image_found imgfnd, boolean consider_also_hidden_files, boolean consider_also_hidden_folders, Aborter aborter);
    void reload();
    List<File> only_files(boolean consider_also_hidden_files); // only files, no folders
    List<Path> only_file_paths(boolean consider_also_hidden_files);

    List<Path> only_image_paths(boolean consider_also_hidden_files);
    List<Path> only_song_paths(boolean consider_also_hidden_files);

    List<File> only_folders(boolean consider_also_hidden_folders);
    List<Path> only_folder_paths(boolean consider_also_hidden_folders);

    Move_provider get_move_provider();

    void delete(Path path, Window owner, double x, double y, Aborter aborter, Logger logger);
    void delete_multiple(List<Path> paths, Window owner, double x, double y, Aborter aborter, Logger logger);


    default boolean has_parent()
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

    int how_many_files_and_folders(boolean consider_also_hidden_files, boolean consider_also_hidden_folders);

}
