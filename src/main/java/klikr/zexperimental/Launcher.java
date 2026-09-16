// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.zexperimental;

//SOURCES ./audio/Audio_player_access.java
//SOURCES ./image_ml/ML_servers_util.java
//SOURCES ./image_ml/UDP_traffic_monitor.java
//SOURCES ./image_ml/UDP_traffic_monitoring_stage.java
//SOURCES ./util/execute/Execute_via_script_in_tmp_file.java

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import klikr.System_info;
import klikr.browsers.browser_core.virtual_landscape.UI_change_target;
import klikr.util.Installers;
import klikr.util.Kontext;
import klikr.util.Shared_services;
import klikr.util.execute.Script_executor;
import klikr.util.execute.actor.Aborter;
import klikr.look.Look_and_feel;
import klikr.look.Look_and_feel_manager;
import klikr.look.Look_and_feel_manager.Icon_type;
import klikr.look.my_i18n.My_I18n;
import klikr.settings.Non_booleans_properties;
import klikr.util.http.Klikr_communicator;
import klikr.util.log.Logger;
import klikr.util.ui.progress.Hourglass;
import klikr.util.ui.progress.Progress_window;

import java.util.List;
import java.util.function.Consumer;

// the launcher can start applications (the image browser klik, the audio player)
// they are started as new processes
// (using a call to gradle to start a new JVM
// or a native application if compiled with gluon
// this may mean that the application may be recompiled
// before launching, if the code has changed)
//
// the launcher passes a reply port number to the application which has 2 uses:
// 1. at start time, the application can send a message to the launcher to say it has started, or not started
// 2. at any time, the application can send a message to the launcher to say that the UI has changed
//
// to enforce the fact that there is only 1 instance of the music player, the launcher has to rely on
// trying to start a new music player instance: if there is already a music player running,
// the music player will fail when it tries to attach a server on the audio player port (port 34539),
// (which the audio player listen to for requests to play songs or playlists),
// then it sends a NOT_STARTED message to the launcher, and the launcher pops up a warning..
//
// launching a new klik instance is not supposed to fail (it may not be a good idea in the sense that
// klik can have as many windows as one wants, but launching a new instance of klik has advantages;
// for example it is immune to the agressive use of ESC by the user)
//
// UI changes are originating from one klik browser instance, they are sent to the launcher,
// which then propagates them to the audio player, on the audio player port

//**********************************************************
@Deprecated
public class Launcher extends Application implements UI_change_target
//**********************************************************
{
    // set gluon to true to compile native with gluon
    public static final boolean gluon = false;
    private final static String name = "Launcher";
    public static final int WIDTH = 600;
    public static final int icon_size = 100;
    public static final String STARTED = "STARTED";
    public static final String NOT_STARTED = "NOT_STARTED";
    public static final String LAUNCHER = "Launcher";

    private Kontext context;
    private HBox  main = new HBox();

    //private static ConcurrentLinkedQueue<Integer> propagate_to = new ConcurrentLinkedQueue<>();
    private Klikr_communicator com;
    Hourglass hourglass;
    Runnable on_started_received;

    //**********************************************************
    public static void main(String[] args)
    //**********************************************************
    {
        launch(args);
    }

    //**********************************************************
    @Override
    public void start(Stage stage_) throws Exception
    //**********************************************************
    {
        Shared_services.init(name, stage_);
        context = new Kontext(stage_,Shared_services.aborter(),Shared_services.logger());
        context.log("Launcher starting");
        System_info.print(context.logger());

        Consumer<String> on_appearance_changed = msg -> {
            define_UI();
        };
        com = new Klikr_communicator("Launcher",context);
        com.set_on_appearance_changed(on_appearance_changed);
        com.start_as_singleton();
        on_started_received = () ->
        {
            context.log("started received from newly started app");
            hourglass.close();
            com.deregister_on_started_received(on_started_received);
        };
        com.register_on_started_received(on_started_received);

        String launcher = My_I18n.get_I18n_string(LAUNCHER,    context);
        Look_and_feel_manager.set_icon_for_main_window(launcher, Icon_type.LAUNCHER,context);

        Scene scene = new Scene(main);
        define_UI();

        context.setTitle("Klik "+launcher);
        context.get_Stage().setScene(scene);
        context.get_Stage().show();
        context.get_Stage().requestFocus(); // trying to make sure it comes on top

        long current = Non_booleans_properties.get_java_VM_max_RAM(context);

        if ( current > 0.8*System_info.get_total_machine_RAM_in_GBytes(context.logger()).orElse(4) )
        {
            // not realistic
            use_default_max_RAM();
            return;

        }
        if ( current == 1 )
        {
            // stupid default
            use_default_max_RAM();
        }
        
    }

    //**********************************************************
    private void use_default_max_RAM()
    //**********************************************************
    {
        long current = System_info.get_total_machine_RAM_in_GBytes(context.logger()).orElse(4);
        current = (current * 8) / 10; // use 80% of the physical RAM
        if ( current < 1) current = 1; // minimum 1GB
        Non_booleans_properties.save_java_VM_max_RAM((int)current, context);
        context.log("Setting the max RAM to 80% of the physical RAM on this machine: "+current+" GBytes");
    }

    //**********************************************************
    @Override // UI_change_target
    public void define_UI()
    //**********************************************************
    {
        context.log("Launcher define_UI");
        Look_and_feel look_and_feel = Look_and_feel_manager.get_instance(context.logger());

        Look_and_feel_manager.set_region_look(main,context.logger());

        main.getChildren().clear();
        VBox left = new VBox();
        main.getChildren().add(left);
        VBox right = new VBox();
        main.getChildren().add(right);

        {
            {
                Button b = new Button(My_I18n.get_I18n_string("Launch_1_New_Klikr_Application", context));
                left.getChildren().add(b);
                look_and_feel.set_Button_look(b, WIDTH, icon_size, Icon_type.IMAGE, context.logger());
                b.setOnAction(event -> {
                    if (Launcher.gluon) {
                        start_app_with_gradle_and_listen("nativeRun", context);
                    } else {
                        start_app_with_gradle_and_listen("klikr", context);
                    }
                });
            }
            {
                Button b = new Button(My_I18n.get_I18n_string("Launch_Music_Player", context));
                right.getChildren().add(b);
                look_and_feel.set_Button_look(b, WIDTH, icon_size, Icon_type.MUSIC, context.logger());
                b.setOnAction(event -> {
                    start_app_with_gradle_and_listen("old_audio_player", context);
                    //propagate_to.add(Audio_player_gradle_start.AUDIO_PLAYER_PORT);
                });
            }

        }
        {
            left.getChildren().add(new Separator());
            right.getChildren().add(new Separator());
        }
        Installers.make_ui_to_start_image_similarity_servers(WIDTH,icon_size,look_and_feel, left, context);
        Installers.make_ui_to_stop_image_similarity_servers(WIDTH,icon_size,look_and_feel, left, context);
        {
            left.getChildren().add(new Separator());
        }
        Installers.make_ui_to_start_face_recognition_servers(WIDTH,icon_size,look_and_feel, left, context);
        Installers.make_ui_to_stop_face_recognition_servers(WIDTH,icon_size,look_and_feel, left, context);
        {
            left.getChildren().add(new Separator());
        }
        Installers.make_ui_to_show_version(WIDTH,icon_size,look_and_feel, left, context);
        Installers.make_ui_get_most_recent_version(WIDTH,icon_size,look_and_feel, left, context);

        Installers.make_ui_to_install_everything(true,WIDTH,icon_size,look_and_feel, right, context);
        Installers.make_ui_to_install_python_libs_for_ML(WIDTH,icon_size,look_and_feel, right, context);
        Installers.make_ui_to_install_all_apps(WIDTH,icon_size,look_and_feel, right, context);
    }


    //**********************************************************
    private void start_app_with_gradle_and_listen(
            String app_name,
            Kontext context)
    //**********************************************************
    {
        hourglass = Progress_window.show(
                "Please wait ... starting "+app_name,
                30*60,
                context).orElse(null);

        String cmd = "gradle "+app_name+ " "+com.get_port();
        Script_executor.execute(List.of(cmd),true,context);

    }


}
