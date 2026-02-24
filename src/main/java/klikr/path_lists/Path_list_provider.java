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
    // could be in a disk folder OR in a 'playlist'

    default Files_and_folders load_cache_from_disk(Aborter aborter, Logger logger)
    {
        Optional<Path> op = get_folder_path();
        if ( op.isEmpty()) return null;
        Path folder_path = op.get();
        try
        {
            byte[] bytes = Files.readAllBytes(get_cache_save_path());
            MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(bytes);
            String folder_path2 = unpacker.unpackString();
            if ( !folder_path.toAbsolutePath().toString().equals(folder_path2))
            {
                logger.log(Stack_trace_getter.get_stack_trace("PANIC different folder paths ?"));
                unpacker.close();
                return null;
            }
            long cache_creation_time = unpacker.unpackLong();

            long folder_modification_time = Files.getLastModifiedTime(folder_path).toMillis();
            if ( folder_modification_time > cache_creation_time)
            {
                logger.log("stale folder cache");
                unpacker.close();
                return null;
            }
            set_cache_creation_time(cache_creation_time);
            List<Path> files;
            {
                int files_count = unpacker.unpackArrayHeader();
                files = new ArrayList<>(files_count);
                for (int i = 0; i < files_count; i++)
                {
                    if ( aborter.should_abort())
                    {
                        unpacker.close();
                        return null;
                    }
                    String path = unpacker.unpackString();
                    files.add(Path.of(path));
                }
            }
            List<Path> folders;
            {
                int folders_count = unpacker.unpackArrayHeader();
                folders = new ArrayList<>(folders_count);
                for (int i = 0; i < folders_count; i++)
                {
                    if ( aborter.should_abort())
                    {
                        unpacker.close();
                        return null;
                    }
                    String path = unpacker.unpackString();
                    folders.add(Path.of(path));
                }
            }
            unpacker.close();
            return new Files_and_folders(files,folders);
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return  null;
        }
    }

    void set_cache_creation_time(long cache_creation_time);

    default boolean save_cache_to_disk(Files_and_folders faf, Aborter aborter, Logger logger)
    {
        Optional<Path> op = get_folder_path();
        if (op.isEmpty()) return true;
        Path folder_path = op.get();
        try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
            packer.packString(folder_path.toAbsolutePath().toString());
            long now = System.currentTimeMillis();
            set_cache_creation_time(now);
            packer.packLong(now);

            {
                List<Path> files = faf.files();
                packer.packArrayHeader(files.size());
                for (Path entry : files)
                {
                    if ( aborter.should_abort())
                    {
                        packer.close();
                        return false;
                    }
                    packer.packString(entry.toAbsolutePath().toString());
                }
            }
            {
                List<Path> folders = faf.folders();
                packer.packArrayHeader(folders.size());
                for (Path entry : folders)
                {
                    if ( aborter.should_abort())
                    {
                        packer.close();
                        return false;
                    }
                    packer.packString(entry.toAbsolutePath().toString());
                }
            }
            Path cache_save_path = get_cache_save_path();
            logger.log("get_cache_save_path()="+cache_save_path);
            Files.write(cache_save_path, packer.toByteArray());
        }
        catch (IOException e)
        {
            logger.log(Stack_trace_getter.get_stack_trace(""+e));
            return false;
        }
        return true;
    }

    Path get_cache_save_path();

    boolean is_rescan_needed();

    String to_string();

    String get_key(); // never null (absolute path of *folder*) or (absolute path of *playlist file*)

    Optional<Path> get_folder_path();
    Optional<Path> resolve(String string);

    Change get_Change();

    Files_and_folders files_and_folders(Image_found imgfnd, boolean consider_also_hidden_files, boolean consider_also_hidden_folders, Aborter aborter);
    void reload();
    //List<File> only_files(boolean consider_also_hidden_files, Aborter aborter); // only files, no folders
    List<Path> only_file_paths(boolean consider_also_hidden_files, Aborter aborter);

    List<Path> only_image_paths(boolean consider_also_hidden_files, Aborter aborter);
    List<Path> only_song_paths(boolean consider_also_hidden_files, Aborter aborter);

    //List<File> only_folders(boolean consider_also_hidden_folders, Aborter aborter);
    List<Path> only_folder_paths(boolean consider_also_hidden_folders, Aborter aborter);

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
