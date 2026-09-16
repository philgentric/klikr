package klikr.util.cache;

import javafx.stage.Window;
import klikr.util.Kontext;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Message;

//**********************************************************
public class RAM_cache_message<K,V> implements Message
//**********************************************************
{
    public final K key;
    public final Kontext context;
    public final Klikr_cache<K,V> cache;
    public final boolean check_if_present;
    //**********************************************************
    public RAM_cache_message(K key, boolean check_if_present, Klikr_cache<K,V> cache, Kontext context)
    //**********************************************************
    {
        this.cache = cache;
        this.key = key;
        this.context = context;
        this.check_if_present = check_if_present;
    }

    //**********************************************************
    @Override
    public String thread_name()
    //**********************************************************
    {
        return "Caching to RAM for key: " + key;
    }

    //**********************************************************
    @Override
    public Aborter get_aborter()
    //**********************************************************
    {
        return context.aborter();
    }
}