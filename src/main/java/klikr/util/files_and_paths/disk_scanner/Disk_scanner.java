// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.files_and_paths.disk_scanner;

import klikr.util.Kontext;
import klikr.util.execute.actor.Actor_engine;
import klikr.browsers.browser_core.icons.Error_type;
import klikr.util.execute.actor.Executor;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;

//**********************************************************
public class Disk_scanner implements Runnable
//**********************************************************
{
    private static final boolean dbg = false;
    final Path path;
    final String origin;
    final File_payload file_payload;
    final Dir_payload dir_payload;
    final ConcurrentLinkedQueue<String> warning_payload;

    private final LongAdder file_count_stop_counter;
    private final LongAdder folder_count_stop_counter;

    public final Kontext context;

    // this will BLOCK until the tree has been traversed
    //**********************************************************
    public static void process_folder(
            Path path,
            String origin,
            File_payload file_payload_,
            Dir_payload dir_payload_,
            ConcurrentLinkedQueue<String> warning_payload_,
            Kontext context)
    //**********************************************************
    {
        /*
        if ( !path.toFile().isDirectory())
        {
            logger.log(Stack_trace_getter.get_stack_trace(origin+" stupid: not a folder "+path));
            return;
        }
        */
        if (Files.isSymbolicLink(path))
        {
            context.log(origin+" WARNING: Scanner not going down symbolic link for folder: "+path);
            return;
        }
        else
        {
            if( dbg) context.log(origin+" Scanner going down (not a symbolic links) on folder: "+path);
        }
        long start = 0L;
        if (dbg) start = System.currentTimeMillis();
        LongAdder folder_count_stop_counter = new LongAdder();
        LongAdder file_count_stop_counter = new LongAdder();
        launch_folder_in_a_thread_(
                path,
                origin,
                file_count_stop_counter,
                folder_count_stop_counter,
                file_payload_,
                dir_payload_,
                warning_payload_,
                context);

        // blocking part: we are sleeping/waiting until the tree is fully processed
        // this is a race condition if the jobs in the threads make the folder count go to zero, while the job is not yet finished
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            context.log_stack_trace(origin+e.toString());
        }
        for(;;)
        {
            if (context.should_abort())
            {
                //logger.log("ABORTED: Scanner monitoring for "+path);
                return;
            }
            try {
                Thread.sleep((int)(10+folder_count_stop_counter.doubleValue()));
                //logger.log("how_many_folders="+how_many_folders);
            } catch (InterruptedException e) {
                context.log_stack_trace(origin+e.toString());
            }
            if (( folder_count_stop_counter.doubleValue() == 0) &&( file_count_stop_counter.doubleValue() == 0) )break;

        }
        if (dbg)
        {
            long end = System.currentTimeMillis();
            context.log(origin+" file tree processing time: "+(end-start)+"ms");
        }

    }
    //**********************************************************
    private static void launch_folder_in_a_thread_(
            Path path,
            String origin,
            LongAdder file_count_stop_counter,
            LongAdder folder_count_stop_counter,
            File_payload file_payload_,
            Dir_payload dir_payload_,
            ConcurrentLinkedQueue<String> warning_payload_,
            Kontext context)
    //**********************************************************
    {
        folder_count_stop_counter.increment();

        Runnable r = new Disk_scanner(path, origin, file_count_stop_counter, folder_count_stop_counter, file_payload_, dir_payload_, warning_payload_, context);
        Actor_engine.execute(r,"Disk scanner, scan folder: "+path,context.logger());
    }
    //**********************************************************
    private Disk_scanner(
            Path path_,
            String origin_,
            LongAdder file_count_stop_counter_,
            LongAdder folder_count_stop_counter_,
            File_payload file_payload_,
            Dir_payload dir_payload_,
            ConcurrentLinkedQueue<String> warning_payload_,
            Kontext context)
    //**********************************************************
    {
        this.context = context;

        path = path_;
        origin = origin_;
        file_payload = file_payload_;
        dir_payload = dir_payload_;
        warning_payload = warning_payload_;
        file_count_stop_counter = file_count_stop_counter_;
        folder_count_stop_counter = folder_count_stop_counter_;
    }


    //**********************************************************
    @Override
    public void run()
    //**********************************************************
    {
        if (context.should_abort())
        {
            //logger.log("ABORTED1: Scanner for "+path);
            return;
        }
        File[] all_files = path.toFile().listFiles();
        if ( all_files == null)
        {
            {
                context.log (origin+ " Scanner: listFiles() returns null for: "+path);
                Error_type error = Static_files_and_paths_utilities.explain_error(path,context);
            }

            folder_count_stop_counter.decrement();
            return ;
        }
        for (File f : all_files)
        {
            if (context.should_abort())
            {
                //logger.log("ABORTED2: Scanner for "+path);
                break;
            }
            if (f.isDirectory())
            {
                if ( Files.isSymbolicLink(f.toPath()))
                {
                    String x = origin+" warning: disk scanner not following symbolic link folder:" + f;
                    if ( dbg) context.log(x);
                    if ( warning_payload!=null) warning_payload.add(x);
                }
                else
                {
                    if ( dir_payload != null) dir_payload.process_dir(f);
                    launch_folder_in_a_thread_(f.toPath(), origin, file_count_stop_counter, folder_count_stop_counter, file_payload, dir_payload, warning_payload, context);
                }
            }
            else
            {
                if ( file_payload!= null)
                {
                    file_count_stop_counter.increment();
                    if ((Executor.use_virtual_threads))
                    {
                        // with virtual threads we can afford to start one thread per file !
                        Actor_engine.execute(() -> {
                            file_payload.process_file(f);
                            file_count_stop_counter.decrement();
                        }, "Disk scanner, scan file: "+path,context.logger());
                    }
                    else
                    {
                        file_payload.process_file(f);
                        file_count_stop_counter.decrement();
                    }
                }
            }
        }
        folder_count_stop_counter.decrement();
    }




}
