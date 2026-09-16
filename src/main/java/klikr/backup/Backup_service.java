// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.backup;
//SOURCES ./Backup_engine.java

import klikr.util.Kontext;
import klikr.util.log.Logger;
import klikr.util.ui.Popups;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//**********************************************************
public class Backup_service
//**********************************************************
{
    private static volatile Backup_service instance;
     private final List<Backup_engine> engines = new ArrayList<>();


    //**********************************************************
    public static boolean start_the_backup(Path source, Path destination, Kontext context)
    //**********************************************************
    {
        if ( instance == null)
        {
            instance = new Backup_service();
        }
        instance.start(source, destination, context);
        return true;
    }

    //**********************************************************
    private void start(Path source, Path destination,Kontext k)
    //**********************************************************
    {
        k.log("Starting backup...");
        Iterator<Backup_engine> it = engines.iterator();
        while ( it.hasNext())
        {
            Backup_engine e = it.next();
            if ( e.is_finished()) it.remove();
            else
            {
                if (e.source.equals(source))
                {
                    if (e.destination.equals(destination)) {
                        Popups.popup_warning(Logger.warning+" A backup like this is already running", "Sorry: denied",true,k);
                        return;
                    }
                }
            }
        }
        k.log("Starting backup 2...");

        {
// Get a CONFIRMATION
            String header = Logger.warning+" Copy Confirmation Required";
            String content = "This will copy all the files down from directory:\n" + source.toAbsolutePath() + "\n"
                    + "Into the directory:\n" + destination.toAbsolutePath() + "\n"
                    + "(this is safe because files with same names, if different, will be renamed)\n"
                    + "Are you sure you want to do that ?";

            if (!Popups.popup_ask_for_confirmation( header, content, k)) return;
        }
        boolean deep = false;
        {
        String header = "How deep should the file identity checks be?";
        String content = "Deep means= check every byte matches, cancel means = not deep, check just names and file sizes";

        deep = Popups.popup_ask_for_confirmation(header, content, k);
        }

        //k.log("Starting backup 3...");

        k.log("backup deep = "+deep);
        Backup_engine b = new Backup_engine(source, destination, k);
        b.go(deep,k);
        engines.add(b);
    }

    //**********************************************************
    public static void abort(Logger logger)
    //**********************************************************
    {
        if ( instance==null) return;
        instance.abort_now(logger);
    }

    //**********************************************************
    private void abort_now(Logger logger)
    //**********************************************************
    {
        for ( Backup_engine be : engines)
        {
            logger.log("CANCEL for "+be.to_string());
            be.abort();
        }
    }



    //**********************************************************
    private static Backup_service get_instance(Logger logger_)
    //**********************************************************
    {
        if (instance == null)
        {
            synchronized (Backup_service.class)
            {
                if (instance == null)
                {
                    instance = new Backup_service();
                }
            }
        }

        return instance;
    }


}
