// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.feature_vector;

import klikr.util.Kontext;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Message;

import java.nio.file.Path;

//**********************************************************
public class Feature_vector_build_message implements Message
//**********************************************************
{

    public final Path path;
    public final Kontext context;
    public final Feature_vector_cache feature_vector_cache;

    //**********************************************************
    public Feature_vector_build_message(Path path, Feature_vector_cache feature_vector_cache, Kontext context)
    //**********************************************************
    {
        this.path = path;
        this.context = context;
        this.feature_vector_cache = feature_vector_cache;

    }

    //**********************************************************
    @Override
    public Aborter get_aborter() {
        return context.aborter();
    }

    //**********************************************************
    @Override
    public String thread_name()
    //**********************************************************
    {
        return "Building Feature vector for: " + path;
    }

}
