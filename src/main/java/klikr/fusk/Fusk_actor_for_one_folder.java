// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.fusk;

import klikr.util.Kontext;
import klikr.util.execute.actor.*;
import klikr.util.log.Logger;
import klikr.util.execute.actor.Executor;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

//**********************************************************
public class Fusk_actor_for_one_folder implements Actor
//**********************************************************
{
    private final Kontext context;
    ConcurrentLinkedQueue<Job> jobs = new ConcurrentLinkedQueue<>();

    //**********************************************************
    Fusk_actor_for_one_folder(Kontext context)
    //**********************************************************
    {
        this.context = context;
    }


    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Fusk_actor_for_one_folder";
    }

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Fusk_message fm = (Fusk_message)m;
        File[] files = fm.target_dir.listFiles();
        if ( files ==null) return null;
        File destination_folder = fm.destination_folder;
        if ( !destination_folder.exists())
        {
            if (destination_folder.mkdir())
            {
                 context.log("FUSK: created folder: " + destination_folder);
            }
            else
            {
                 context.log(Logger.error+"FATAL ! could not create folder: " + destination_folder);
                return Logger.error+"FATAL ! could not create folder: " + destination_folder;
            }
        }
        for ( File f : files)
        {
            if ( context.should_abort()) return null;
            if ( f.isDirectory())
            {
                 context.log("FUSK: doing folder: " + f.getAbsolutePath());

                // generate a new actor job
                jobs.add(fusk_this_folder(f,destination_folder, context));
            }
            else
            {
                 context.log("FUSK: doing file: " + f.getAbsolutePath());

                if ( Executor.use_virtual_threads)
                {
                    Runnable r = () -> Fusk_static_core.fusk_file(f.toPath(), destination_folder.toPath(), context);
                    Actor_engine.execute(r,"Fusk a file", context.logger());
                }
                else {
                    Fusk_static_core.fusk_file(f.toPath(), destination_folder.toPath(), context);
                }
            }
        }
        return null;
    }

    //**********************************************************
    public static Job fusk_this_folder(File target_dir, File destination_folder, Kontext context)
    //**********************************************************
    {
         context.log("going to fusk:"+target_dir);
        return Actor_engine.run(
                new Fusk_actor_for_one_folder(context),
                new Fusk_message(target_dir,new File(destination_folder, target_dir.getName()), context.aborter()),
                null,
                context.logger());
    }

    //**********************************************************
    public void abort()
    //**********************************************************
    {
        context.abort("fusk job aborting");
        Actor_engine.get_instance().cancel_jobs(jobs);
    }
}
