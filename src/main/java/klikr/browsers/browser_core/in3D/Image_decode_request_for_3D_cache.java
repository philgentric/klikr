// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browsers.browser_core.in3D;

import javafx.stage.Window;
import klikr.util.Kontext;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Message;

import java.nio.file.Path;

//**********************************************************
public class Image_decode_request_for_3D_cache implements Message
//**********************************************************
{
    public final Path path;
    public final Image_cache_cafeine_for_3D cache;
    public final Kontext context;

    //**********************************************************
    public Image_decode_request_for_3D_cache(Path path,
                                             Image_cache_cafeine_for_3D cache,
                                             Kontext context)
    //**********************************************************
    {
        this.path = path;
        this.cache = cache;
        this.context = context;
    }

    //**********************************************************
    public static String get_key(Path path)
    //**********************************************************
    {
        return path.toAbsolutePath().toString();
    }




    //**********************************************************
    public String get_string()
    //**********************************************************
    {
        if ( path == null)         return "path:null";

        return " path:" + path.toAbsolutePath();
    }

    //**********************************************************
    @Override
    public String thread_name()
    //**********************************************************
    {
        return "image decoding for: "+path;
    }

    @Override
    public Aborter get_aborter() {
        return context.aborter();
    }
}
