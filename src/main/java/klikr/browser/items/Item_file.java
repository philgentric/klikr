// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browser.items;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import klikr.util.execute.actor.Aborter;
import klikr.browser.icons.Icon_factory_actor;
import klikr.browser.virtual_landscape.Path_comparator_source;
import klikr.path_lists.Path_list_provider;
import klikr.browser.virtual_landscape.Selection_handler;
import klikr.properties.Non_booleans_properties;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.Optional;

//**********************************************************
public abstract class Item_file extends Item
//**********************************************************
{
    protected Path path;
    public final int icon_size;
    public final Iconifiable_item_type item_type;


    //**********************************************************
    public Item_file(
            Application application,
            Scene scene,
            Selection_handler selection_handler,
            Icon_factory_actor icon_factory_actor,
            Color color,
            Path path_,
            Path_list_provider path_list_provider,
            Path_comparator_source path_comparator_source,
            Window owner,
            Aborter aborter,
            Logger logger)
    //**********************************************************
    {
        super(application,scene, selection_handler, icon_factory_actor, color, path_list_provider, path_comparator_source, owner, aborter, logger);
        if ( path_ == null )
        {
            logger.log(Stack_trace_getter.get_stack_trace("path_ == null ???"));
        }
        this.path = path_;
        Optional<Path> optional_of_item_path = get_item_path();
        if ( optional_of_item_path.isEmpty())
        {
            logger.log(Stack_trace_getter.get_stack_trace(""));
        }
        item_type = Iconifiable_item_type.determine(optional_of_item_path.get(),owner,aborter,logger);
        icon_size = Non_booleans_properties.get_icon_size(owner);
    }

    //**********************************************************
    @Override
    public Iconifiable_item_type get_item_type()
    //**********************************************************
    {
        return item_type;
    }

    //**********************************************************
    @Override
    public String get_string()
    //**********************************************************
    {
        return "is file: " + path;
    }

}
