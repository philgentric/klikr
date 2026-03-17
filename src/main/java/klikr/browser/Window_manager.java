package klikr.browser;

import javafx.application.Platform;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.Shared_services;
import klikr.util.log.Logger;
import klikr.util.mmap.Mmap;
import klikr.util.mmap.Save_and_what;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

// counts how many Stages (all any type) are still alive,
// and on zero, calls System.exit(),
// bit ugly, but only way to make sure we "always" make a clean exit
// AND we properly save the mmap cache
//**********************************************************
public class Window_manager
//**********************************************************
{
    private static final boolean dbg = false;
    private static final AtomicInteger ID_generator = new AtomicInteger(0);
    private static final ConcurrentLinkedQueue<Integer> alive =  new ConcurrentLinkedQueue<>();

    //**********************************************************
    public static int register()
    //**********************************************************
    {
        int returned = ID_generator.getAndIncrement();
        Window_manager.alive.add(returned);
        return returned;
    }

    //**********************************************************
    public static void unregister(int id, Logger logger)
    //**********************************************************
    {
        alive.remove(id);
        logger.log("closing a window, remaining ="+alive.size());
        if (alive.isEmpty())
        {
            //if ( dbg)
            logger.log("last window dies");
            Shared_services.aborter().abort("primary_stage closing");
            if ( Feature_cache.get(Feature.Enable_mmap_caching))
            {
                CountDownLatch cdl = new CountDownLatch(1);
                Mmap.instance.save_index(new Save_and_what(cdl));
                try {
                    cdl.await();
                } catch (InterruptedException e) {
                    logger.log("" + e);
                }
            }
            //if (dbg)
            logger.log("last window closing GOING TO CALL Platform.exit()");
            Platform.exit();
            //if (dbg)
            logger.log("last window closing GOING TO CALL System.exit()");
            System.exit(0);
        }
        else
        {
            if ( dbg) logger.log("number_of_windows > 0");
        }
    }

    //**********************************************************
    public static int how_many_windows()
    //**********************************************************
    {
        return alive.size();
    }
}
