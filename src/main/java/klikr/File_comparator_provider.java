// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr;

import javafx.stage.Window;

import java.nio.file.Path;
import java.util.Comparator;

//**********************************************************
public interface File_comparator_provider
//**********************************************************
{
    Comparator<? super Path> get_file_comparator();
}
