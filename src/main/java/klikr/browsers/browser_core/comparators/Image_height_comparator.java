// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browsers.browser_core.comparators;

import klikr.browsers.browser_core.icons.image_properties_cache.Image_properties;
import klikr.util.Kontext;
import klikr.util.cache.Klikr_cache;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.Comparator;

//**********************************************************
public record Image_height_comparator(
        Klikr_cache<Path, Image_properties> image_properties_cache,
        Kontext context) implements Comparator<Path>
//**********************************************************
{


    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        Integer x = Hidden_files.show_last(p1, p2);
        if (x != null) return x;

        Image_properties ip1 = image_properties_cache.get(p1,null,context);
        if (ip1 == null) {
            context.log(Stack_trace_getter.get_stack_trace(Logger.error+" PANIC image_property not found"));
            return 0;
        }
        Double d1 = ip1.get_image_height();
        if (d1 == null) {
            context.log(Stack_trace_getter.get_stack_trace(Logger.error+" PANIC image height not found"));
            return 0;
        }
        Image_properties ip2 = image_properties_cache.get(p2,null,context);
        if (ip2 == null) {
            context.log(Stack_trace_getter.get_stack_trace(Logger.error+"PANIC image_property not found"));
            return 0;
        }
        Double d2 = ip2.get_image_height();
        if (d2 == null) {
            context.log(Stack_trace_getter.get_stack_trace(Logger.error+"PANIC image height not found"));
            return 0;
        }

        int diff = d1.compareTo(d2);
        if (diff != 0) return diff;

        return (p1.getFileName().toString().compareTo(p2.getFileName().toString()));
    }
}
