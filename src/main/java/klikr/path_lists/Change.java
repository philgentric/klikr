// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.path_lists;

import klikr.util.log.Logger;

import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Change
//**********************************************************
{
    public final Logger logger;
    List<Runnable> change_listeners = new ArrayList<>();

    //**********************************************************
    public Change(Logger logger)
    //**********************************************************
    {
        this.logger = logger;
    }

    //**********************************************************
    public void add_change_listener(Runnable listener)
    //**********************************************************
    {
        change_listeners.add(listener);
    }
    //**********************************************************
    public  void call_change_listeners()
    //**********************************************************
    {
        logger.log("Change: call_change_listeners() ");
        for (Runnable r : change_listeners)
        {
            r.run();
        }
    }
}
