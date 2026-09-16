// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.animated_gifs;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import klikr.browsers.browser_core.Image_and_properties;
import klikr.util.Kontext;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.execute.actor.Job_termination_reporter;
import klikr.change.Change_gang;
import klikr.look.Look_and_feel;
import klikr.look.Look_and_feel_manager;
import klikr.util.cache.Cache_folder;
import klikr.util.files_and_paths.*;
import klikr.change.old_and_new.Command;
import klikr.change.old_and_new.Old_and_new_Path;
import klikr.change.old_and_new.Status;
import klikr.util.image.Icons_from_disk;
import klikr.util.image.icon_cache.Icon_caching;
import klikr.util.log.Logger;
import klikr.util.ui.Folder_chooser;
import klikr.util.ui.Jfx_batch_injector;
import klikr.util.ui.Popups;
import klikr.util.ui.progress.Hourglass;
import klikr.util.ui.progress.Progress_window;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


//**********************************************************
public class Animated_gifs_from_video
//**********************************************************
{
    private  final Kontext context;
    private final Path video_path;
    private  ImageView the_imageview;

    public  final int Mini_console_width = 1000;
    public  final int Mini_console_height = 200;

    public  TextField tf_start;
    public  TextField tf_duration;
     double start_time_seconds;
     double duration_seconds;
    //Path temporary_gif_full_path;
     File gif_saving_dir = null;
     Path icon_cache_dir = null;
     final int[] HUNDRED = {100};

    public Animated_gifs_from_video(Path video_path, Kontext context)
    {
        this.video_path = video_path;
        this.context = context;
    }


    //**********************************************************
    public  void generate_many_gifs(int clip_length, int skip_to_next)
    //**********************************************************
    {
        Double duration_in_seconds = Ffmpeg_utils.get_media_duration(video_path,context);
        if ( duration_in_seconds == null)
        {
            context.log(Logger.error+"FATAL: ffprobe cannot find duration of "+video_path);
            return;
        }
        if ( duration_in_seconds > 3*3600)
        {
            context.log("WARNING: ffprobe reports duration that looks wrong?"+duration_in_seconds+" in hours="+duration_in_seconds/3600+ "... going to assume 30 minutes");
            duration_in_seconds = Double.valueOf(1800.0); // assume half an hour ...
        }
        String folder_name = video_path.getFileName().toString()+"_anim";
        // will create all the animated gifs in the folder the video is
        File dir = new File(video_path.getParent().toFile(),folder_name);
        if ( !dir.exists())
        {
            if (!dir.mkdir())
            {
                context.log("WARNING: creating dir failed for "+dir.getAbsolutePath());
                return;
            }
            List<Old_and_new_Path> c = new ArrayList<>();
            c.add(new Old_and_new_Path(null,dir.toPath(), Command.command_move, Status.before_command,false));
            Change_gang.report_changes(c, context.owner());
        }
        AtomicBoolean abort_reported = new AtomicBoolean(false);
        Animated_gif_generation_actor actor = new Animated_gif_generation_actor(context.logger());
        AtomicInteger in_flight = new AtomicInteger();
        Aborter local = new Aborter("generate_many_gifs_internal", context.logger());
        Kontext k = new Kontext(context.owner(), local, context.logger());
        Optional<Hourglass> hourglass = Progress_window.show_with_in_flight(
                in_flight,
                "Wait for animated gifs to be generated",
                20*60,
                k);
        for ( int start = 0 ; start < duration_in_seconds; start+=skip_to_next)
        {
           if (local.should_abort())
           {
                Jfx_batch_injector.inject(() -> Popups.popup_warning(Logger.warning+" ABORTING MASSIVE GIF GENERATION for " + video_path, "On abort request", true, context), context);
                return;
           }
           String name = make_file_name(start);
           Path destination_gif_full_path = Path.of(dir.getAbsolutePath(),name);

           Job_termination_reporter tr = (message, job) -> in_flight.decrementAndGet();
           in_flight.incrementAndGet();
           Actor_engine.run(actor,
                    new Animated_gif_generation_message(video_path,512,50,destination_gif_full_path,clip_length,start,abort_reported,k),
                    tr,
                   context.logger());
        }

    }

    //**********************************************************
    private  String make_file_name(Integer start)
    //**********************************************************
    {
        String name = video_path.getFileName().toString()+"_part_"+String.format(Ffmpeg_utils.us_locale,"%07d", start)+".gif";
        return name;
    }

    //**********************************************************
    public  void interactive(Kontext k)
    //**********************************************************
    {
        start_time_seconds = 0;
        duration_seconds =  5;
        final int[] icon_height = {256};
        final int[] fps = {50};

        Platform.runLater(() -> {
            Stage the_stage = new Stage();
            Kontext context = new Kontext(the_stage,k.aborter(),k.logger());
            Look_and_feel look_and_feel = Look_and_feel_manager.get_instance(context.logger());
            HUNDRED[0] = (int)look_and_feel.estimate_text_width("Start time in s");

            // displayed animated gifs are 'temporary' (until saved maybe)
            icon_cache_dir = Cache_folder.get_cache_dir( Cache_folder.icon_cache,context);
            the_stage.setTitle("Animated gif maker for :"+video_path.getFileName().toString());
            the_stage.setMinWidth(Mini_console_width);
            the_stage.setMinHeight(Mini_console_height);
            the_imageview = new ImageView();
            the_imageview.setPreserveRatio(true);
            the_imageview.setFitHeight(icon_height[0]);
            Double full_clip_duration_in_seconds = Ffmpeg_utils.get_media_duration(video_path, context);

            if ( full_clip_duration_in_seconds == null)
            {
                context.log(Logger.error+"FATAL: ffprobe cannot find duration of "+video_path);
                return;
            }
            make_animated_gif_in_tmp_folder(icon_height[0],fps[0]);//start_time_seconds,duration_seconds,  logger, icon_cache_dir);
            Pane vb = new VBox();
            Look_and_feel_manager.set_region_look(vb,context.logger());
            {
                HBox hb =  new HBox();
                {
                    VBox vbb = new VBox();
                    vbb.getChildren().add(new Label("GIF height (pixel)"));
                    TextField icon_height_tf = new TextField(""+icon_height[0]);
                    Look_and_feel_manager.set_TextField_look(icon_height_tf,false,context.logger());
                    vbb.getChildren().add(icon_height_tf);
                    icon_height_tf.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            icon_height[0] = Integer.valueOf(icon_height_tf.getText());
                            the_imageview.setFitHeight(icon_height[0]);
                            make_animated_gif_in_tmp_folder(icon_height[0],fps[0]);
                        }
                    });
                    vbb.getChildren().add(new Label("Frame rate (per second)"));
                    TextField fps_tf = new TextField(""+fps[0]);
                    Look_and_feel_manager.set_TextField_look(fps_tf,false,context.logger());
                    vbb.getChildren().add(fps_tf);
                    fps_tf.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            fps[0] = Integer.valueOf(fps_tf.getText());
                            //context.log("fps="+fps[0]);
                            make_animated_gif_in_tmp_folder(icon_height[0],fps[0]);
                        }
                    });
                    hb.getChildren().add(vbb);
                }
                {
                    Region spacer = new Region();
                    Look_and_feel_manager.set_region_look(spacer,context.logger());
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    hb.getChildren().add(spacer);
                }
                hb.getChildren().add(the_imageview);
                vb.getChildren().add(hb);
                {
                    Region spacer = new Region();
                    Look_and_feel_manager.set_region_look(spacer,context.logger());
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    hb.getChildren().add(spacer);
                }
            }
            Button choose_folder_button = new Button("Choose folder & save");
            Look_and_feel_manager.set_region_look(choose_folder_button,true,context.logger());
            vb.getChildren().add(choose_folder_button);
            Button save_button = new Button("Save in ...");
            Look_and_feel_manager.set_region_look(save_button,true,context.logger());
            save_button.setDisable(true);
            vb.getChildren().add(save_button);

            choose_folder_button.setOnAction(actionEvent ->
            {
                if ( gif_saving_dir == null) gif_saving_dir = video_path.getParent().toFile();//new File(System.getProperty("user.home"));
                gif_saving_dir = Folder_chooser.show_dialog_for_folder_selection("Choose folder to save animated gifs", gif_saving_dir.toPath(), context).toFile();
                if ( gif_saving_dir == null) return;
                save_button.setDisable(false);
                save_button.setText("Save in "+gif_saving_dir.getAbsolutePath());
                save_now(icon_height[0],fps[0]);
            });

            save_button.setOnAction(actionEvent -> {
                if (gif_saving_dir== null) return;
                save_now( icon_height[0],fps[0]);
            });

            {
                {
                    HBox hb = new HBox();
                    Label label = new Label("Start time: ");
                    Look_and_feel_manager.set_region_look(label,context.logger());
                    label.setPrefWidth(HUNDRED[0]);
                    label.setMinWidth(HUNDRED[0]);
                    label.setMaxWidth(HUNDRED[0]);
                    hb.getChildren().add(label);
                    tf_start = new TextField(String.valueOf(start_time_seconds));
                    tf_start.setPrefWidth(3 * HUNDRED[0]);
                    tf_start.setMinWidth(3 * HUNDRED[0]);
                    tf_start.setMaxWidth(3 * HUNDRED[0]);
                    EventHandler<ActionEvent> start_change = actionEvent -> {
                        start_time_seconds = Double.parseDouble(tf_start.getText());
                        context.log(" START  =" + start_time_seconds);
                        make_animated_gif_in_tmp_folder(icon_height[0],fps[0]);//start_time_seconds,duration_seconds, logger, icon_cache_dir);

                    };
                    tf_start.setOnAction(start_change);
                    hb.getChildren().add(tf_start);
                    {
                        Label label2 = new Label("Total time: " + full_clip_duration_in_seconds);
                        label2.setPrefWidth(HUNDRED[0]);
                        label2.setMinWidth(HUNDRED[0]);
                        label2.setMaxWidth(HUNDRED[0]);
                        hb.getChildren().add(label2);
                    }
                    vb.getChildren().add(hb);
                    Button jump =  new Button("Jump to next (add current duration)");
                    Look_and_feel_manager.set_region_look(jump,true,context.logger());

                    hb.getChildren().add(jump);
                    jump.setOnAction(actionEvent -> {
                        change_start_time(start_time_seconds+duration_seconds);
                        make_animated_gif_in_tmp_folder( icon_height[0],fps[0]);
                    });
                }




                {
                    HBox hb = new HBox();
                    double[] values ={0.1,0.5,1,5,10,30,60,180};
                    for ( double val : values) add_change_start_time_button( val,  hb,icon_height,fps);
                    vb.getChildren().add(hb);
                }

                {
                    HBox hb = new HBox();
                    double[] values ={-0.1,-0.5,-1,-5,-10,-30,-60,-180};
                    for ( double val : values) add_change_start_time_button( val, hb,icon_height,fps);
                    vb.getChildren().add(hb);
                }
            }

            {
                HBox hb_dur = new HBox();
                Label label = new Label("Duration");
                Look_and_feel_manager.set_region_look(label,context.logger());
                label.setPrefWidth(HUNDRED[0]);
                label.setMinWidth(HUNDRED[0]);
                label.setMaxWidth(HUNDRED[0]);
                hb_dur.getChildren().add(label);
                tf_duration = new TextField(String.valueOf(duration_seconds));

                EventHandler<ActionEvent> duration_change = actionEvent -> {
                    duration_seconds = Double.parseDouble(tf_duration.getText());
                    context.log(" DURATION  ="+duration_seconds);
                    make_animated_gif_in_tmp_folder( icon_height[0],fps[0]);//start_time_seconds,duration_seconds, logger, icon_cache_dir);

                };
                tf_duration.setOnAction(duration_change);
                tf_duration.setPrefWidth(3*HUNDRED[0]);
                tf_duration.setMinWidth(3*HUNDRED[0]);
                tf_duration.setMaxWidth(3*HUNDRED[0]);
                hb_dur.getChildren().add(tf_duration);
                vb.getChildren().add(hb_dur);

                {
                    HBox hb = new HBox();
                    double[] values ={0.1,0.5,1,5,10,30,60,180};
                    for ( double val : values) add_change_duration_button( val,hb, icon_height,fps);
                    vb.getChildren().add(hb);
                }

                {
                    HBox hb = new HBox();
                    double[] values ={-0.1,-0.5,-1,-5,-10,-30,-60,-180};
                    for ( double val : values) add_change_duration_button( val, hb, icon_height,fps);
                    vb.getChildren().add(hb);
                }
            }

            Scene the_scene = new Scene(vb);
            Look_and_feel_manager.set_scene_look(the_scene,context.logger());
            the_stage.setScene(the_scene);
            the_stage.setX(0);
            the_stage.setY(0);
            the_stage.sizeToScene();
            the_stage.show();
        });

    }

    //**********************************************************
    private  void save_now(int icon_height, int fps)
    //**********************************************************
    {
        // if the user already saved, the file has been moved to the target folder
        // so we need to re-generate (use case is: user saved, changed her mind, erased the result, wants to redo it)
        Path temporary_gif_full_path = make_animated_gif_in_tmp_folder( icon_height, fps);

        String new_name = Extensions.add(video_path.getFileName().toString()+"_"+ icon_height +"_"+ start_time_seconds +"_"+ duration_seconds,Icon_caching.gif_extension);
        ;//temporary_gif_full_path.getFileName().toString();
        //if (new_name.length() > 24) new_name = new_name.substring(new_name.length() - 12);
        Path new_path = Path.of(gif_saving_dir.getAbsolutePath(), new_name);
        Old_and_new_Path oandnp = new Old_and_new_Path(temporary_gif_full_path, new_path, Command.command_move, Status.before_command,false);
        List<Old_and_new_Path> ll = new ArrayList<>();
        ll.add(oandnp);
        context.log("moving saved animated gif from tmp:"+oandnp.old_Path+"=>"+oandnp.new_Path);
        Moving_files.perform_safe_moves_in_a_thread(ll,false, context);
    }

    //**********************************************************
    private  void change_start_time(double new_val)
    //**********************************************************
    {
        start_time_seconds = new_val;
        tf_start.setText(String.valueOf(start_time_seconds));
        context.log(" START  =" + start_time_seconds);
    }

    //**********************************************************
    private  void change_duration(double new_val)
    //**********************************************************
    {
        duration_seconds = new_val;
        tf_duration.setText(String.valueOf(duration_seconds));
        context.log(" DURATION  =" + duration_seconds);
    }



    //**********************************************************
    private  void add_change_start_time_button(double amount, HBox hb, int height[], int fps[])
    //**********************************************************
    {
        String d = amount+" s";
        if( amount > 0) d = " + "+d;
        Button button = new Button(d);
        Look_and_feel_manager.set_region_look(button,true,context.logger());
        button.setPrefWidth(HUNDRED[0]);
        button.setMinWidth(HUNDRED[0]);
        button.setMaxWidth(HUNDRED[0]);
        EventHandler<ActionEvent> plus_action = actionEvent -> {
            change_start_time(start_time_seconds+amount);
            make_animated_gif_in_tmp_folder( height[0], fps[0]);//start_time_seconds, duration_seconds, path, logger, icon_cache_dir);
        };
        button.setOnAction(plus_action);
        hb.getChildren().add(button);
    }
    //**********************************************************
    private  void add_change_duration_button(double amount, HBox hb, int height[], int fps[])
    //**********************************************************
    {
        String d = amount+" s";
        if( amount > 0) d = " + "+d;
        Button button = new Button(d);
        Look_and_feel_manager.set_region_look(button,true,context.logger());
        button.setPrefWidth(HUNDRED[0]);
        button.setMinWidth(HUNDRED[0]);
        button.setMaxWidth(HUNDRED[0]);
        EventHandler<ActionEvent> plus_action = actionEvent -> {
            change_duration(duration_seconds+amount);
            make_animated_gif_in_tmp_folder( height[0],fps[0]);//start_time_seconds, duration_seconds, path, logger, icon_cache_dir);
        };
        button.setOnAction(plus_action);
        hb.getChildren().add(button);
    }

    //**********************************************************
    private  Path make_animated_gif_in_tmp_folder(int height, int fps)
    //**********************************************************
    {
        context.log("make_animated_gif_in_tmp_folder, video path=" + video_path);

        String tag = height +"_"+ start_time_seconds +"_"+ duration_seconds;
        Path temporary_gif_full_path = Icon_caching.path_for_icon_caching(video_path, tag, Icon_caching.gif_extension, context);
        if (temporary_gif_full_path == null) return null;
        context.log("make_animated_gif_in_tmp_folder, icon_file=" + temporary_gif_full_path.toAbsolutePath());

        Ffmpeg_utils.video_to_gif(
            video_path,
            height,
            fps,
            temporary_gif_full_path,
            duration_seconds,
            start_time_seconds,
            0,
            context);


        Image_and_properties iap = Icons_from_disk.load_icon_from_disk_cache(video_path, height, tag,Icon_caching.gif_extension, Icons_from_disk.dbg, context);
        //Image image = Icons_from_disk.get_image_from_cache(video_path, height, owner,logger);

        if ( iap == null)
        {
            context.log(Logger.error+"FATAL: load_icon_from_disk_cache==null");
            return null;
        }
        the_imageview.setImage(iap.image());
        return temporary_gif_full_path;
    }


}
