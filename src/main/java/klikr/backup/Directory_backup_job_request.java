// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.backup;

import klikr.util.Kontext;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Message;
import klikr.util.log.Logger;

import java.io.File;

//**********************************************************
public class Directory_backup_job_request implements Message
//**********************************************************
{
    public final File source_dir;
    public final File destination_dir;
    public final Kontext context;
    public boolean finished = false;

    //**********************************************************
    public Directory_backup_job_request(File source_dir, File destination_dir,
                                        Kontext context)
    //**********************************************************
    {
        this.source_dir = source_dir;
        this.destination_dir = destination_dir;
        if ( context.aborter() == null)
        {
            context.log_stack_trace(Logger.error+"FATAL: aborter must not be null");
        }
        this.context = context;
    }

    //**********************************************************
    @Override
    public String thread_name()
    //**********************************************************
    {
        return "Directory backup for: "+source_dir+" => "+destination_dir;
    }

    @Override
    public Aborter get_aborter() {
        return context.aborter();
    }
}
