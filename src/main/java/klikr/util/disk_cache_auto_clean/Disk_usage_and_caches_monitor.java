// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Disk_usage_monitor.java
//SOURCES ./Disk_cache_auto_clean.java
package klikr.util.disk_cache_auto_clean;

import klikr.Window_provider;
import klikr.settings.Non_booleans_properties;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.Shared_services;
import klikr.util.log.Logger;

//**********************************************************
public class Disk_usage_and_caches_monitor
//**********************************************************
{
    public final Logger logger;
    private final Disk_usage_monitor disk_usage_monitor;
    private final Disk_cache_auto_clean cache_auto_clean;

    //**********************************************************
    public Disk_usage_and_caches_monitor(Window_provider window_provider, Logger logger)
    //**********************************************************
    {
        this.logger = logger;

        // monitor cache folder SIZE
        disk_usage_monitor = new Disk_usage_monitor(window_provider.get_owner(), logger);

        // monitor cache files AGE
        int cache_max_days = Non_booleans_properties.get_animated_gif_duration_for_a_video(window_provider.get_owner());
        cache_auto_clean = new Disk_cache_auto_clean(cache_max_days,window_provider.get_owner(), logger);
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
                    logger.log("All 3 Monitors aborted");
                    return;
                }

                try {
                    Thread.sleep(10*60*1000);
                } catch (InterruptedException e) {
                    logger.log(""+e);
                }

                if ( !disk_usage_monitor.monitor()) break;
                if (cache_auto_clean!= null)
                {
                    if ( !cache_auto_clean.monitor()) break;
                }
                //if ( !history_auto_clean.monitor()) break;


            }
        };
        Actor_engine.execute(r,"Cache auto clean",logger);

    }
}
