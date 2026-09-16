// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browsers.browser_core.in3D;

import klikr.path_lists.Path_list_provider_for_file_system;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.Kontext;
import klikr.util.P2S;
import klikr.util.perf.Perf;

import java.nio.file.Path;
import java.util.*;

//*******************************************************
public class Image_source_from_files implements Image_source
//*******************************************************
{
    private final int small_icon_size;
    private final int large_icon_size;
    private final List<Path> paths = new ArrayList<>();
    private final Map<String,Image_and_path> cache = new HashMap<>();
    private final Path_list_provider_for_file_system path_list_provider;
    private final Kontext context;
    //*******************************************************
    public Image_source_from_files(Path folder, int small_icon_size, int large_icon_size, Kontext context)
    //*******************************************************
    {
        this.small_icon_size = small_icon_size;
        this.large_icon_size = large_icon_size;
        this.context = context;

        path_list_provider = new Path_list_provider_for_file_system(folder,context);

        try ( Perf p = new Perf("Image_source_from_files: sorting"))
        {
            List<Path> folders = path_list_provider.only_folder_paths(true,Feature_cache.get(Feature.Show_hidden_folders),context.aborter());
            Collections.sort(folders);
            paths.addAll(folders);

            List<Path> files = path_list_provider.only_file_paths(true,Feature_cache.get(Feature.Show_hidden_files),context.aborter());
            Collections.sort(files);
            paths.addAll(files);
        }
    }


    //*******************************************************
    @Override
    public Image_and_path get(int i)
    //*******************************************************
    {
        if ( i < 0 ) return null;
        if ( i >= paths.size() ) return null;
        Path p = paths.get(i);
        String key = P2S.p2s(p);
        Image_and_path returned = cache.get(key);
        if ( returned != null) return returned;
        returned = new Image_and_path(p,small_icon_size,large_icon_size,context);
        cache.put(key,returned);
        return  returned;
    }


    //*******************************************************
    @Override
    public int how_many_items()
    //*******************************************************
    {
        return paths.size();
    }


}
