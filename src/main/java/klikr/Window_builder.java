// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Window;
import klikr.audio.Song_playlist_browser;
import klikr.browser.classic.Browser;
import klikr.browser.comparators.Last_access_comparator;
import klikr.browser.virtual_landscape.Scroll_position_cache;
import klikr.browser.virtual_landscape.Shutdown_target;
import klikr.diskview.Disk_footprint;
import klikr.experimental.image_playlist.Image_playlist_browser;
import klikr.in3D.Circle_3D;
import klikr.path_lists.Path_list_provider;
import klikr.path_lists.Path_list_provider_for_playlist;
import klikr.util.log.Logger;

import java.nio.file.Path;
import java.util.Optional;

//**********************************************************
public class Window_builder
//**********************************************************
{
    private static final boolean dbg = false;
    public final Window_type window_type;
    public final Path_list_provider path_list_provider;
    public final Rectangle2D rectangle;
    public final Shutdown_target shutdown_target; // if null, there is no previous guy to shutdown
    public final Window owner;
    public final Application application;

    //**********************************************************
    private Window_builder(
            Application application,
            Window_type window_type,
            Path_list_provider path_list_provider,
            Rectangle2D rectangle,
            Shutdown_target shutdown_target,
            Window owner,
            Logger logger)
    //**********************************************************
    {
        this.application = application;
        this.window_type = window_type;
        this.rectangle = rectangle;
        this.shutdown_target = shutdown_target;
        this.owner = owner;
        this.path_list_provider = path_list_provider;
    }

    //**********************************************************
    private String to_string()
    //**********************************************************
    {
        String returned = "shutdown_target="+shutdown_target;
        return returned;
    }


    //**********************************************************
    public static Window_provider additional_no_past(Application application,Window_type window_type, Path_list_provider path_list_provider, Window owner, Logger logger)
    //**********************************************************
    {
        Optional<Path> op = path_list_provider.get_folder_path();
        op.ifPresent(path -> Last_access_comparator.set_last_access(path, logger));
        Window_builder window_builder = new Window_builder(
                application,
                window_type,
                path_list_provider,
                null,
                null,
                owner,
                logger);
        if ( dbg) logger.log(("\nadditional_no_past\n"+ window_builder.to_string() ));
        return get_one_new(window_builder,logger);
    }

    //**********************************************************
    public static void additional_same_folder(
            Application application,
            Window_type window_type,
            Path_list_provider path_list_provider,
            Optional<Path> top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        // make sure the new window is scrolled at the same position
        top_left.ifPresent((top_left_item)->  Scroll_position_cache.scroll_position_cache_write(path_list_provider.get_key(),top_left_item.toAbsolutePath().normalize().toString()));

        Rectangle2D rectangle = new Rectangle2D(originator.getX()+100,originator.getY()+100,originator.getWidth()-100,originator.getHeight()-100);

        Window_builder window_builder =  new Window_builder(
                application,
                window_type,
                path_list_provider,
                rectangle,
                null,
                originator,
                logger);
        if ( dbg) logger.log(("\nadditional_same_folder\n"+ window_builder.to_string() ));
        get_one_new(window_builder,logger);
    }


    //**********************************************************
    public static void additional_same_folder_fat_tall(
            Application application,
            Window_type window_type,
            Path_list_provider path_list_provider,
            Optional<Path> top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        additional_same_folder_ratio(application,window_type,path_list_provider,5,top_left,originator ,logger);

    }
    //**********************************************************
    public static void additional_same_folder_twin(
            Application application,
            Window_type window_type,
            Path_list_provider path_list_provider,
            Optional<Path> top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        additional_same_folder_ratio(application,window_type,path_list_provider,2,top_left,originator,logger);
    }
    //**********************************************************
    public static void additional_same_folder_ratio(
            Application application,
            Window_type window_type,
            Path_list_provider path_list_provider,
            int ratio,
            Optional<Path> top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        top_left.ifPresent((top_left_item)->  Scroll_position_cache.scroll_position_cache_write(path_list_provider.get_key(),top_left_item.toAbsolutePath().normalize().toString()));

        ObservableList<Screen> intersecting_screens = Screen.getScreensForRectangle(originator.getX(), originator.getY(), originator.getWidth(), originator.getHeight());

        Screen s = intersecting_screens.get(0);
        logger.log("    getBounds" + s.getBounds());
        Rectangle2D rectangle = s.getBounds();
        originator.setX(rectangle.getMinX());
        originator.setY(rectangle.getMinY());
        double h = s.getBounds().getHeight();

        // adjust existing window to "fat"
        double ratio_fat = ((double) ratio - 1.0)/ (double) ratio;
        double w_fat = s.getBounds().getWidth() * ratio_fat;
        originator.setWidth(w_fat);
        originator.setHeight(h);

        // create new "tall" window
        double ratio_tall = 1.0 / (double) ratio;
        double w2 = s.getBounds().getWidth() * ratio_tall;
        rectangle = new Rectangle2D(rectangle.getMinX()+w_fat, rectangle.getMinY(), w2, h);

        Window_builder window_builder = new Window_builder(
                application,
                window_type,
                path_list_provider,
                rectangle,
                null,
                originator,
                logger);
        if (dbg) logger.log(("\nadditional_same_folder\n" + window_builder.to_string()));
        get_one_new(window_builder,logger);
    }


    //**********************************************************
    public static void replace_same_folder(
            Application application,
            Shutdown_target shutdown_target,
            Window_type window_type,
            Path_list_provider path_list_provider,
            Optional<Path> top_left,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        top_left.ifPresent((top_left_item)->  Scroll_position_cache.scroll_position_cache_write(path_list_provider.get_key(),top_left_item.toAbsolutePath().normalize().toString()));

        Rectangle2D rectangle = new Rectangle2D(originator.getX(),originator.getY(),originator.getWidth(),originator.getHeight());
        Window_builder window_builder =  new Window_builder(
                application,
                window_type,
                path_list_provider,
                rectangle,
                shutdown_target,
                originator,
                logger);
        if ( dbg) logger.log(("\nreplace_same_folder\n"+ window_builder.to_string() ));
        get_one_new(window_builder,logger);
    }

    //**********************************************************
    public static void replace_different_folder(
            Application application,
            Shutdown_target shutdown_target,
            Window_type window_type, Path_list_provider path_list_provider,
            Window originator,
            Logger logger)
    //**********************************************************
    {
        Optional<Path> op = path_list_provider.get_folder_path();
        op.ifPresent((Path folder_path)->
                {
                    if ( dbg)
                        logger.log("replace_different_folder new path: " + folder_path.toAbsolutePath());
                    Last_access_comparator.set_last_access(folder_path,logger);
                });

        Rectangle2D rectangle = new Rectangle2D(originator.getX(),originator.getY(),originator.getWidth(),originator.getHeight());
        Window_builder window_builder =  new Window_builder(
                application,
                window_type,
                path_list_provider,
                rectangle,
                shutdown_target,
                originator,
                logger);
        if ( dbg)
            logger.log(("\nreplace_different_folder\n"+ window_builder.to_string() ));
        get_one_new(window_builder,logger);

    }

    //**********************************************************
    private static Window_provider get_one_new(Window_builder window_builder, Logger logger)
    //**********************************************************
    {
        switch (window_builder.window_type)
        {
            case Image_playlist_2D -> {
                Path_list_provider_for_playlist pp = (Path_list_provider_for_playlist) window_builder.path_list_provider;
                return new Image_playlist_browser(window_builder.application, pp.the_playlist_file_path,window_builder.shutdown_target,null,null,logger);
            }

            case File_system_2D -> {
                return new Browser(window_builder,logger);
            }
            case File_system_3D -> {
                return new Circle_3D(window_builder,logger);
            }
            case File_system_diskview -> {
                return new Disk_footprint(window_builder,logger);
            }
            case Song_playlist_browser -> {
                return new Song_playlist_browser(window_builder,logger);
            }

        }
        return null;
    }

}
