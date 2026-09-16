// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browsers.browser_core;

import javafx.scene.image.Image;
import klikr.browsers.browser_core.icons.image_properties_cache.Image_properties;
import klikr.browsers.browser_core.icons.image_properties_cache.Rotation;
import klikr.look.Jar_utils;
import klikr.util.log.Logger;

//**********************************************************
public record Image_and_properties(Image image, Image_properties properties)
//**********************************************************
{
    //**********************************************************
    public static Image_and_properties build(Image image, boolean is_broken_icon)
    //**********************************************************
    {
        return new Image_and_properties(image,new Image_properties(image.getWidth(),image.getHeight(), Rotation.normal,is_broken_icon));
    }

    //**********************************************************
    public static Image_and_properties broken(Logger logger)
    //**********************************************************
    {
        Image b = Jar_utils.get_broken_icon(300,logger);
        if ( b == null) return null;
        return Image_and_properties.build(b,true);
    }
}