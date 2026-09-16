// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.similarity;

import javafx.stage.Window;
import klikr.util.Kontext;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Message;

import java.nio.file.Path;

//**********************************************************
public class Similarity_cache_warmer_message implements Message
//**********************************************************
{

    private final Kontext context;
    final Path p1;
    final int index_of_p1;

    //**********************************************************
    public Similarity_cache_warmer_message(Path p1, int index, Kontext context)
    //**********************************************************
    {
        this.context = context;
        this.p1 = p1;
        this.index_of_p1 = index;
    }
    @Override
    public String thread_name() {
        return "Similarity cache warmer for: "+p1;
    }

    @Override
    public Aborter get_aborter() {
        return context.aborter();
    }

    public Window get_owner() { return context.owner(); }

}
