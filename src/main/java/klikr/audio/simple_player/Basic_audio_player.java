// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.audio.simple_player;
//SOURCES ./Playlist.java

//SOURCES ./Song.java

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.media.EqualizerBand;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import klikr.audio.old_player.UI_instance_holder;
import klikr.change.Change_gang;
import klikr.look.Look_and_feel_manager;
import klikr.look.my_i18n.My_I18n;
import klikr.properties.File_storage;
import klikr.properties.Non_booleans_properties;
import klikr.util.Shared_services;
import klikr.util.execute.actor.Aborter;
import klikr.util.log.Logger;

import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Basic_audio_player implements Media_callbacks
// **********************************************************
{
    static Basic_audio_player instance;

    private static final boolean dbg = false;
    private static final boolean ultra_dbg = false;
    static final boolean keyword_dbg = false;


    static final String AUDIO_PLAYER_EQUALIZER_BAND_ = "AUDIO_PLAYER_EQUALIZER_BAND_";
    static final String AUDIO_PLAYER_VOLUME = "AUDIO_PLAYER_VOLUME";

    public static final int WIDTH = 800;
    public static final String AUDIO_PLAYER = "AUDIO_PLAYER";
    private static final String PAUSE = "Pause";
    private static final String PLAY = "Play";
    Stage stage;
    VBox the_equalizer_vbox = new VBox();
    HBox the_equalizer_hbox = new HBox();
    HBox the_sound_control_hbox = new HBox();
    Button play_pause_button;
    Slider balance_slider;
    Slider the_timeline_slider;
    ImageView speaker_image_view;

    Label now_value_label;
    Label duration_value_label;
    Label the_status_label;
    Button previous;
    Button next;
    CheckBox auto_next_cb;
    private volatile boolean auto_next = true;

    Logger logger;
    Aborter aborter;
    private ObservableList<EqualizerBand> equalizer_bands;

    double volume = 0.5;
    double balance = 0.0;

    String pause_string;
    String play_string;
    Navigator navigator; // may be null !

    // **********************************************************
    public static Basic_audio_player get(Navigator navigator, Aborter aborter, Logger logger)
    // **********************************************************
    {
        if (instance == null)
        {
            synchronized (Change_gang.class)
            {
                if (instance == null)
                {
                    instance = new Basic_audio_player(navigator, aborter, logger);
                    instance.define_ui();
                }
            }
        }
        return instance;
    }

    // **********************************************************
    private Basic_audio_player(Navigator navigator, Aborter aborter, Logger logger)
    // **********************************************************
    {
        this.navigator = navigator;
        this.aborter = aborter;
        this.logger = logger;
        stage = new Stage();

        pause_string = My_I18n.get_I18n_string(PAUSE, stage, logger);
        play_string = My_I18n.get_I18n_string(PLAY, stage, logger);
        // logger.log("play_string = " + play_string);

        Rectangle2D r = Non_booleans_properties.get_window_bounds(AUDIO_PLAYER, stage);
        stage.setX(r.getMinX());
        stage.setY(r.getMinY());
        stage.setWidth(r.getWidth());
        stage.setHeight(r.getHeight());
        ChangeListener<Number> change_listener = (observableValue, number, t1) -> {
            // if ( dbg) logger.log("ChangeListener: image window position and/or length
            // changed");
            Non_booleans_properties.save_window_bounds(stage, AUDIO_PLAYER, logger);
        };
        stage.xProperty().addListener(change_listener);
        stage.yProperty().addListener(change_listener);
        stage.widthProperty().addListener(change_listener);
        stage.heightProperty().addListener(change_listener);
        stage.setMinWidth(WIDTH);
        stage.sizeToScene();

    }

    // **********************************************************
    public static void play_song(String new_song, boolean and_seek)
    // **********************************************************
    {
        if (instance == null)
        {
            System.out.println("Error: Basic_audio_player instance is null");
            return;
        }
        instance.play_song_internal(new_song,and_seek);
    }

    // **********************************************************
    private void play_song_internal(String new_song, boolean and_seek)
    // **********************************************************
    {
        logger.log("Basic_audio_player ->"+new_song+"<-");
        Media_instance_statics.stop();


        Media_instance_statics.play_this(new_song, this, and_seek, stage, logger);

        Platform.runLater(() ->
                {
                    play_pause_button.setText(pause_string);
                    stage.setTitle(new_song);
                });
    }


    // **********************************************************
    private void define_ui()
    // **********************************************************
    {

        the_equalizer_vbox.getChildren().clear();
        the_equalizer_hbox.getChildren().clear();
        the_sound_control_hbox.getChildren().clear();

        VBox the_top_vbox = new VBox();
        Look_and_feel_manager.set_region_look(the_top_vbox, stage, logger);

        VBox duration_vbox = define_duration_vbox();
        the_top_vbox.getChildren().add(duration_vbox);
        volume_and_balance(the_top_vbox);

        // called only on EXTERNAL close requests i.e. hitting the cross in the title
        stage.setOnCloseRequest(windowEvent -> {
            logger.log("Audio player NEW closing");
            stop_current_media();
            stage.close();
            UI_instance_holder.set_null();
        });

        BorderPane bottom_border_pane = define_bottom_pane(the_top_vbox);//, scroll_pane);

        Scene scene = new Scene(bottom_border_pane);
        stage.setScene(scene);
        // this keyboqard event handler is trapped in define_scrollpane_with_songs
        scene.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> handle_keyboard(keyEvent, logger));
        stage.show();

    }

    // **********************************************************
    private VBox define_jump_vbox()
    // **********************************************************
    {
        VBox returned = new VBox();
        Look_and_feel_manager.set_region_look(returned, stage, logger);

        previous = new Button(My_I18n.get_I18n_string("Jump_To_Previous_Song", stage, logger));
        Look_and_feel_manager.set_button_look(previous, true, stage, logger);
        previous.setOnAction((ActionEvent e) ->{
            if ( navigator!=null) navigator.previous();
        } );
        returned.getChildren().add(previous);

        next = new Button(My_I18n.get_I18n_string("Jump_To_Next_Song", stage, logger));
        Look_and_feel_manager.set_button_look(next, true, stage, logger);
        next.setOnAction((ActionEvent e) ->{
            if ( navigator!=null) navigator.next();
        } );
        returned.getChildren().add(next);

        auto_next_cb = new CheckBox("Auto next");
        auto_next_cb.setSelected(auto_next);
        Look_and_feel_manager.set_region_look(auto_next_cb, stage, logger);
        auto_next_cb.setOnAction((ActionEvent e) -> {
            auto_next = auto_next_cb.isSelected();
        });
        returned.getChildren().add(auto_next_cb);

        return returned;
    }

    // **********************************************************
    private BorderPane define_bottom_pane(Pane top_pane)
    // **********************************************************
    {
        BorderPane returned = new BorderPane();
        returned.setTop(top_pane);

        VBox the_status_bar = new VBox();

        the_status_label = new Label("Status: OK");
        the_status_label.prefWidth(1000);
        the_status_label.setWrapText(true);
        Look_and_feel_manager.set_region_look(the_status_label, stage, logger);
        the_status_bar.getChildren().add(the_status_label);
        returned.setBottom(the_status_bar);
        Look_and_feel_manager.set_region_look(returned, stage, logger);
        return returned;
    }

    // **********************************************************
    private VBox define_duration_vbox()
    // **********************************************************
    {
        VBox returned = new VBox();
        Look_and_feel_manager.set_region_look(returned, stage, logger);
        HBox h1 = define_duration_hbox();
        returned.getChildren().add(h1);

        define_timeline_slider();
        returned.getChildren().add(the_timeline_slider);

        Look_and_feel_manager.set_button_look(returned, true, stage, logger);
        return returned;
    }


    // **********************************************************
    private void volume_and_balance(VBox the_big_vbox)
    // **********************************************************
    {
        the_sound_control_hbox.getChildren().add(define_volume_and_balance_vbox());
        the_sound_control_hbox.getChildren().add(the_equalizer_vbox);
        the_sound_control_hbox.getChildren().add(define_jump_vbox());
        the_big_vbox.getChildren().add(the_sound_control_hbox);

    }



    // **********************************************************
    private VBox define_volume_and_balance_vbox()
    // **********************************************************
    {
        VBox returned = new VBox();

        VBox volume_vbox = define_volume_vbox();
        returned.getChildren().add(volume_vbox);

        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer, stage, logger);
            VBox.setVgrow(spacer, Priority.ALWAYS);
            returned.getChildren().add(spacer);
        }

        VBox balance_vbox = define_balance_vbox();
        returned.getChildren().add(balance_vbox);

        return returned;
    }

    // **********************************************************
    private VBox define_balance_vbox()
    // **********************************************************
    {
        VBox balance_vbox = new VBox();
        HBox h1 = define_balance_hbox();
        balance_vbox.getChildren().add(h1);
        Button b = define_reset_balance_button();
        balance_vbox.getChildren().add(b);

        Look_and_feel_manager.set_button_look(balance_vbox, true, stage, logger);
        return balance_vbox;

    }

    // **********************************************************
    private HBox define_balance_hbox()
    // **********************************************************
    {
        // balance hbox
        HBox balance_hbox = new HBox();
        Label label = new Label("Balance");
        balance_hbox.getChildren().add(label);

        balance_slider = new Slider(-1.0, 1.0, 0.0);
        balance_slider.setMinWidth(30);
        balance_hbox.getChildren().add(balance_slider);
        balance_slider.valueProperty().addListener((observableValue, number, t1) -> {
            balance = balance_slider.getValue();
            Media_instance_statics.set_balance(balance);
        });

        return balance_hbox;
    }

    // **********************************************************
    private Button define_reset_balance_button()
    // **********************************************************
    {
        Button reset_balance = new Button(My_I18n.get_I18n_string("Reset_Balance", stage, logger));
        Look_and_feel_manager.set_button_look(reset_balance, true, stage, logger);
        reset_balance.setOnAction((ActionEvent e) -> {
            balance_slider.setValue(0);
            Media_instance_statics.set_balance(0);
        });
        return reset_balance;
    }

    // **********************************************************
    private VBox define_volume_vbox()
    // **********************************************************
    {
        VBox returned = new VBox();
        HBox hbox = define_volume_hbox();
        returned.getChildren().add(hbox);

        String mute_string = My_I18n.get_I18n_string("Mute", stage, logger);
        Button mute = new Button(mute_string);
        Look_and_feel_manager.set_button_look(mute, true, stage, logger);
        mute.setOnAction((ActionEvent e) -> {
            if (Media_instance_statics.toggle_mute()) {
                mute.setText(mute_string);
                speaker_image_view.setImage(Look_and_feel_manager.get_speaker_on_icon(stage, logger).orElse(null));
            } else {
                mute.setText(My_I18n.get_I18n_string("Unmute", stage, logger)); // "Unmute");
                speaker_image_view.setImage(Look_and_feel_manager.get_speaker_off_icon(stage, logger).orElse(null));
            }
        });
        returned.getChildren().add(mute);
        Look_and_feel_manager.set_button_look(returned, true, stage, logger);

        return returned;
    }

    // **********************************************************
    private HBox define_volume_hbox()
    // **********************************************************
    {
        HBox volume_hbox = new HBox();

        speaker_image_view = new ImageView(Look_and_feel_manager.get_speaker_on_icon(stage, logger).orElse(null));
        speaker_image_view.setFitHeight(60);
        speaker_image_view.setFitWidth(60);
        volume_hbox.getChildren().add(speaker_image_view);

        volume = get_audio_volume(stage, logger);
        Slider volume_slider = new Slider(0, 1, volume);
        volume_slider.setMinWidth(30);
        volume_hbox.getChildren().add(volume_slider);
        volume_slider.valueProperty().addListener((observableValue, number, t1) -> {
            volume = volume_slider.getValue();
            Media_instance_statics.set_volume(volume);
            save_audio_volume(volume, stage);
            if (volume >= 0.01) {
                speaker_image_view.setImage(Look_and_feel_manager.get_speaker_on_icon(stage, logger).orElse(null));
            } else {
                speaker_image_view.setImage(Look_and_feel_manager.get_speaker_off_icon(stage, logger).orElse(null));
            }
        });
        return volume_hbox;
    }


    // **********************************************************
    private void define_timeline_slider()
    // **********************************************************
    {
        the_timeline_slider = new Slider();
        // image_viewerLook_and_feel_manager.set_region_look(the_timeline_slider);
        the_timeline_slider.setMinWidth(WIDTH);
        the_timeline_slider.setPrefWidth(WIDTH);
        // but the user may click/slide the slider
        the_timeline_slider.setOnMouseReleased(event -> {
            the_timeline_slider.setValueChanging(true);
            double v = event.getX() / the_timeline_slider.getWidth() * the_timeline_slider.getMax();
            the_timeline_slider.setValue(v);
            // logger.log("player seeking: slider new value = "+slider.getValue());
            Duration target = Duration.seconds(v);
            // logger.log("player seeking:"+target);
            Media_instance_statics.seek(target);
            the_timeline_slider.setValueChanging(false);
        });
    }

    // **********************************************************
    private HBox define_duration_hbox()
    // **********************************************************
    {
        HBox hbox = new HBox();
        Look_and_feel_manager.set_region_look(hbox, stage, logger);
        Label duration_text = new Label(My_I18n.get_I18n_string("Duration", stage, logger) + " : ");
        Look_and_feel_manager.set_region_look(duration_text, stage, logger);
        hbox.getChildren().add(duration_text);
        duration_value_label = new Label("0.0s");
        Look_and_feel_manager.set_region_look(duration_value_label, stage, logger);
        hbox.getChildren().add(duration_value_label);

        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer, stage, logger);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().add(spacer);
        }

        Button rewind = new Button(My_I18n.get_I18n_string("Rewind", stage, logger));
        Look_and_feel_manager.set_button_look(rewind, true, stage, logger);
        rewind.setOnAction((ActionEvent e) -> {
            rewind();
        });
        hbox.getChildren().add(rewind);

        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer, stage, logger);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().add(spacer);
        }

        play_pause_button = new Button(pause_string);
        Look_and_feel_manager.set_button_look(play_pause_button, true, stage, logger);
        play_pause_button.setOnAction((ActionEvent e) -> toggle_play_stop());
        hbox.getChildren().add(play_pause_button);

        {
            Region spacer = new Region();
            Look_and_feel_manager.set_region_look(spacer, stage, logger);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hbox.getChildren().add(spacer);
        }

        Label now_text = new Label(My_I18n.get_I18n_string("Now", stage, logger) + " : ");
        Look_and_feel_manager.set_region_look(now_text, stage, logger);
        hbox.getChildren().add(now_text);
        now_value_label = new Label("0.0s");
        Look_and_feel_manager.set_region_look(now_value_label, stage, logger);

        hbox.getChildren().add(now_value_label);
        return hbox;
    }

    // **********************************************************
    @Override // Media_callbacks
    public void on_end_of_media()
    // **********************************************************
    {
        save_current_time_in_song(0, null);
        navigator.next();
    }

    // **********************************************************
    @Override // Media_callbacks
    public void on_player_ready()
    // **********************************************************
    {
        //logger.log("Audio_player_FX_UI on_player_ready()");
        equalizer_bands = Media_instance_statics.get_bands();
        define_equalizer();

        ChangeListener<Duration> xx = (observable, oldValue, newValue) -> {
            if (ultra_dbg)
                logger.log("song current time:" + newValue.toSeconds());
            double seconds = newValue.toSeconds();
            the_timeline_slider.setValue(seconds);
            now_value_label.setText((int) seconds + " seconds");
            save_current_time_in_song((int) seconds, stage);

        };

        // the player pilots how the slider moves during playback
        Media_instance_statics.add_current_time_listener(xx);

        // logger.log("song start:"+ player.getStartTime().toSeconds());
        // logger.log("song stop:"+ player.getStopTime().toSeconds());
        double seconds = Media_instance_statics.get_stop_time().toSeconds();
        the_timeline_slider.setValue(0);
        the_timeline_slider.setMax(seconds);
        Media_instance_statics.set_volume(volume);
        Media_instance_statics.set_balance(balance);
        Media_instance_statics.play();
        String s = get_nice_string_for_duration(seconds, stage, logger);
        duration_value_label.setText(s);

    }

    // **********************************************************
    public static String get_nice_string_for_duration(double seconds_in, Window owner, Logger logger)
    // **********************************************************
    {
        int d = 0;
        int h = 0;
        int m = 0;
        int seconds = (int) seconds_in;

        h = seconds / 3600;
        if (h > 24) {
            d = h / 24;
            h = h - d * 24;
            seconds = seconds - d * 24 * 3600 - h * 3600;
        }
        if (seconds > 60) {
            m = seconds / 60;
            seconds = seconds - m * 60;
        }
        String abbreviation_for_second = My_I18n.get_I18n_string("Abbreviation_For_Second", owner, logger);
        String abbreviation_for_minute = My_I18n.get_I18n_string("Abbreviation_For_Minute", owner, logger);
        String abbreviation_for_hour = My_I18n.get_I18n_string("Abbreviation_For_Hour", owner, logger);
        String abbreviation_for_day = My_I18n.get_I18n_string("Abbreviation_For_Day", owner, logger);
        String returned = seconds + abbreviation_for_second;
        if (m > 0)
            returned = m + abbreviation_for_minute + " " + returned;
        if (h > 0)
            returned = h + abbreviation_for_hour + " " + returned;
        if (d > 0)
            returned = d + abbreviation_for_day + " " + returned;
        return returned;
    }

    boolean equalizer_created = false;
    List<Slider> sliders = new ArrayList<>();
    List<ChangeListener<? super Number>> listeners = new ArrayList<>();

    // **********************************************************
    private void define_equalizer()
    // **********************************************************
    {
        if (equalizer_created)
        {
            for (int i = 0; i < sliders.size(); i++)
            {
                Slider s = sliders.get(i);
                double value = get_equalizer_value_for_band(i, stage, logger);
                s.setValue(value);
                equalizer_bands.get(i).setGain(value);

                int finalI = i;
                ChangeListener<? super Number> listener = listeners.get(i);
                s.valueProperty().removeListener((ChangeListener<? super Number>) listener);

                listener = (ov, old_val_, new_val_) -> {
                    double slider_value = new_val_.doubleValue();
                    logger.log("slider_value="+ slider_value);
                    equalizer_bands.get(finalI).setGain(slider_value);
                    save_equalizer_value_for_band(finalI, slider_value, stage);
                };
                s.valueProperty().addListener(listener);
                listeners.add(listener);
            }
        }
        else
        {

            double MIN = -24;
            double MAX = 12;
            int how_many_rectangles = equalizer_bands.size();
            for (int i = 0; i < how_many_rectangles; i++)
            {
                double value = get_equalizer_value_for_band(i, stage, logger);
                logger.log(i+" value="+ value);
                equalizer_bands.get(i).setGain(value);

                Slider s = new Slider(MIN, MAX, value);
                s.setMinHeight(100);
                s.setMinWidth(30);
                s.setOrientation(Orientation.VERTICAL);
                the_equalizer_hbox.getChildren().add(s);
                int finalI = i;
                ChangeListener<? super Number> listener = (ov, old_val_, new_val_) -> {
                    double slider = new_val_.doubleValue();
                    logger.log("slider="+ slider);
                    equalizer_bands.get(finalI).setGain(slider);
                    save_equalizer_value_for_band(finalI, slider, stage);
                };
                listeners.add(listener);
                s.valueProperty().addListener(listener);
                sliders.add(s);
            }
            equalizer_created = true;
            the_equalizer_vbox.getChildren().add(the_equalizer_hbox);
            Button reset_equalizer_button = make_reset_equalizer_button();
            the_equalizer_vbox.getChildren().add(reset_equalizer_button);
            Look_and_feel_manager.set_button_look(the_equalizer_vbox, true, stage, logger);
        }
    }

    // **********************************************************
    private Button make_reset_equalizer_button()
    // **********************************************************
    {
        Button reset_button = new Button(My_I18n.get_I18n_string("Reset_Equalizer", stage, logger));
        Look_and_feel_manager.set_button_look(reset_button, true, stage, logger);
        reset_button.setOnAction(actionEvent -> {
            for (int i = 0; i < equalizer_bands.size(); i++)
            {
                equalizer_bands.get(i).setGain(0.0);
                sliders.get(i).setValue(0.0);
                save_equalizer_value_for_band(i, 0.0, stage);
            }
        });
        return reset_button;
    }

    // **********************************************************
    public void stop_current_media()
    // **********************************************************
    {
        Media_instance_statics.stop();
        Media_instance_statics.dispose();
    }

    // **********************************************************
    public void set_title(String s)
    // **********************************************************
    {
        Runnable r = () -> stage.setTitle(s);
        Platform.runLater(r);
    }

    // **********************************************************
    void handle_keyboard(final KeyEvent key_event, Logger logger)
    // **********************************************************
    {
        if (keyword_dbg)
            logger.log("Audio_player_FX_UI KeyEvent=" + key_event);

        switch (key_event.getCode()) {
            case F7:
                if (keyword_dbg)
                    logger.log("F7");
                rewind();
                break;
            case F8:
                if (keyword_dbg)
                    logger.log("F8");
                toggle_play_stop();
                break;
            case F9:
                if (keyword_dbg)
                    logger.log("F9");
            case DOWN:
                if (keyword_dbg)
                    logger.log("DOWN");
            case SPACE:
                if (keyword_dbg)
                    logger.log("space");
            case RIGHT:
                if (keyword_dbg)
                    logger.log("right");
                navigator.next();
                break;

            case UP:
                if (keyword_dbg)
                    logger.log("UP");
            case LEFT:
                if (keyword_dbg)
                    logger.log("left");
                navigator.previous();
                break;

            default:
                if (keyword_dbg)
                    logger.log("default");
                break;

        }

        if (key_event.getCode() == KeyCode.ESCAPE) {
            key_event.consume();
            return;
        }



        key_event.consume();

    }


    // **********************************************************
    private void toggle_play_stop()
    // **********************************************************
    {
        MediaPlayer.Status status = Media_instance_statics.get_status();
        if (status == MediaPlayer.Status.PLAYING)
            set_is_paused();
        else
            set_is_playing();
    }

    // **********************************************************
    private void rewind()
    // **********************************************************
    {

        Media_instance_statics.stop();
        set_is_playing();
    }

    // **********************************************************
    private void set_is_playing()
    // **********************************************************
    {
        Media_instance_statics.play();
        play_pause_button.setText(pause_string);

    }

    // **********************************************************
    private void set_is_paused()
    // **********************************************************
    {
        Media_instance_statics.pause();
        play_pause_button.setText(play_string);
    }



    //**********************************************************
    public void die()
    //**********************************************************
    {
        stage.close();
        Media_instance_statics.dispose();
    }

    static int previous_current_time_in_song = -1;

    //**********************************************************
    public static void save_current_time_in_song(int time, Window owner)
    //**********************************************************
    {
        if (previous_current_time_in_song > 0) {
            if (previous_current_time_in_song / 10 == time / 10) return;
        }
        previous_current_time_in_song = time;
        //logger.log("save_current_time_in_song "+time);
        File_storage pm = Shared_services.main_properties();
        pm.set_and_save(Media_instance.AUDIO_PLAYER_CURRENT_TIME, "" + time);

    }



    //**********************************************************
    public static double get_equalizer_value_for_band(int i, Window owner,Logger logger)
    //**********************************************************
    {
        File_storage pm = Shared_services.main_properties();
        String s = pm.get(AUDIO_PLAYER_EQUALIZER_BAND_ + i);
        if (s == null) return 0;

        double value = 0;
        try {
            value = Double.valueOf(s);
        } catch (NumberFormatException e) {
            logger.log("WARNING: cannot parse equalizer value for band " + i + "->" + s + "<-");
        }
        return value;
    }

    //**********************************************************
    public static void save_equalizer_value_for_band(int i, double value, Window owner)
    //**********************************************************
    {
        File_storage pm = Shared_services.main_properties();
        pm.set_and_save(AUDIO_PLAYER_EQUALIZER_BAND_ + i, "" + value);
    }

    //**********************************************************
    public static void save_audio_volume(double value, Window owner)
    //**********************************************************
    {
        Non_booleans_properties.set_double(value,AUDIO_PLAYER_VOLUME,owner);
    }

    //**********************************************************
    public static double get_audio_volume(Window owner,Logger logger)
    //**********************************************************
    {
        return Non_booleans_properties.get_double(AUDIO_PLAYER_VOLUME,0.5,owner);
    }

}
