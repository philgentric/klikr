// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browsers.browser_core;
//SOURCES ./virtual_landscape/Shutdown_target.java
//SOURCES ./virtual_landscape/Full_screen_handler.java
//SOURCES ./virtual_landscape/Path_list_provider.java
//SOURCES ../Owner_provider.java
//SOURCES ./Title_target.java
//SOURCES ../UI_change_target.java
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import klikr.*;
import klikr.browsers.Browser_for_file_system_in_2D;
import klikr.browsers.Selection_manager;
import klikr.browsers.browser_core.virtual_landscape.UI_change_target;
import klikr.look.my_i18n.My_I18n;
import klikr.path_lists.File_comparator_provider;
import klikr.util.Kontext;
import klikr.util.execute.actor.Aborter;
import klikr.browsers.browser_core.virtual_landscape.*;
import klikr.change.Change_gang;
import klikr.change.Change_receiver;
import klikr.change.history.History_engine;
import klikr.look.Look_and_feel_manager;
import klikr.path_lists.Path_list_provider;
import klikr.settings.Non_booleans_properties;
import klikr.settings.boolean_features.Feature_cache;
import klikr.change.file_system_monitoring.Filesystem_item_modification_watcher;
import klikr.util.http.Klikr_communicator;

import java.nio.file.Path;
import java.util.function.Consumer;

//**********************************************************
public abstract class Abstract_browser implements
        Change_receiver,
        Shutdown_target,
        Title_target,
        Full_screen_handler,
        Owner_provider,
        Selection_manager,
        File_comparator_provider,
        UI_change_target
//**********************************************************
{
    public static final boolean dbg = false;
    public static final boolean kbd_dbg = false;

    //public static final AtomicInteger number_of_windows = new AtomicInteger(0);

    public static final String BROWSER_WINDOW = "BROWSER_WINDOW";

    protected final int ID;

    protected static final int FOLDER_MONITORING_TIMEOUT_IN_MINUTES = 600;


    protected Filesystem_item_modification_watcher filesystem_item_modification_watcher;
    protected My_Stage my_Stage;
    protected Virtual_landscape virtual_landscape;
    protected Kontext context; // init in 2 steps
    protected boolean ignore_escape_as_the_stage_is_full_screen = false;

    protected abstract String get_path_for_history();
    protected abstract String get_name();
    protected abstract Path_list_provider get_Path_list_provider();
    protected abstract String signature();
    protected abstract void monitor_current_path_list_source();
    public final Color background_color;

    //**********************************************************
    protected void init_base(Change_receiver change_receiver)
    //**********************************************************
    {
        Change_gang.register(change_receiver, context);

        // the derived class call this after super() in their constructor,
        // so that
        // 1. fields can be final
        // 2. you can use 'this' for 'change_receiver'
        // since you are after super()
    }
    //**********************************************************
    public Abstract_browser(
    Window_builder window_builder,
    String badge,
    String name,
    Color background,
    Kontext k)
    //**********************************************************
    {

        Stage stage = new Stage();
        Aborter aborter = new Aborter(name, k.logger());
        this.context = new Kontext(stage,aborter, k.logger());
        my_Stage = new My_Stage(context);
        this.background_color = background;
        ID = Window_manager.register();
        if ( dbg)
            context.log("AbstractBrowser constructor ID=" + ID);

        virtual_landscape = new Virtual_landscape(window_builder.application,window_builder.window_type,window_builder.path_list_provider, background_color,this,this,this,this,context);

        Consumer<String> on_appearance_changed = new Consumer<String>() {
            @Override
            public void accept(String s) {
                Non_booleans_properties.force_reload_from_disk();
                String new_ui_option = s.split(" ")[1];
                context.log("Klikr UI_CHANGED RECEIVED, msg is "+new_ui_option);
                My_I18n.reset();
                Look_and_feel_manager.reset();
                Platform.runLater(() -> define_UI());
            }
        };
        Klikr_communicator.instance.set_on_appearance_changed(on_appearance_changed);

        my_Stage.get_Stage().setOnCloseRequest(event -> {
            //context.log(Stack_trace_getter.get_stack_trace("never happens? AbstractBrowser setOnCloseRequest "));
            shutdown();
        });


        double x = 0;
        double y = 0;
        double width = 2400 / 2.0;
        double height = 1080 - y;
        if (window_builder.rectangle != null)
        {
            x = window_builder.rectangle.getMinX();
            context.log("Klikr Rectangle MinX=" + x);
            y = window_builder.rectangle.getMinY();
            width = window_builder.rectangle.getWidth();
            height = window_builder.rectangle.getHeight();
        }
        else
        {
            Rectangle2D r = Non_booleans_properties.get_window_bounds(BROWSER_WINDOW);
            if (r != null)
            {
                x = r.getMinX();
                y = r.getMinY();
                width = r.getWidth();
                height = r.getHeight();
            }
        }
        if (dbg)
            context.log("NEW Abstract_browser "+x+","+y);

        my_Stage.get_Stage().setX(x);
        my_Stage.get_Stage().setY(y);
        my_Stage.get_Stage().setWidth(width);
        my_Stage.get_Stage().setHeight(height);
        my_Stage.get_Stage().show();

        Look_and_feel_manager.set_icon_for_main_window(badge, Look_and_feel_manager.Icon_type.KLIK, context);

        if ( window_builder.window_type != Window_type.Search_results)
        {
            //record in history
            String path_for_history = get_path_for_history();
            if (path_for_history != null) History_engine.get(context).record(path_for_history);
        }

        set_title();


        ChangeListener<Number> change_listener = (observableValue, number, t1) -> {
            record_stage_bounds();
        };
        my_Stage.get_Stage().xProperty().addListener(change_listener);
        my_Stage.get_Stage().yProperty().addListener(change_listener);

        virtual_landscape.redraw_fx(true,"AbstractBrowser constructor", true);

        my_Stage.set_escape_event_handler(this);

        my_Stage.get_Stage().widthProperty().addListener((observable, oldValue, newValue) -> {
            if (dbg) context.log("new browser width =" + newValue.doubleValue());
            record_stage_bounds();
            virtual_landscape.redraw_fx(false,"width changed by user", false);
        });
        my_Stage.get_Stage().heightProperty().addListener((observable, oldValue, newValue) -> {
            record_stage_bounds();
            virtual_landscape.redraw_fx(false,"height changed by user", false);
        });


    }

    @Override // Owner_provider
    public Window get_owner()
    {
        return my_Stage.get_Stage();
    }

    //*******************************************************
    @Override // Selection_manager
    public void set_unique_selected_item(Path path)
    //*******************************************************
    {
        context.log("Abstract_browser set_unique_selected_item : "+path);
        virtual_landscape.set_selected_look_for(path);
    }

    //**********************************************************
    @Override // UI_change_target signaled by launcher
    public void define_UI()
    //**********************************************************
    {
        virtual_landscape.redraw_fx(true,"UI changed HTTP signal received",true);
    }

    //**********************************************************
    private void record_stage_bounds()
    //**********************************************************
    {
        if (dbg) context.log("ChangeListener: image window position and/or length changed");
        Non_booleans_properties.save_window_bounds(my_Stage.get_Stage(), BROWSER_WINDOW, context.logger());
    }


    //**********************************************************
    @Override // Shutdown_target
    public void shutdown()
    //**********************************************************
    {
        context.abort("AbstractBrowser shutdown "+get_Path_list_provider().get_key());
        if (dbg) context.log("AbstractBrowser shutdown " + signature());
        Window_manager.unregister(ID,context);

        // when we change dir, we need to de-register the old browser
        // otherwise the list in the change_gang keeps growing
        // plus memory leak! ==> the RAM footprint keeps growing
        Change_gang.deregister(this, context.aborter());
        if (filesystem_item_modification_watcher != null) filesystem_item_modification_watcher.cancel();

        Feature_cache.deregister_for_all(virtual_landscape);
        Feature_cache.string_deregister_all(virtual_landscape);

        virtual_landscape.stop_scan();
        my_Stage.close();
    }


    //**********************************************************
    @Override // Full_screen_handler
    public void go_full_screen()
    //**********************************************************
    {
        ignore_escape_as_the_stage_is_full_screen = true;
        my_Stage.get_Stage().setFullScreen(true);
    }

    //**********************************************************
    //@Override // Full_screen_handler
    public void stop_full_screen()
    //**********************************************************
    {
        // this is the menu action, on_fullscreen_end() will be called
        my_Stage.get_Stage().setFullScreen(false);
    }



}
