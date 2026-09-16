// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browsers.browser_core.comparators;

import klikr.util.Kontext;
import klikr.util.image.decoding.Fast_date_from_filesystem;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;

//**********************************************************
public record Date_comparator(Kontext context) implements Comparator<Path>
//**********************************************************
{
    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {
        Integer x = Hidden_files.show_last(p1, p2);
        if (x != null) return x;

        FileTime ldt1 = Fast_date_from_filesystem.get_date(p1, context);
        FileTime ldt2 = Fast_date_from_filesystem.get_date(p2, context);
        int diff = ldt2.compareTo(ldt1);
        if (diff != 0) return diff;

        return (p1.toString().compareTo(p2.toString()));
    }
}