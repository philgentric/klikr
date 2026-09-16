// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.change.bookmarks;
//SOURCES ../../properties/Properties_with_base.java

import javafx.stage.Window;
import klikr.settings.File_storage;
import klikr.util.Kontext;
import klikr.util.Shared_services;
import klikr.settings.File_storage_using_Properties;
import klikr.settings.Properties_with_base;

import java.util.List;

//**********************************************************
public class Bookmarks
//**********************************************************
{
    private static volatile Bookmarks instance = null;
    private final Kontext context;
    private final File_storage file_storage;
    private final Properties_with_base pb;

    //**********************************************************
    public static Bookmarks get(Kontext context)
    //**********************************************************
    {
        if (instance == null)
        {
            synchronized (Bookmarks.class)
            {
                if (instance == null)
                {
                    instance = new Bookmarks(context);
                }
            }
        }

        return instance;
    }

    //**********************************************************
    private Bookmarks(Kontext context)
    //**********************************************************
    {
        this.context = context;
        file_storage = new File_storage_using_Properties("bookmarks","bookmarks",true, context);
        pb = new Properties_with_base(file_storage,"bookmark_",30, context.logger());
    }


    //**********************************************************
    public void add(String s)
    //**********************************************************
    {
        context.log("adding bookmark:"+s);
        pb.add(s);
    }
    //**********************************************************
    public void clear()
    //**********************************************************
    {
        pb.clear();
    }
    //**********************************************************
    public List<String> get_list()
    //**********************************************************
    {
        return pb.get_all();
    }
}
