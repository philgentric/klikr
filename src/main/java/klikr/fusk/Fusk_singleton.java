// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Defusk_actor_for_one_folder.java
//SOURCES ./Fusk_actor_for_one_folder.java
package klikr.fusk;

import klikr.util.Kontext;

import java.nio.file.Path;

import static klikr.fusk.Defusk_actor_for_one_folder.defusk_this_folder;
import static klikr.fusk.Fusk_actor_for_one_folder.fusk_this_folder;

//**********************************************************
public class Fusk_singleton
//**********************************************************
{
    private static volatile Fusk_singleton instance;
    Path source;
    Path destination;
    private final Kontext context;
    //**********************************************************
    public Fusk_singleton(Kontext context)
    //**********************************************************
    {
        this.context = context;
    }

    //**********************************************************
    public static void abort()
    //**********************************************************
    {
        instance.context.abort("fusk instance abort");
    }


    //**********************************************************
    public static void set_source(Path fusk_source, Kontext context)
    //**********************************************************
    {
        Fusk_singleton b = get_instance(context);
        instance.source = fusk_source;
    }

    //**********************************************************
    private static Fusk_singleton get_instance(Kontext context)
    //**********************************************************
    {
        if (instance == null)
        {
            synchronized (Fusk_singleton.class)
            {
                if (instance == null)
                {
                    instance = new Fusk_singleton(context);
                }
            }
        }

        return instance;
    }

    //**********************************************************
    public static void set_destination(Path fusk_destination, Kontext context)
    //**********************************************************
    {
        Fusk_singleton b = get_instance(context);
        instance.destination = fusk_destination;
    }

    //**********************************************************
    public static boolean start_fusk()
    //**********************************************************
    {
        if ( instance == null) return false;
        fusk_this_folder(instance.source.toFile(), instance.destination.toFile(), instance.context);

        return true;
    }

    //**********************************************************
    public static boolean start_defusk()
    //**********************************************************
    {
        if ( instance == null) return false;
        defusk_this_folder(instance.source.toFile(), instance.destination.toFile(), instance.context);
        return true;
    }
}
