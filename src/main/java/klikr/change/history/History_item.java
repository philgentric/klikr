// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.change.history;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

//**********************************************************
public class History_item
//**********************************************************
{

    public static Comparator<? super History_item> comparator_by_date = (Comparator<History_item>) (o1, o2) -> o2.time_stamp.compareTo(o1.time_stamp);

    public final String value; // typically a path
    public final LocalDateTime time_stamp;
    public final UUID uuid;
    public final boolean available;


    //**********************************************************
    public History_item(String value_, LocalDateTime time_stamp_)
    //**********************************************************
    {
        value = value_;
        time_stamp = time_stamp_;
        this.available = Files.exists(Path.of(value_));
        ;
        uuid = UUID.randomUUID();
    }

    //**********************************************************
    String get_timestamp_as_string()
    //**********************************************************
    {
        return time_stamp.toString();
    }


    //**********************************************************
    public String to_string()
    //**********************************************************
    {
        return value +" "+time_stamp.toString()+" "+uuid;
    }


}
