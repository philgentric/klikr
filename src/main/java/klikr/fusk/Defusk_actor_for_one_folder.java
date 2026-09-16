// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Fusk_message.java
package klikr.fusk;

import klikr.util.Kontext;
import klikr.util.execute.actor.*;
import klikr.util.log.Logger;
import klikr.util.execute.actor.Executor;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentLinkedQueue;

//**********************************************************
public class Defusk_actor_for_one_folder implements Actor
//**********************************************************
{
    private final Kontext context;
    ConcurrentLinkedQueue<Job> jobs = new ConcurrentLinkedQueue<>();

    //**********************************************************
    Defusk_actor_for_one_folder(Kontext context)
    //**********************************************************
    {
        this.context = context;
    }


    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Defusk_actor_for_one_folder";
    }

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Fusk_message fm = (Fusk_message)m;
        File[] files = fm.target_dir.listFiles();
        if ( files ==null)
        {
            return null;
        }
        File destination_folder = fm.destination_folder;

        for ( File f : files)
        {
            if (context.should_abort())
            {
                abort();
                return null;
            }

            if (!Files.isSymbolicLink(f.toPath()))
            {
                if ( f.isDirectory())
                {
                        jobs.add(defusk_this_folder(f,destination_folder,context));
                }
                else
                {

                    if ( Executor.use_virtual_threads)
                    {
                        Runnable r = () -> Fusk_static_core.defusk_file(f.toPath(), destination_folder.toPath(), context);
                        Actor_engine.execute(r,"Defusk a file", context.logger());
                    }
                    else
                    {
                        Fusk_static_core.defusk_file(f.toPath(), destination_folder.toPath(), context);
                    }
                }
            }
        }
        return null;
    }

    //**********************************************************
    public static Job defusk_this_folder(File target_dir, File destination_folder, Kontext context)
    //**********************************************************
    {
        return Actor_engine.run(
                new Defusk_actor_for_one_folder(context), // need an instance for abort
                new Fusk_message(target_dir, destination_folder,context.aborter()),
                null,
                context.logger());
    }

    //**********************************************************
    public void abort()
    //**********************************************************
    {
        context.abort("defusk job aborting");
        Actor_engine.cancel_jobs(jobs);
    }

}
