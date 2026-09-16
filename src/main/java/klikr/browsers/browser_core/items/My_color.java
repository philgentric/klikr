// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browsers.browser_core.items;

import javafx.scene.paint.Color;
import org.jspecify.annotations.Nullable;

// for localized_nam 'no color' color is null
public record My_color(@Nullable Color color, String localized_name){}

