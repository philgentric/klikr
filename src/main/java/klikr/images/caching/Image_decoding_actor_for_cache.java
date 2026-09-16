// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.images.caching;

import klikr.util.Kontext;
import klikr.util.execute.actor.Actor;
import klikr.util.execute.actor.Message;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.images.Image_context;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.util.Optional;

//**********************************************************
public class Image_decoding_actor_for_cache implements Actor
//**********************************************************
{
    private static final boolean dbg = false;
    private static final boolean ultra_dbg = false;
    final Kontext context;


    //**********************************************************
    public Image_decoding_actor_for_cache(Kontext context)
    //**********************************************************
    {
        this.context = context;
    }


    //**********************************************************
    @Override
    public String name()
    //**********************************************************
    {
        return "Image_decoding_actor_for_cache";
    }

    //**********************************************************
    @Override
    public String run(Message m)
    //**********************************************************
    {
        Image_decode_request_for_cache request = (Image_decode_request_for_cache) m;
        if (ultra_dbg) context.log("decode request:"+request.get_string());

        String key = request.make_key();
        if ( request.cache.get(key) != null)
        {
            if (dbg)
                context.log("NOT decoding because image found in cache:"+key);
            return"found in cache";
        }

        if ( m.get_aborter().should_abort()) return "aborted";

        // this is the expensive operation:
        Optional<Image_context> option = Image_context.build_Image_context(request.path, request.image_window);

        if (option.isPresent())
        {
            Image_context image_context = option.get();
            // OutOfMemory can manisfest either as an exception, and then we get a null
            // or the image is of length zero (!)
            if ( (image_context.image.getWidth() > 1) && (image_context.image.getHeight() > 1))
            {
                request.cache.put(request.make_key(), image_context);
                if (dbg) context.log(Logger.ok+"  image decoded ok is now in cache: " + image_context.path.getFileName() );
            }
            else
            {
                if (!Guess_file_type.should_ignore(image_context.path, context.logger()))
                {
                    context.log( Stack_trace_getter.get_stack_trace(image_context.path.getFileName().toString()
                            +" WARNING weird image: "+image_context.path.toAbsolutePath()+"\n we have: w="+image_context.image.getWidth() +" h="+image_context.image.getHeight()));
                }
            }
        }
        else
        {
            context.log( Stack_trace_getter.get_stack_trace(Logger.error+"BAD WARNING get_Image_and_index failed"));
        }
        return "OK, image decoded";
    }

}
