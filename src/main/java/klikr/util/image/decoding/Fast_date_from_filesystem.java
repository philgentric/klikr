// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.image.decoding;

import klikr.util.Kontext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

//**********************************************************
public class Fast_date_from_filesystem
//**********************************************************
{
    public static final boolean dbg = false;

    //**********************************************************
    public static FileTime get_date(Path path, Kontext context)
    //**********************************************************
    {

        BasicFileAttributes attr;
        try
        {
            attr = Files.readAttributes(path, BasicFileAttributes.class);
            return attr.creationTime();
        }
        catch (IOException e)
        {
            if ( dbg)
            {
                context.log_with_stack_trace_from_throwable("extract_exif_metadata() Managed exception (1)->"+e+"<- for:"+ path.toAbsolutePath(),e);
            }
        }
        return FileTime.from(Instant.now());

    }

}
