package klikr.util.cache;

import javafx.stage.Window;
import klikr.util.Kontext;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static klikr.util.Shared_services.aborter;

//**********************************************************
public class Binary_file_engine<V> implements Disk_engine<V>
//**********************************************************
{
    private static final boolean dbg = false;
    private static final boolean ultra_dbg = false;

    public final String name;
    public final Path cache_file_path;
    public final BiPredicate<V, DataOutputStream> value_serializer;
    public final Function<DataInputStream, V> value_deserializer;
    public final Kontext context;

    //**********************************************************
    public Binary_file_engine(
            String name,
            Path cache_file_path,
            BiPredicate<V, DataOutputStream> value_serializer,
            Function<DataInputStream, V> value_deserializer,
            Kontext context
    )
    //**********************************************************
    {
        this.cache_file_path = cache_file_path;
        this.value_serializer = value_serializer;
        this.value_deserializer = value_deserializer;
        this.context = context;
        this.name = name;
    }

    //**********************************************************
    @Override
    public int load_from_disk(Map<String, V> cache)
    //**********************************************************
    {
        long start = System.currentTimeMillis();

        int reloaded = 0;
        try(DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(cache_file_path.toFile()))))
        {
            int number_of_items = dis.readInt();
            if (dbg) context.log("number_of_items in cache :"+number_of_items);

            for ( int k = 0; k < number_of_items; k++)
            {
                if ( context.should_abort())
                {
                    if (dbg) context.log("aborting cal reload "+context.aborter().reason());
                    return reloaded;
                }
                String key = dis.readUTF();
                if (ultra_dbg) context.log("key "+key);
                V value = value_deserializer.apply(dis);
                if ( value == null)
                {
                    if (dbg) context.log(Stack_trace_getter.get_stack_trace("FATAL"));
                    return reloaded;
                }
                if (ultra_dbg) context.log("value "+value);
                cache.put(key,value);
                if ( k%10000 == 0) context.log(k +" items loaded from disk ....");
                reloaded++;
            }
            if (dbg) context.log("Done: "+reloaded+" items loaded from disk");
            return reloaded;
        }
        catch (FileNotFoundException e)
        {
            if (dbg) context.log("first time in this folder: "+e);
        }
        catch (IOException e)
        {
            if (dbg) context.log(Stack_trace_getter.get_stack_trace(""+e));
        }
        context.log("reloading "+reloaded+" similarities from disk took "+(System.currentTimeMillis()-start)+" ms");

        return reloaded;
    }

    //**********************************************************
    @Override
    public int save_to_disk(Map<String, V> cache)
    //**********************************************************
    {

        int saved = 0;
        try(DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(cache_file_path.toFile()))))
        {
            dos.writeInt(cache.size());
            for(Map.Entry<String, V> e : cache.entrySet())
            {
                String key = e.getKey();
                dos.writeUTF(key);
                if ( !value_serializer.test(e.getValue(),dos))
                {
                    context.log(Stack_trace_getter.get_stack_trace(" Panic"));
                    break;
                }
                saved++;
                //context.log("to disk similarity "+e.getValue()+" for "+pi1.getFileName().toString()+" "+pi2.getFileName().toString());
            }
        }
        catch (IOException e)
        {
            context.log(Stack_trace_getter.get_stack_trace(""+e));
        }

        if (dbg) context.log(saved +" items from cache saved to file");
        return saved;
    }

}
