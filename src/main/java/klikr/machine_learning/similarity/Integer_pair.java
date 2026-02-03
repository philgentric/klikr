// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.machine_learning.similarity;

import java.nio.file.Path;
import java.nio.file.Paths;

// DO NOT use the constructor !
//**********************************************************
public record Integer_pair(int i, int j)
//**********************************************************
{
    public static long size()
    {
        return 2*(Integer.SIZE)/8;
    }

    public static Integer_pair build(int i, int j)
    {
        // trying to make sure pairs are in the same order
        // i.e. pair(i,j) == pair(j,i)
        if ( i < j ) return new Integer_pair(i,j);
        else return new Integer_pair(j,i);
    }

    public String to_string_key()
    {
        return i+"\0"+j;
    }

    public static Integer_pair from_string_key(String s)
    {
        String[] parts = s.split("\0", 2); // Split into exactly 2 parts
        int i = Integer.parseInt(parts[0]);
        int j = Integer.parseInt(parts[1]);
        return Integer_pair.build(i,j);
    }

}
