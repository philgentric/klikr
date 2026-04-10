// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.path_lists;

import klikr.util.log.Logger;

import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Change_broadcaster
//**********************************************************
{
    public final Logger logger;
    List<Runnable> change_subscribers = new ArrayList<>();

    //**********************************************************
    public Change_broadcaster(Logger logger)
    //**********************************************************
    {
        this.logger = logger;
    }

    //**********************************************************
    public void add_change_subscriber(Runnable subscriber)
    //**********************************************************
    {
        change_subscribers.add(subscriber);
    }
    //**********************************************************
    public void call_all_change_subscribers()
    //**********************************************************
    {
        logger.log("Change_broadcaster: call_change_listeners() ");
        for (Runnable subscriber : change_subscribers)
        {
            subscriber.run();
        }
    }
}
