// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser.items;

import java.nio.file.Path;
import java.util.Optional;

public interface Top_left_provider {
    Optional<Path> get_top_left();
}
