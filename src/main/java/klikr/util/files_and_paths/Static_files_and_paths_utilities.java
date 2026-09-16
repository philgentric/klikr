// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Sizes.java
//SOURCES ./disk_scanner/File_payload.java
//SOURCES ./disk_scanner/Dir_payload.java
//SOURCES ./disk_scanner/Scanner.java

package klikr.util.files_and_paths;


import javafx.scene.control.TextInputDialog;
import klikr.settings.String_constants;
import klikr.util.Kontext;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.browsers.browser_core.icons.Error_type;
import klikr.look.my_i18n.My_I18n;
import klikr.util.cache.Cache_folder;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.files_and_paths.disk_scanner.Dir_payload;
import klikr.util.files_and_paths.disk_scanner.Disk_scanner;
import klikr.util.files_and_paths.disk_scanner.File_payload;
import klikr.look.Look_and_feel_manager;
import klikr.change.Change_gang;
import klikr.change.old_and_new.Command;
import klikr.change.old_and_new.Old_and_new_Path;
import klikr.change.old_and_new.Status;
import klikr.util.log.Logger;
import klikr.util.ui.Jfx_batch_injector;
import klikr.util.ui.Popups;
import klikr.util.log.Stack_trace_getter;
import org.apache.commons.io.FileUtils;


import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

//**********************************************************
public class Static_files_and_paths_utilities
//**********************************************************
{
    private static final boolean dbg = false;

    //**********************************************************
    private static boolean copy_directory(Path old_path, Path new_path, Kontext context)
    //**********************************************************
    {
        if (! old_path.toFile().exists())
        {
             context.log("cannot move, file does not exists: "+old_path);
            return false;
        }
        // move a file, if the destination path contains folders that do not exist yet, create them

        Path parent = new_path.getParent();
         context.log("copy_directory parent = "+parent.toAbsolutePath());
        if ( parent != null && !parent.toFile().exists())
        {
            if ( !parent.toFile().mkdirs())
            {
                 context.log("could not create folders for new path "+new_path.toAbsolutePath());
                return  false;
            }
            else
            {
                 context.log("created folders for new path "+new_path.toAbsolutePath());
            }
        }

        try
        {
            Files.copy(old_path,new_path);
        }
        catch (IOException e)
        {
             context.log("could not move "+old_path+" => "+ new_path);
            return false;
        }

        // copy_folder_content
        boolean returned = true;
        for (File f : old_path.toFile().listFiles())
        {
            Path ff = Path.of(new_path.toAbsolutePath().toString(),f.getName());
            if ( f.isDirectory())
            {
                if ( !copy_directory(f.toPath(),ff,context))
                {
                     context.log("could not copy folder: "+f.getAbsolutePath());
                    returned = false;
                }
            }
            try
            {
                Files.copy(f.toPath(), ff);
            }
            catch (IOException e)
            {
                 context.log("copy_folder_content failed : "+f.getAbsolutePath()+ " "+e);
                returned = false;
            }
        }
        return returned;
    }


    //**********************************************************
    public static void move_to_trash_multiple(List<Path> paths,  Runnable after_the_move, Kontext context)
    //**********************************************************
    {
        if ( paths.size() == 0)
        {
             context.log("nothing to delete");
            return;
        }
        Path trash_dir = get_trash_dir_of(paths.get(0), context);
        if (paths.get(0).getParent().toAbsolutePath().toString().equals(trash_dir.toAbsolutePath().toString()))
        {
            Popups.popup_warning(Logger.warning+"" + My_I18n.get_I18n_string("Nothing_Done",  context), My_I18n.get_I18n_string("Nothing_Done_Explanation", context), false,  context);
            return;
        }
        List<Old_and_new_Path> l2 = new ArrayList<>();
        for ( Path p : paths) {
            Path new_Path = (Paths.get(trash_dir.toString(), p.getFileName().toString()));
            Old_and_new_Path oanf2 = new Old_and_new_Path(p, new_Path, Command.command_move_to_trash, Status.before_command, false);
            oanf2.run_after = after_the_move;
            l2.add(oanf2);
        }
        Moving_files.perform_safe_moves_in_a_thread(l2, true, context);

    }

    //**********************************************************
    public static Path get_trash_dir(Kontext context)
    //**********************************************************
    {
        // works on all OSES
        return Static_files_and_paths_utilities.get_trash_dir_of(Path.of(""), context);
    }

    //**********************************************************
    public static void move_to_trash(Path path, Runnable after_the_move, Kontext context)
    //**********************************************************
    {
        Path trash_dir = get_trash_dir_of(path, context);
        if (path.getParent().toAbsolutePath().toString().equals(trash_dir.toAbsolutePath().toString())) {
            Popups.popup_warning(Logger.warning+" "+ My_I18n.get_I18n_string("Nothing_Done",  context), My_I18n.get_I18n_string("Nothing_Done_explanation",  context), false,  context);
            return;
        }
        List<Old_and_new_Path> l2 = new ArrayList<>();
        Path new_Path = (Paths.get(trash_dir.toString(), path.getFileName().toString()));
        Old_and_new_Path oanf2 = new Old_and_new_Path(path, new_Path, Command.command_move_to_trash, Status.before_command,false);
        oanf2.run_after = after_the_move;
        l2.add(oanf2);
        Moving_files.perform_safe_moves_in_a_thread(l2, true,context);

    }


    //**********************************************************
    public static void remove_empty_folders(Path p, boolean recursively, Kontext context)
    //**********************************************************
    {
        if (Files.isDirectory(p)) {
            File dir = p.toFile();
            delete_empty_dirs(dir, recursively, context);
        }
    }

    //**********************************************************
    // returns true if the dir was deleted (because it was empty)
    private static boolean delete_empty_dirs(File dir, boolean recursively, Kontext context)
    //**********************************************************
    {
        int deleted = 0;
        for (; ; ) {
            File[] files = dir.listFiles();
            if ( files == null) return false;
            if (files.length == 0) {
                boolean status = dir.delete();
                if (status) {
                     context.log("Empty directory deleted: " + dir.getAbsolutePath());
                    return true;
                } else {
                     context.log("Empty directory NOT deleted: " + dir.getAbsolutePath());
                    return false;
                }
            }
            if (!recursively) return false;
            int count = 0;
            for (File f : files) {
                if (f.isDirectory()) {
                    if (delete_empty_dirs(f, true, context)) {
                        deleted++;
                        count++;
                    }
                }
            }
            if (count == 0) break;
            // if an empty dir was removed then maybe THIS directory is now empty? need to retry!
             context.log("Empty sub-directories deleted, will retry " + dir.getAbsolutePath());
        }
        if (deleted > 0) return true;
        return false;
    }




    /*
    //**********************************************************
    public static Path get_icons_cache_dir(Kontext context)
    //**********************************************************
    {


        Path tmp_dir = get_absolute_dir_on_user_home(Cache_folder.icon_cache.name(), false, context);
        if (dbg) if (tmp_dir != null) {
             context.log("icon cache dir=" + tmp_dir.toAbsolutePath());
        }
        return tmp_dir;
    }

    //**********************************************************
    public static Path get_folders_icons_cache_dir(context context)
    //**********************************************************
    {
        Path tmp_dir = get_absolute_dir_on_user_home(Cache_folder.folder_icon_cache.name(), false, context);
        if (dbg) if (tmp_dir != null) {
             context.log("folder icon dir file=" + tmp_dir.toAbsolutePath());
        }
        return tmp_dir;
    }



    //**********************************************************
    public static double clear_icon_DISK_cache(boolean show_popup,Stage owner, Kontext context)
    //**********************************************************
    {
        Path icons = get_icons_cache_dir(context);
        return clear_folder(icons, "Image icons' cache on disk", show_popup, context);
    }
*/




    //**********************************************************
    public static double clear_folder(
            Path folder,
            String tag,
            boolean show_popup,
            boolean english,
            Kontext context)
    //**********************************************************
    {
        double size = get_size_on_disk(folder, context);
        if ( show_popup)
        {
            if (user_cancel(tag, size, context)) return 0.0;
        }
        String text;
        if ( english)
        {
            text = Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size_english(size, context);
        }
        else
        {
            text = Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(size, context);
        }
         context.log(tag+", folder cleared: "+folder.toAbsolutePath()+" "+text+" bytes");
        delete_for_ever_all_files_in_dir_in_a_thread(folder, true,  context);
        return size;
    }


    //**********************************************************
    public static boolean user_cancel(String tag, double size, Kontext context)
    //**********************************************************
    {
        String s1 = Logger.warning+" Deleting: "+tag;//My_I18n.get_I18n_string("Warning_deleting_icon", context);
        String s2 = size / 1000_000_000.0 + My_I18n.get_I18n_string("GB_deleted",  context);
        if (size < 1_000_000_000) {
            s2 = size / 1000_000.0 + My_I18n.get_I18n_string("MB_deleted",  context);
        }
        return !Popups.popup_ask_for_confirmation( s1, s2,  context);
    }


    //**********************************************************
    public static void clear_trash(boolean show_popup,Kontext context)
    //**********************************************************
    {
        Runnable r = () -> {
            List<Path> trashes = get_existing_trash_dirs( context);
            String s1 = My_I18n.get_I18n_string("Warning_delete",  context);
            double size = 0;
            for (Path trash : trashes)
            {
                long tmp = get_size_on_disk(trash, context);
                 context.log("trash dir: "+trash+" "+tmp+" bytes");
                size += tmp;
            }
            String s2 = (int) (size / 1000_000.0) + My_I18n.get_I18n_string("MB_deleted",  context);//"MB of files will be truly deleted";
            if (size > 1000_000_000) {
                double r1 = Math.round(size / 1000_000_00);
                double s = r1 / 10.0;
                s2 = (s) + My_I18n.get_I18n_string("GB_deleted",  context);//"MB of files will be truly deleted";

            }
            String finalS = s2;
            Runnable r2 = () -> {
                if ( show_popup)
                {
                    if (!Popups.popup_ask_for_confirmation(Logger.warning+""+s1, finalS,  context)) return;
                }
                for (Path trash : trashes)
                {
                    delete_for_ever_all_files_in_dir_in_a_thread(trash, true, context);
                     context.log("deletion ongoing: "+trash);
                }
                 context.log("Clearing trash folders (one per drive): done");
            };
            Jfx_batch_injector.inject(r2,context);
        };
        Actor_engine.execute(r,"Clear trash",context.logger());

    }


    //**********************************************************
    public static long get_size_on_disk(Path path, Kontext context )
    //**********************************************************
    {
        // the concurrent version is at least 5 times faster!
        return get_size_on_disk_concurrent(path,context);
    }

    //**********************************************************
    public static Sizes get_sizes_on_disk_shallow(Path path, Aborter aborter, Kontext  context )
    //**********************************************************
    {
        File[] all_files = path.toFile().listFiles();
        if ( all_files == null)
        {
            Error_type error = Static_files_and_paths_utilities.explain_error(path,context);
             context.log(error+ " get_sizes_on_disk_shallow: cannot scan folder "+path);
            return new Sizes(0,0,0,0);
        }
        int folders = 0;
        int files = 0;
        int images = 0;
        long bytes = 0;

        for (File f : all_files)
        {
            if (aborter.should_abort())
            {
                 context.log("ABORTED2: Scanner for "+path);
                break;
            }
            if (f.isDirectory())
            {
                folders++;
            }
            else
            {
                files++;
                if( Guess_file_type.is_this_file_extension_an_image(f, context)) images++;
                bytes += f.length();
            }
        }
        return new Sizes(bytes,folders,files,images);

    }
    //**********************************************************
    public static Sizes get_sizes_on_disk_deep(Path folder_path, Kontext context)
    //**********************************************************
    {
        //get_size_on_disk_with_streams(path,context);
        // the concurrent version is at least 5 times faster than this one
        return get_sizes_on_disk_deep_concurrent(folder_path,context);
    }


    //**********************************************************
    public static Sizes get_sizes_on_disk_deep_concurrent(Path folder_path, Kontext context)
    //**********************************************************
    {
        if ( !folder_path.toFile().isDirectory())
        {
             context.log(Stack_trace_getter.get_stack_trace("Stupid2: not a folder: "+folder_path));
            return null;
        }


        //long start = System.currentTimeMillis();
        AtomicInteger folders = new AtomicInteger(0);
        AtomicLong bytes = new AtomicLong(0);

        AtomicLong files = new AtomicLong(0);
        AtomicLong images = new AtomicLong(0);

        File_payload fp = (f) -> {
            bytes.addAndGet(f.length());
            files.incrementAndGet();
            if ( Guess_file_type.is_this_file_extension_an_image(f, context)) images.incrementAndGet();
        };
        Dir_payload dp = f -> {
            folders.incrementAndGet();
            //System.out.println("folder:"+f.getName());
        };
        ConcurrentLinkedQueue<String> warnings = new ConcurrentLinkedQueue<>();
        Disk_scanner.process_folder(folder_path, "get_sizes_on_disk_deep_concurrent", fp,dp, warnings, context);
        if (! warnings.isEmpty())
        {
            StringBuilder sb = new StringBuilder();
            for ( String w : warnings)
            {
                sb.append(w).append("\n");
            }
             context.log(Logger.warning+" Scanner.process_folder, warnings:\n"+sb);
        }

        return new Sizes(bytes.get(),folders.get(),files.get(),images.get());
    }


    //**********************************************************
    public static long get_size_on_disk_concurrent(Path path, Kontext context)
    //**********************************************************
    {
        if ( !path.toFile().isDirectory())
        {
            if(dbg)  context.log(("skipping get_size_on_disk_concurrent: not a folder: "+path));
            return 0L;
        }
        // this is a blocking call
        //long start = System.currentTimeMillis();
        AtomicLong bytes = new AtomicLong(0);
        File_payload fp = (f) ->
        {
            bytes.addAndGet(f.length());
        };
        ConcurrentLinkedQueue<String> wp = new ConcurrentLinkedQueue<>();
        Disk_scanner.process_folder(path, "get_size_on_disk_concurrent", fp,null,wp,context);
        //long now = System.currentTimeMillis();
        // context.log("get_size_on_disk_concurrent: " + (now-start)+" length=" +length.get());
        return bytes.get();
    }



    //**********************************************************
    public static long get_how_many_files_deep(Path path, Aborter aborter, Kontext context)
    //**********************************************************
    {
        //return get_how_many_files_streams(path,context);
        return get_how_many_files_deep_concurrent(path,aborter, context);
    }

    //**********************************************************
    private static long get_how_many_files_deep_concurrent(Path path, Aborter aborter, Kontext context)
    //**********************************************************
    {
        if ( !path.toFile().isDirectory())
        {
             context.log(Stack_trace_getter.get_stack_trace("Stupid4: not a folder: "+path));
            return 0L;
        }
        AtomicLong files = new AtomicLong(0);
        File_payload fp = (f) ->
        {
            if (Guess_file_type.is_this_path_invisible_when_browsing(f.toPath(), context))
            {
                if ( !Feature_cache.get(Feature.Show_hidden_files))
                {
                    return;
                }
            }
            files.incrementAndGet();
        };
        ConcurrentLinkedQueue<String> wp = new ConcurrentLinkedQueue<>();
        Disk_scanner.process_folder(path, "get_how_many_files_deep_concurrent", fp,null, wp,context);
        return files.get();
    }


    //**********************************************************
    public static String get_1_line_string_for_byte_data_size_english(double size,Kontext  context)
    //**********************************************************
    {
        String returned;
        if (size < 1000) {
            String bytes = "Bytes";
            returned = size + " "+bytes;
        } else if (size < 1000_000) {
            String kBytes = "kBytes";
            returned = String.format("%.1f", size / 1000.0) + " "+kBytes;
        } else if (size < 1000_000_000) {
            String MBytes = "MBytes";
            returned = String.format("%.1f", size / 1000_000.0) + " "+MBytes;
        } else if (size < 1000_000_000_000.0) {
            String GBytes = "GBytes";
            returned = String.format("%.1f", size / 1000_000_000.0) + " "+GBytes;
        } else {
            String TBytes = "TBytes";
            returned = String.format("%.1f", size / 1000_000_000_000.0) + " "+TBytes;
        }
        return returned;
    }

    //**********************************************************
    public static String get_1_line_string_for_byte_data_size(double size,Kontext context)
    //**********************************************************
    {
        String returned;
        if (size < 1000) {
            String bytes = My_I18n.get_I18n_string("Bytes",  context);
            returned = size + " "+bytes;
        } else if (size < 1000_000) {
            String kBytes = My_I18n.get_I18n_string("kBytes",  context);
            returned = String.format("%.1f", size / 1000.0) + " "+kBytes;
        } else if (size < 1000_000_000) {
            String MBytes = My_I18n.get_I18n_string("MBytes",  context);
            returned = String.format("%.1f", size / 1000_000.0) + " "+MBytes;
        } else if (size < 1000_000_000_000.0) {
            String GBytes = My_I18n.get_I18n_string("GBytes",  context);
            returned = String.format("%.1f", size / 1000_000_000.0) + " "+GBytes;
        } else {
            String TBytes = My_I18n.get_I18n_string("TBytes",  context);
            returned = String.format("%.1f", size / 1000_000_000_000.0) + " "+TBytes;
        }
        return returned;
    }


    //**********************************************************
    public static String get_1_line_string_with_size(Path path, Kontext context)
    //**********************************************************
    {
        long BYTES = path.toFile().length();
        StringBuilder sb = new StringBuilder();
        if (BYTES > 1000_000_000)
        {
            double GB = (double) BYTES / 1000_000_000.0;
            String GBytes = My_I18n.get_I18n_string("GBytes",  context);
            sb.append(String.format("%.1f", GB)).append(" ").append(GBytes);
        }
        else if (BYTES > 1000_000)
        {
            double MB = (double) BYTES / 1000_000.0;
            String MBytes = My_I18n.get_I18n_string("MBytes",  context);
            sb.append(String.format("%.1f", MB)).append(" ").append(MBytes);
        }
        else if (BYTES > 1000)
        {
            String kBytes = My_I18n.get_I18n_string("kBytes",  context);
            sb.append(String.format("%.1f", (double) BYTES / 1000.0)).append(" ").append(kBytes);
        }
        else
        {
            String bytes = My_I18n.get_I18n_string("Bytes",  context);
            sb.append(BYTES).append(" ").append(bytes);
        }

        return sb.toString();
    }

    //**********************************************************
    private static void delete_for_ever_all_files_in_dir_in_a_thread(Path dir, boolean also_folders, Kontext context)
    //**********************************************************
    {
        Runnable r = () -> {
            String s = delete_for_ever_all_files_in_dir(dir, also_folders, context);
            if ( s != null)
            {
                Runnable rr = new Runnable() {
                    @Override
                    public void run() {
                        if ( s.contains("AccessDeniedException") && s.contains(String_constants.TRASH_DIR))
                        {
                            Popups.popup_warning(Logger.error+"There is a permission issue in the TRASH folder, did you move in the trash a folder that you do not own?\nYou will have to fix that manually",s,false, context);
                        }
                        else {
                            Popups.popup_warning( Logger.error+"Error", s, false,  context);
                        }
                    }
                };
                Jfx_batch_injector.inject(rr,context);
            }
        };

        Actor_engine.execute(r,"Delete forever files in trash",context.logger());

    }

    //**********************************************************
    private static String delete_for_ever_all_files_in_dir(Path dir, boolean also_folders, Kontext context)
    //**********************************************************
    {
        File files[] = dir.toFile().listFiles();
        if ( files == null)
        {
             context.log("cannot list dir ->"+dir.toAbsolutePath()+"<-");
            return "cannot list dir ->"+dir.toAbsolutePath()+"<-";
        }
        List<Old_and_new_Path> l = new ArrayList<>();
        for ( File f : files)
        {
            if ( dbg)  context.log("delete_for_ever_all_files_in_dir: ->"+f.getAbsolutePath()+"<-");

            if (f.isDirectory())
            {
                String s = delete_for_ever_all_files_in_dir(f.toPath(), also_folders,  context);
                if ( s != null)  context.log(s);
                if (also_folders) f.delete();
            }
            else
            {
                String s = delete_for_ever_a_file(f.toPath(),context);
                if ( s != null)  context.log(s);
            }

        }
        Change_gang.report_changes(l,context.owner());

        /*
        catch (IOException x)
        {
            // directory permission problems are caught here.
            if ( x.toString().contains("AccessDeniedException"))
            {
                try {
                    Set<PosixFilePermission> permissions = new TreeSet<>();
                    permissions.add(PosixFilePermission.OWNER_WRITE);
                    permissions.add(PosixFilePermission.OTHERS_WRITE);
                    permissions.add(PosixFilePermission.GROUP_WRITE);
                    Files.setPosixFilePermissions(dir, permissions);
                }
                catch (AccessDeniedException e)
                {
                     context.log(Stack_trace_getter.get_stack_trace(e.toString()));
                } catch (IOException e) {
                     context.log(Stack_trace_getter.get_stack_trace(e.toString()));
                }
                try
                {
                    Files.delete(dir);
                }
                catch (AccessDeniedException e)
                {
                     context.log(Stack_trace_getter.get_stack_trace(e.toString()));
                }
                catch (IOException e) {
                     context.log(Stack_trace_getter.get_stack_trace(e.toString()));
                }

            }
             context.log(Stack_trace_getter.get_stack_trace(x.toString()));
        }*/
        return null;
    }

    //**********************************************************
    private static String delete_for_ever_all_files_in_dir2(Path dir, boolean also_folders, Kontext context)
    //**********************************************************
    {

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir))//, "*.{jpg,JPG,gif,GIF,png,PNG,jpeg,JPEG}"))
        {

            List<Old_and_new_Path> l = new ArrayList<>();
            for (Path p : stream)
            {
                if (Files.isDirectory(p))
                {
                    String s = delete_for_ever_all_files_in_dir(p, also_folders,  context);
                    if ( s != null)  context.log(s);
                    if (also_folders) Files.delete(p);
                }
                else
                {
                    String s = delete_for_ever_a_file(p,context);
                    if ( s != null)  context.log(s);

                }

            }
            Change_gang.report_changes(l,context.owner());

        }
        catch (IOException x)
        {
            // directory permission problems are caught here.
            if ( x.toString().contains("AccessDeniedException"))
            {
                try {
                    Set<PosixFilePermission> permissions = new TreeSet<>();
                    permissions.add(PosixFilePermission.OWNER_WRITE);
                    permissions.add(PosixFilePermission.OTHERS_WRITE);
                    permissions.add(PosixFilePermission.GROUP_WRITE);
                    Files.setPosixFilePermissions(dir, permissions);
                }
                catch (AccessDeniedException e)
                {
                     context.log(Stack_trace_getter.get_stack_trace(e.toString()));
                } catch (IOException e) {
                     context.log(Stack_trace_getter.get_stack_trace(e.toString()));
                }
                try
                {
                    Files.delete(dir);
                }
                catch (AccessDeniedException e)
                {
                     context.log(Stack_trace_getter.get_stack_trace(e.toString()));
                }
                catch (IOException e) {
                     context.log(Stack_trace_getter.get_stack_trace(e.toString()));
                }

            }
             context.log(Stack_trace_getter.get_stack_trace(x.toString()));
        }
        return null;// OK
    }

    //**********************************************************
    private static String delete_for_ever_a_file(Path p, Kontext context)
    //**********************************************************
    {
        try
        {
            Files.delete(p);
            if ( dbg)  context.log("Deleted for ever: " + p);
        }
        catch (NoSuchFileException x)
        {
            if ( p.getFileName().toString().startsWith("._"))
            {
                if ( dbg)  context.log("ignoring file with name " + p);
                // this is a macOS file found on external drives, if the file was deleted, then this file was deleted too,
                // so the original list is "wrong", it is not an error, just ignore it
                return null;
            }
             context.log(Stack_trace_getter.get_stack_trace(x.toString()));
            return x.toString();
        }
        catch (DirectoryNotEmptyException x) {
             context.log(Stack_trace_getter.get_stack_trace(x.toString()));
            return x.toString();
        }
        catch (FileSystemException x)
        {
             context.log(Stack_trace_getter.get_stack_trace(x.toString()));
            return x.toString();
        }
        catch (IOException x)
        {
            if ( x.toString().contains("AccessDeniedException"))
            {
                try {
                    Set<PosixFilePermission> permissions = new TreeSet<>();
                    permissions.add(PosixFilePermission.OWNER_WRITE);
                    permissions.add(PosixFilePermission.OWNER_READ);
                    permissions.add(PosixFilePermission.OWNER_EXECUTE);
                    permissions.add(PosixFilePermission.OTHERS_WRITE);
                    permissions.add(PosixFilePermission.OTHERS_READ);
                    permissions.add(PosixFilePermission.OTHERS_EXECUTE);
                    permissions.add(PosixFilePermission.GROUP_WRITE);
                    permissions.add(PosixFilePermission.GROUP_READ);
                    permissions.add(PosixFilePermission.GROUP_EXECUTE);
                    Files.setPosixFilePermissions(p, permissions);
                }
                catch (UnsupportedOperationException e)
                {
                     context.log(Stack_trace_getter.get_stack_trace(e.toString()));
                    return e.toString();
                }
                catch (AccessDeniedException e)
                {
                     context.log(Stack_trace_getter.get_stack_trace(e.toString()));
                    return e.toString();
                }
                catch (IOException e)
                {
                     context.log(Stack_trace_getter.get_stack_trace(e.toString()));
                    return e.toString();
                }
                try
                {
                    Files.delete(p);
                }
                catch (IOException e)
                {
                     context.log(Stack_trace_getter.get_stack_trace(e.toString()));
                    return e.toString();
                }
            }
            else
            {
                 context.log(Stack_trace_getter.get_stack_trace(x.toString()));
                return x.toString();

            }
        }
        return null;// OK
    }


    //**********************************************************
    public static Path change_file_name(Path old_path, String new_name, Kontext context)
    //**********************************************************
    {
        if (dbg)  context.log("change_file_name, new name: " + new_name);
       //  context.log("trying rename: " + old_path.getFileName() + " => " + new_name);
        Path new_path = Paths.get(old_path.getParent().toString(), new_name);

        Old_and_new_Path oan = new Old_and_new_Path(old_path, new_path, Command.command_rename, Status.before_command, false);
        List<Old_and_new_Path> l = new ArrayList<>();
        l.add(oan);
        List<Old_and_new_Path> done = Moving_files.actual_safe_moves(l, true, context);
        if ( done.isEmpty()) return null;

        return new_path;
    }




    //**********************************************************
    public static Path ask_user_for_new_file_name(Path path, Kontext context)
    //**********************************************************
    {
        String old_name = path.getFileName().toString();

        TextInputDialog dialog = new TextInputDialog(old_name);
        Look_and_feel_manager.set_dialog_look(dialog, context.logger());
        dialog.getDialogPane().setMinWidth(800);
        dialog.initOwner(context.owner());
        {
            String text = My_I18n.get_I18n_string("Rename",  context);// to: " + parent.toAbsolutePath().toString();
            dialog.setTitle(text);
        }
        {
            String text = My_I18n.get_I18n_string("Rename_Explanation",  context);// to: " + parent.toAbsolutePath().toString();
            dialog.setHeaderText(text);

        }
        {
            String text = My_I18n.get_I18n_string("New_name",  context);// to: " + parent.toAbsolutePath().toString();
            dialog.setContentText(text);
        }
        // The Java 8 way to get the response value (with lambda expression).
        //result.ifPresent(name ->  context.log("Your name: " + name));
        // Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent()) {
             context.log("ask_user_for_new_file_name: no result? cancel I hope?");
            return null;
        }

        String new_name = result.get();
        if (new_name.equals(old_name)) {
             context.log("Rename not done as names are same ->" + old_name + "=" + new_name + "<-");
            return null;
        }
         context.log("ask_user_for_new_file_name ->" + old_name + "<-   ==>   ->" + new_name + "<-");

        if (Extensions.get_extension(new_name).isEmpty()) {
            if (!Extensions.get_extension(old_name).isEmpty()) {
                 context.log(Logger.warning+" WARNING, should not remove extension");
                if (Guess_file_type.is_this_path_extension_an_image(path, context) || Guess_file_type.is_this_path_extension_a_video(path, context.logger())) {
                     context.log(Logger.warning+" WARNING, extension restored");
                    new_name = Extensions.add(new_name ,Extensions.get_extension(old_name));
                    Popups.popup_warning( Logger.warning+" extension restored: ", old_name + "=>" + new_name, true,  context);
                } else {
                     context.log(Logger.warning+" WARNING, should not remove extension");
                    Popups.popup_warning( Logger.warning+" extension lost?", old_name + "had an extension... and you entered a name without extension? :" + new_name, false,  context);
                }

            } else {
                if (!Extensions.get_extension(new_name).equals(Extensions.get_extension(old_name))) {
                    Popups.popup_warning( Logger.warning+" extension check:", "you changed the file name extension", false,  context);
                }
            }
        }
        Path returned = Paths.get(path.getParent().toAbsolutePath().toString(), new_name);
         context.log("ask_user_for_new_file_name returns ->" + returned + "<-");
        return returned;

    }

    //**********************************************************
    public static Path ask_user_for_new_dir_name(Path path, Kontext context)
    //**********************************************************
    {
        String old_name = path.getFileName().toString();
        TextInputDialog dialog = new TextInputDialog(old_name);
        Look_and_feel_manager.set_dialog_look(dialog, context.logger());
        dialog.initOwner(context.owner());
        {
            String text = My_I18n.get_I18n_string("Folder_Copy_Name",  context);// to: " + parent.toAbsolutePath().toString();
            dialog.setTitle(text);
        }
        {
            String text = My_I18n.get_I18n_string("Folder_Copy_Name_Explanation",  context);// to: " + parent.toAbsolutePath().toString();
            dialog.setHeaderText(text);

        }
        {
            String text = My_I18n.get_I18n_string("Folder_Copy_Name_Explanation",  context);// to: " + parent.toAbsolutePath().toString();
            dialog.setContentText(text);
        }
        // The Java 8 way to get the response value (with lambda expression).
        //result.ifPresent(name ->  context.log("Your name: " + name));
        // Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent()) {
            // context.log("ask_user_for_new_dir_name no result? cancel I hope?");
            return null;
        }

        String new_name = result.get();
        if (new_name.equals(old_name)) {
             context.log("ask_user_for_new_dir_name: Rename not done as names are same ->" + old_name + "=" + new_name + "<-");
            return null;
        }
        // context.log("ask_user_for_new_dir_name ->" + old_name + "<-   ==>   ->" + new_name + "<-");

        Path returned = Paths.get(path.getParent().toAbsolutePath().toString(), new_name);
        // context.log("ask_user_for_new_dir_name returns ->" + returned + "<-");
        return returned;
    }

    //**********************************************************
    public static boolean is_same_path(Path p1, Path p2, Kontext context)
    //**********************************************************
    {
        if (p1 == null) return false;
        if (p2 == null) return false; // happens for delete forever
        try {
            return Files.isSameFile(p1, p2);
        } catch (IOException e) {
            // happens if for one of the path, no file exists
            if (dbg)  context.log_exception("WARNING: Exception in Files.isSameFile() :", e);
        }
        return false;

    }


    //**********************************************************
    public static void copy_dir_in_a_thread( Path path, Path new_path, Kontext context)
    //**********************************************************
    {
        if ( dbg)  context.log("copy_dir_in_a_thread start");
        Runnable r = () -> {
            boolean status = copy_dir(path, new_path, context);
            if (status) {
                if (dbg)  context.log(Logger.ok+" Folder copy done: " + new_path);
            }
            else
            {
                 context.log(Logger.error+"Folder copy error!");
                Jfx_batch_injector.inject(() -> Popups.popup_warning(Logger.error+"copy of folder failed", "see the logs", false,  context),context);
            }
        };
        try {
            Actor_engine.execute(r,"Copy folder", context.logger());
            if ( dbg)  context.log(Logger.ok+" copy_dir_in_a_thread LAUNCHED");
        } catch (RejectedExecutionException ree) {
             context.log(Logger.error+"copy_dir_in_a_thread()" + ree);
        }

    }

    //**********************************************************
    public static boolean copy_dir(Path origin, Path new_path, Kontext context)
    //**********************************************************
    {
        File src = origin.toFile();
        File dst = new_path.toFile();
        try
        {
            FileUtils.copyDirectory(src,dst);
            return true;
        }
        catch (IOException e)
        {
             context.log(Logger.error+"Folder copy failed "+e);
            Popups.popup_warning( My_I18n.get_I18n_string(Logger.error+"Folder copy failed",  context), "Folder copy failed: "+e, false,  context);
        }

        return false;
    }

    //**********************************************************
    private static boolean copy_dir2(Path origin, Path new_path, Kontext context)
    //**********************************************************
    {

        if (origin.toAbsolutePath().toString().equals(new_path.toAbsolutePath().toString())) {
             context.log(Logger.error+"cannot copy: names are same !");
            return false;
        }
        if ( !copy_directory(origin, new_path,context))
        {
            return false;
        }

        List<Old_and_new_Path> l = new ArrayList<>();
        Old_and_new_Path oan = new Old_and_new_Path(null, new_path, Command.command_copy, Status.copy_done, false);
        l.add(oan);
        Change_gang.report_changes(l,context.owner());
        return true;

    }



    //**********************************************************
    public static Error_type explain_error(Path dir, Kontext context)
    //**********************************************************
    {
        try
        {
            BasicFileAttributes x = Files.readAttributes(dir, BasicFileAttributes.class);
             context.log(dir.toAbsolutePath()+", BasicFileAttributes: "+ BasicFileAttributes_to_string(x));
        }
        catch (AccessDeniedException e2)
        {
             context.log(Stack_trace_getter.get_stack_trace(Logger.error+"ACCESS DENIED EXCEPTION" + e2));
            return Error_type.DENIED;
        }
        catch (NoSuchFileException e2)
        {
             context.log(Stack_trace_getter.get_stack_trace(Logger.error+"NoSuchFileException" + e2));
            // the DIR is gone !!
            return Error_type.NOT_FOUND;
        }
        catch (IOException e2)
        {
             context.log(Stack_trace_getter.get_stack_trace(e2.toString()));
            return Error_type.ERROR;
        }
        return Error_type.OK;
    }

    //**********************************************************
    private static String BasicFileAttributes_to_string(BasicFileAttributes x)
    //**********************************************************
    {
        return "isDirectory: " + x.isDirectory() + " isSymbolicLink: " + x.isSymbolicLink();
    }

    //**********************************************************
    public static long get_file_age_in_hours(File f, Kontext context)
    //**********************************************************
    {
        Duration age = get_file_age(f,context);
        if ( age == null) return Integer.MAX_VALUE;
        return age.toHours();
    }
    //**********************************************************
    public static long get_file_age_in_days(File f, Kontext  context)
    //**********************************************************
    {
        Duration age = get_file_age(f,context);
        if ( age == null) return Integer.MAX_VALUE;
        return age.toDays();
    }
    //**********************************************************
    public static Duration get_file_age(File f, Kontext context)
    //**********************************************************
    {
        BasicFileAttributes x = null;
        try {
            x = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
        } catch (IOException e) {
             context.log_exception("get_file_age",e);
            return null;
        }
        FileTime creation= x.creationTime();
        Instant c = creation.toInstant();
        Instant now = Instant.now();

        Duration age = Duration.between(c,now);
        return age;
    }

    public static Path get_cache_folder(Cache_folder cache_folder, Kontext context)
    {
        return get_absolute_hidden_dir_on_user_home(cache_folder.name(), false,  context);
    }

    public static Path get_face_reco_folder(Kontext context)
    {
        return get_absolute_hidden_dir_on_user_home(String_constants.FACE_RECO_DIR, false,  context);
    }





    // returns a directory using that relative name, on the user home, creates it if needed
    //**********************************************************
    public static Path get_absolute_hidden_dir_on_user_home(String relative_dir_name, boolean can_fail, Kontext context)
    //**********************************************************
    {
        if (dbg)  context.log("get_absolute_hidden_dir_on_user_home relative_dir_name=" + relative_dir_name);
        String home = System.getProperty(String_constants.USER_HOME);
        if (dbg)  context.log("user home =" + home);
        return from_top_folder(home, relative_dir_name, can_fail, context);
    }


    //**********************************************************
    public static Path from_top_folder(String top_folder, String relative_dir_name, boolean can_fail, Kontext context)
    //**********************************************************
    {
        Path conf_dir1 = Paths.get(top_folder, String_constants.CONF_DIR);
        if (dbg)  context.log("conf_dir1 =" + conf_dir1);
        if (!conf_dir1.toFile().exists())
        {
            try {
                Files.createDirectory(conf_dir1);
                if (dbg) context.log("conf_dir1 =" + conf_dir1+" directory created");

            } catch (IOException e) {
                String err = " Attempt to create a directory named->" + conf_dir1.toAbsolutePath() + "<- failed (1)" + e;
                if (can_fail) {
                    context.log(err);
                    return null;
                }
                Popups.popup_Exception(e, 300, err, context);
                return null;
            }
        }

        // do it deeper, this way icons don't show up in $home/.klikr to avoid privacy violation when browsing $home

        Path conf_dir2 = Paths.get(conf_dir1.toString(), String_constants.PRIVACY_SCREEN );
        if (dbg)  context.log("conf_dir2 =" + conf_dir2);

        if (!conf_dir2.toFile().exists())
        {
            try {
                Files.createDirectory(conf_dir2);
                if (dbg)  context.log("conf_dir2 =" + conf_dir2+" directory created");
            } catch (IOException e) {
                String err = " Attempt to create a directory named->" + conf_dir2.toAbsolutePath() + "<- failed (2)";
                Popups.popup_Exception(e, 300, err, context);
                return null;
            }
        }


        Path returned = Paths.get(conf_dir2.toAbsolutePath().toString(), relative_dir_name);
        if (dbg)  context.log("privacy screen dir=" + returned.toAbsolutePath());
        if (!Files.exists(returned))
        {
            try {
                Files.createDirectory(returned);
                return returned;
            }
            catch(FileAlreadyExistsException e)
            {
                 context.log("IGNORING privacy screen dir=" + e);
            }
            catch (IOException e) {
                String err = " Attempt to create a directory named->" + returned.toAbsolutePath() + "<- failed (3)";
                Popups.popup_Exception(e, 300, err, context);
                return null;
            }
        }
        if (dbg)  context.log("from_top_folder returning directory named->" + returned.toAbsolutePath() + "<- OK");
        return returned;
    }

    // returns a directory using that relative name
    //**********************************************************
    public static Path get_trash_dir_of(Path for_this, Kontext context)
    //**********************************************************
    {
        // the idea of having multiple trash dirs is that when you are moving file to trash,
        // it is MUCH faster to move them to a trash dir on the same disk,
        // than to move them to the user home trash dir,
        // this is especially important on slow/network drives

        Path full_path = for_this.toAbsolutePath();
        if ((!full_path.toString().equals("/Volumes")) && (full_path.toString().startsWith("/Volumes"))) {
            // this is MacOS...
            // context.log("what is trash_dir for :" + for_this.toAbsolutePath());
            Path volume = get_MacOS_volume(for_this, context);
            if (volume == null) {
                 context.log(Logger.error+"PANIC get_trash_dir " + for_this.toAbsolutePath() + " fails");
            }
            Path candidate =from_top_folder(volume.toString(), String_constants.TRASH_DIR, true, context);
            if ( candidate != null) return candidate;
            {
                // happens for OneDrive, where the real path is
                // /Volumes/Macintosh HD/Users/<name>/OneDrive -<companyname>/floder... etc
                // and /Volumes/Macintosh HD is NOT writable, so cannot create a trash dir there
                // better use the user home
            }
        }

        Path trash_dir = get_absolute_hidden_dir_on_user_home(String_constants.TRASH_DIR, false, context);
        if (trash_dir == null) {
             context.log(Logger.error+"PANIC: trash dir unknown");
            return null;
        }
        if (dbg)  context.log("trash_dir file=" + trash_dir.toAbsolutePath());
        return trash_dir;
    }

    //**********************************************************
    private static Path get_MacOS_volume(Path for_this, Kontext context)
    //**********************************************************
    {
        // context.log("get_MacOS_volume ENTRY = "+for_this);

        Path volume = for_this.toAbsolutePath();
        for (;;)
        {
            Path test = volume.getParent();
            if (test == null) {
                // context.log("get_MacOS_volume test == null ");
                break;
            }
            if (test.getFileName() == null) {
                // context.log("get_MacOS_volume test == null ");
                break;
            }
            // context.log("get_MacOS_volume testing "+test);
            if (test.toString().equals("/Volumes")) {
                // context.log("get_MacOS_volume returning " + volume);
                return volume;
            }
            volume = volume.getParent();
            if (volume.toString().equals("/")) break;
        }
         context.log("get_MacOS_volume FAILED! ");
        return null;

    }


    //**********************************************************
    public static List<Path> get_existing_trash_dirs(Kontext context)
    //**********************************************************
    {
        List<Path> trashes = new ArrayList<>();
        for (File f : File.listRoots())
        {
             context.log("get_existing_trash_dirs root ->"+f+"<-");
            if (f.toString().equals("/"))
            {
                // unix system...
                Path trash_dir = get_absolute_hidden_dir_on_user_home(String_constants.TRASH_DIR, false, context);
                trashes.add(trash_dir);
                // unix system...
                Path volumes = Path.of("/", "Volumes");
                File[] files = volumes.toFile().listFiles();
                if (files == null) continue;
                for (File ff : files) {
                    if (ff.isDirectory()) {
                        Path test = Path.of(ff.toPath().toString(), String_constants.CONF_DIR);
                        if (Files.exists(test)) {
                            trashes.add(test);
                        }
                    }
                }
            }
            else
            {
                if (f.getAbsolutePath().startsWith("C:"))
                {
                    Path trash_dir = get_absolute_hidden_dir_on_user_home(String_constants.TRASH_DIR, false, context);
                     context.log("get_existing_trash_dirs, windows , trash for+"+f+" is : "+trash_dir);
                    trashes.add(trash_dir);
                }
                else
                {
                    Path trash_dir = from_top_folder(f.toPath().toString(), String_constants.TRASH_DIR, true, context);
                     context.log("get_existing_trash_dirs, windows , trash for+"+f+" is : "+trash_dir);
                    trashes.add(trash_dir);
                }
            }
        }
        return trashes;
    }



}
