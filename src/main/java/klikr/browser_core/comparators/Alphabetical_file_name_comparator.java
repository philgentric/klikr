// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../Clearable_RAM_cache.java
package klikr.browser_core.comparators;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Comparator;

//**********************************************************
public class Alphabetical_file_name_comparator implements Comparator<Path>
//**********************************************************
{
    //**********************************************************
    @Override
    public int compare(Path p1, Path p2)
    //**********************************************************
    {

        Integer x = Hidden_files.show_last(p1, p2);
        if (x != null) return x;

        int diff = p1.getFileName().toString().compareToIgnoreCase(p2.getFileName().toString());
        if (diff != 0) return diff;
        // in case the file names differ by case
        return p1.getFileName().toString().compareTo(p2.getFileName().toString());
    }


}
