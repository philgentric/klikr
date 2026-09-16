// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browsers.browser_core.comparators;

import klikr.util.Kontext;
import klikr.util.P2S;
import klikr.util.cache.Clearable_RAM_cache;
import klikr.browsers.browser_core.icons.image_properties_cache.Image_properties;
import klikr.util.cache.Klikr_cache;
import klikr.util.cache.Size_;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

//**********************************************************
public class Aspect_ratio_comparator_random implements Comparator<Path>, Clearable_RAM_cache
//**********************************************************
{
    private final long seed;
    // make sure the comparator is consistent
    private final HashMap<String,Long> cache_local = new HashMap<>();
    private final Klikr_cache<Path, Image_properties> image_properties_cache;
    private final Kontext context;
    //**********************************************************
    public Aspect_ratio_comparator_random(Klikr_cache<Path, Image_properties> image_properties_cache, Kontext context) 
    //**********************************************************
    {
        this.context = context;
        this.image_properties_cache = image_properties_cache;
        Random r = new Random();
        seed = r.nextLong();
    }

    //**********************************************************
    @Override
    public double clear_RAM()
    //**********************************************************
    {
        double returned = Size_.of_Map(cache_local,Size_.of_String_F(),Size_.of_Long_F());
        cache_local.clear();
        return returned;
    }
    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        Integer x = Hidden_files.show_last(p1, p2);
        if (x != null) return x;

        Image_properties ip1 = image_properties_cache.get(p1,null, context);
        if ( ip1 == null)
        {
            //logger.log(Stack_trace_getter.get_stack_trace("PANIC image_property not found"));
            return 0;
        }
        Double d1 = ip1.get_aspect_ratio();
        Image_properties ip2 = image_properties_cache.get(p2,null, context);
        if ( ip2 == null)
        {
            //logger.log(Stack_trace_getter.get_stack_trace("PANIC image_property not found"));
            return 0;
        }
        Double d2 = ip2.get_aspect_ratio();

        int diff = d1.compareTo(d2);
        if (diff != 0) return diff;

        Long l1 = cache_local.get(P2S.p2s(p1));
        if ( l1 == null) {
            // same aspect ratio so the order must be pseudo random... but consistent for each comparator instance
            long s1 = UUID.nameUUIDFromBytes(p1.getFileName().toString().getBytes()).getMostSignificantBits();
            l1 = new Random(seed * s1).nextLong();
            cache_local.put(P2S.p2s(p1),l1);
        }

        Long l2 = cache_local.get(P2S.p2s(p2));
        if ( l2 == null) {
            // same aspect ratio so the order must be pseudo random... but consistent for each comparator instance
            long s2 = UUID.nameUUIDFromBytes(p2.getFileName().toString().getBytes()).getMostSignificantBits();
            l2 = new Random(seed * s2).nextLong();
            cache_local.put(P2S.p2s(p2), l2);
        }

        return l1.compareTo(l2);
    }
}
