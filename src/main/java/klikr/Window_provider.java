// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr;

import javafx.stage.Window;

import java.nio.file.Path;
import java.util.Comparator;

//**********************************************************
public interface Window_provider
//**********************************************************
{
    Window get_owner();
    void replace_current_item(Path path, Path old);
    Comparator<? super Path> get_file_comparator();
}
