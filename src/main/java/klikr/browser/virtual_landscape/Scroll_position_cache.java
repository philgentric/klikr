package klikr.browser.virtual_landscape;

import klikr.util.cache.Clearable_RAM_cache;
import klikr.util.cache.Size_;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

//**********************************************************
public class Scroll_position_cache implements Clearable_RAM_cache
//**********************************************************
{
    public static final boolean scroll_cache_dbg = false;

    // when browsing disk, the key is the full folder path, as a string
    // for playlist, it is the playlist name
    // the value is always the full item path, as a string
    public final static Map<String, String> scroll_position_cache = new HashMap<>();

    //**********************************************************
    @Override
    public double clear_RAM()
    //**********************************************************
    {
        double returned = Size_.of_Map(scroll_position_cache,Size_.of_String_F(),Size_.of_String_F());
        scroll_position_cache.clear();
        return returned;
    }


    //**********************************************************
    public static void scroll_position_cache_write(String key, String top_left, String origin, Logger logger)
    //**********************************************************
    {
        if ( scroll_cache_dbg)
        {
            logger.log(("scroll_position_cache WRITE origin:\n     origin:"+origin+ "\n    key: "+key+ "\n    value: "+top_left));
        }
        scroll_position_cache.put(key, top_left);
    }

    //**********************************************************
    public static double scroll_position_cache_clear()
    //**********************************************************
    {
        double size = Size_.of_Map(scroll_position_cache,Size_.of_String_F(),Size_.of_String_F());
        scroll_position_cache.clear();
        return size;
    }

    //**********************************************************
    public static String scroll_position_cache_read(String tag)
    //**********************************************************
    {
        return scroll_position_cache.get(tag);
    }
}
