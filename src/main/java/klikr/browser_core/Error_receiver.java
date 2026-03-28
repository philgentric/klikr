// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES icons/Error_type.java
package klikr.browser_core;

import klikr.browser_core.icons.Error_type;

public interface Error_receiver {
    void receive_error(Error_type error);
}
