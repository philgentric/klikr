// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.backup;

import klikr.util.Kontext;
import klikr.util.log.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

/*
this is NOT re-entrant : you need one instance per thread
 */
//**********************************************************
public class File_comparator
//**********************************************************
{
    private static final int BUFFER_SIZE = 2_000_000;//8192;//4096;//1024;

    private final Kontext context;
    private static final boolean debug_flag = false;
    private final byte[] hashsrc = new byte[BUFFER_SIZE];
    private final byte[] hashdest = new byte[BUFFER_SIZE];


    //**********************************************************
    File_comparator(Kontext context)
    //**********************************************************
    {
        if ( context.aborter() == null)
        {
            context.log_stack_trace(Logger.error+"FATAL aborter must not be null");
        }
        this.context = context;
    }
    //**********************************************************
    public void abort()
    //**********************************************************
    {
        context.abort("File_comparator abort");
    }
    //**********************************************************
    public  Similarity_result files_have_same_size(
            File file_to_be_copied,
            File destination_file)
    //**********************************************************
    {
        long ori_s = file_to_be_copied.length();
        long des_s = destination_file.length();
        if (ori_s != des_s) {
            if (debug_flag) {
                context.log("file sizes differ " + file_to_be_copied.getName() + " " + ori_s + " vs " + des_s);
            }
            return Similarity_result.not_same;
        }

        return Similarity_result.same_size;

    }

    //**********************************************************
    public  Similarity_result files_are_same(
            File file_to_be_copied,
            File destination_file,
            long[] bytes_read)
    //**********************************************************
    {
        long ori_s = file_to_be_copied.length();
        long des_s = destination_file.length();
        if (ori_s != des_s)
        {
            if (debug_flag) {
                context.log("file sizes differ " + file_to_be_copied.getName() + " " + ori_s + " vs " + des_s);
            }
            return Similarity_result.not_same;
        }


        // same length, let us check bytes, block per block
        // at the first sign of a difference, return false
        try (
                BufferedInputStream bis1 = new BufferedInputStream(new FileInputStream(file_to_be_copied));
                BufferedInputStream bis2 = new BufferedInputStream(new FileInputStream(destination_file))
        )
        {
            for (;;)
            {
                if (context.should_abort())
                {
                    context.log("abort from files_are_same()");
                    return Similarity_result.aborted;
                }

                int available_src = bis1.read(hashsrc, 0, BUFFER_SIZE);
                int available_dest = bis2.read(hashdest, 0, BUFFER_SIZE);
                if (( available_src == -1 ) && ( available_dest == -1 ))
                {
                    // this is the end
                    break;
                }
                bytes_read[0] += available_src+available_dest;

                if ( available_src != available_dest )
                {
                    context.log("sizes differ");
                    return Similarity_result.not_same;
                }
                if (Arrays.mismatch(hashsrc, hashdest) != -1)
                {
                    //if ( debug_flag == true)
                    context.log("content differ");
                    return Similarity_result.not_same;
                }
            }
            return Similarity_result.same;
        } // end try
        catch (Exception ioe) {
            context.log(ioe.toString());
            return Similarity_result.aborted;
        }

    }

}
