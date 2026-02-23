// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Finder_frame.java
package klikr.search;

import javafx.application.Application;
import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.browser.virtual_landscape.Path_comparator_source;
import klikr.path_lists.Path_list_provider;
import klikr.util.log.Logger;

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
            Aborter aborter,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        Finder_frame popup = new Finder_frame(
                application,
                    keywords,
                    extension,
                    search_only_images,
                    path_list_provider,
                    path_comparator_source,
                    aborter,
                    owner,
                    logger);
            popup.start_search();
    }
}
