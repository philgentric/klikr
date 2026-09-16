// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Finder_frame.java
package klikr.search;

import javafx.application.Application;
import klikr.util.Kontext;
import klikr.browsers.browser_core.virtual_landscape.Path_comparator_source;
import klikr.path_lists.Path_list_provider;

import java.util.List;

//**********************************************************
public class Finder
//**********************************************************
{
    //**********************************************************
    public static void find(
            Application application,
            Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            List<String> keywords,
            String extension,
            boolean search_only_images,
            Kontext context)
    //**********************************************************
    {
        Finder_frame popup = new Finder_frame(
                application,
                    keywords,
                    extension,
                    search_only_images,
                    path_list_provider,
                    path_comparator_source,
                    context);
            popup.start_search();
    }
}
