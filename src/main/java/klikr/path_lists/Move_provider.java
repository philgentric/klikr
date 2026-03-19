// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.path_lists;

import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public interface Move_provider {
    void move(
            Path destination, // a folder or a playlist-file
            boolean destination_is_trash,
            List<File> the_list,
            Window owner, double x, double y, Aborter aborter, Logger logger);
}
