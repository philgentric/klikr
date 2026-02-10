// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.search;

import klikr.path_lists.Path_list_provider;

import java.nio.file.Path;
import java.util.List;

public record Search_config(Path_list_provider path_list_provider,
                            List<String> keywords,
                            boolean look_only_for_images,
                            String extension,
                            boolean search_folders,
                            boolean search_files,
                            boolean ignore_hidden,
                            boolean check_case) {}
