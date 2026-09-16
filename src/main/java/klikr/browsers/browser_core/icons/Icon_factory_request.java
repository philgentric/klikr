// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browsers.browser_core.icons;

import javafx.stage.Window;
import klikr.util.Kontext;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Message;

//**********************************************************
public class Icon_factory_request implements Message
//**********************************************************
{
    public final int icon_size;
    public final Icon_destination destination;
    public final Kontext context;
    public int retry_count = 0;
    public final static int max_retry = 3;

    //**********************************************************
    public Icon_factory_request(Icon_destination destination, int icon_size,Kontext context)
    //**********************************************************
    {
        this.icon_size = icon_size;
        this.destination = destination;
        this.context = context;
    }

    @Override
    public String thread_name() {
        return "Icon_factory_request for: "+destination.get_string();
    }

    @Override
    public Aborter get_aborter() {
        return context.aborter();
    }


}
