// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Disk_usage_monitor.java
//SOURCES ./Disk_cache_auto_clean.java
package klikr.util.disk_cache_auto_clean;

import klikr.Owner_provider;
import klikr.settings.Non_booleans_properties;
import klikr.util.Kontext;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.Shared_services;
import klikr.util.log.Logger;

//**********************************************************
public class Disk_usage_and_caches_monitor
//**********************************************************
{
    public final Kontext context;
    private final Disk_usage_monitor disk_usage_monitor;
    private final Disk_cache_auto_clean cache_auto_clean;

    //**********************************************************
    public Disk_usage_and_caches_monitor(Kontext context)
    //**********************************************************
    {
        this.context = context;

        // monitor cache folder SIZE
        disk_usage_monitor = new Disk_usage_monitor(context);

        // monitor cache files AGE
        int cache_max_days = Non_booleans_properties.get_animated_gif_duration_for_a_video();
        cache_auto_clean = new Disk_cache_auto_clean(cache_max_days,context);
    }

    //**********************************************************
    public void start()
    //**********************************************************
    {
        Runnable r = () -> {
            for(;;)
            {
                if ( Shared_services.aborter().should_abort())
                {
                    context.log("All 3 Monitors aborted");
                    return;
                }

                try {
                    Thread.sleep(10*60*1000);
                } catch (InterruptedException e) {
                    context.log(""+e);
                }

                if ( !disk_usage_monitor.monitor()) break;
                if (cache_auto_clean!= null)
                {
                    if ( !cache_auto_clean.monitor()) break;
                }
                //if ( !history_auto_clean.monitor()) break;


            }
        };
        Actor_engine.execute(r,"Cache auto clean",context.logger());

    }
}
