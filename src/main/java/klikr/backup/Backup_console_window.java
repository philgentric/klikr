// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ../../util/Strings.java
package klikr.backup;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import klikr.util.Kontext;
import klikr.look.Look_and_feel_manager;
import klikr.util.ui.Jfx_batch_injector;
import klikr.util.Strings;


//**********************************************************
public class Backup_console_window
//**********************************************************
{
    //private static final boolean dbg = false;

    Backup_engine backup_engine;
    Backup_stats stats;
    TextField number_of_folders_processed;
    TextField number_of_files_processed;
    TextField number_of_files_skipped;
    TextField number_of_files_copied;
    TextField number_of_bytes_copied;
    TextField remaining_time;
    TextField number_of_bytes_to_be_processed;
    TextField application_bytes_per_second;
    TextField bytes_read_per_second_for_file_bit_level_compare;
    TextField last_minute_bytes_per_second;
    TextField min_last_minute_bytes_per_second;
    TextField max_last_minute_bytes_per_second;
    TextField number_of_bytes_processed;
    TextArea textArea;
    private final long start;
    private long previous_ms;
    private double previous_bytes = 0;
    private int min_speed = Integer.MAX_VALUE;
    private int max_speed = 0;
    private static final int MILLISECONDS = 10_000;

    private final Kontext local_context;

    //**********************************************************
    public Backup_console_window(Backup_engine backup_engine_, Backup_stats stats_, Kontext k)
    //**********************************************************
    {
        backup_engine = backup_engine_;
        stats = stats_;

        Stage stage = new Stage();
        local_context = new Kontext(stage,k.aborter(),k.logger());
        stage.setX(k.getX()+Per_folder_mini_console.Mini_console_max_x);
        stage.setY(k.getY());
        stage.setX(Per_folder_mini_console.Mini_console_max_x);
        stage.setY(0);
        //stage.setHeight(500);
        //stage.setWidth(500);

        stage.setTitle("Backing up");
        VBox vbox = new VBox();

        {
            Label source = new Label("SOURCE= "+ backup_engine.source.toAbsolutePath());
            Look_and_feel_manager.set_region_look(source,local_context.logger());
            vbox.getChildren().add(source);
            Label destination = new Label("DESTINATION= "+ backup_engine.destination.toAbsolutePath());
            Look_and_feel_manager.set_region_look(destination,local_context.logger());
            vbox.getChildren().add(destination);
        }

        {
            remaining_time = new TextField("0");
            Look_and_feel_manager.set_region_look(remaining_time,local_context.logger());
            //remaining_time.setPrefColumnCount(70);
            //remaining_time.setAlignment(Pos.BASELINE_LEFT);
            add_one_line(vbox, remaining_time, "Remaining time:");
        }
        Button cancel = new Button("cancel");
        {
            cancel.setOnAction(actionEvent -> {
                local_context.logger().log("backup CANCEL!");
                die();
                stage.close();
                Backup_service.abort(local_context.logger());
            });
        }
        Look_and_feel_manager.set_region_look(cancel,true,local_context.logger());
        vbox.getChildren().add(cancel);

        {
            number_of_folders_processed = new TextField("0");
            Look_and_feel_manager.set_region_look(number_of_folders_processed,local_context.logger());
            add_one_line(vbox, number_of_folders_processed, "Number of folders processed:");
        }
        {
            number_of_files_processed = new TextField("0");
            Look_and_feel_manager.set_region_look(number_of_files_processed,local_context.logger());
            add_one_line(vbox, number_of_files_processed, "Number of files processed:");
        }
        {
            number_of_files_copied = new TextField("0");
            Look_and_feel_manager.set_region_look(number_of_files_copied,local_context.logger());
            add_one_line(vbox, number_of_files_copied, "Number of files copied:");
        }
        {
            number_of_files_skipped = new TextField("0");
            Look_and_feel_manager.set_region_look(number_of_files_skipped,local_context.logger());
            add_one_line(vbox, number_of_files_skipped, "Number of files skipped:");
        }
        {
            number_of_bytes_copied = new TextField("0");
            Look_and_feel_manager.set_region_look(number_of_bytes_copied,local_context.logger());
            add_one_line(vbox, number_of_bytes_copied, "Number of bytes copied:");
        }
        {
            number_of_bytes_to_be_processed = new TextField("0");
            Look_and_feel_manager.set_region_look(number_of_bytes_to_be_processed,local_context.logger());
            add_one_line(vbox,number_of_bytes_to_be_processed,"Total number of bytes in the source");
        }
        {
            number_of_bytes_processed = new TextField("0");
            Look_and_feel_manager.set_region_look(number_of_bytes_processed,local_context.logger());
            add_one_line(vbox,number_of_bytes_processed,"Total number of bytes processed:");
        }
        {
            application_bytes_per_second = new TextField("0");
            Look_and_feel_manager.set_region_look(application_bytes_per_second,local_context.logger());
            add_one_line(vbox, application_bytes_per_second,"Speed (since start): ");
        }
        {
            bytes_read_per_second_for_file_bit_level_compare = new TextField("0");
            Look_and_feel_manager.set_region_look(bytes_read_per_second_for_file_bit_level_compare,local_context.logger());
            add_one_line(vbox, bytes_read_per_second_for_file_bit_level_compare,"READING speed since start for file bit-level compare: ");
        }
        {
            last_minute_bytes_per_second = new TextField("0");
            Look_and_feel_manager.set_region_look(last_minute_bytes_per_second,local_context.logger());
            add_one_line(vbox,last_minute_bytes_per_second,"Speed (over last "+ MILLISECONDS/1000 +" seconds): ");
        }
        {
            min_last_minute_bytes_per_second = new TextField("0");
            Look_and_feel_manager.set_region_look(min_last_minute_bytes_per_second,local_context.logger());
            add_one_line(vbox,min_last_minute_bytes_per_second,"Min speed (over "+ MILLISECONDS/1000 +" seconds): ");
        }
        {
            max_last_minute_bytes_per_second = new TextField("0");
            Look_and_feel_manager.set_region_look(max_last_minute_bytes_per_second,local_context.logger());
            add_one_line(vbox,max_last_minute_bytes_per_second,"Max speed (over "+ MILLISECONDS/1000 +" seconds): ");
        }

        textArea = new TextArea();
        Look_and_feel_manager.set_region_look(textArea,local_context.logger());
        textArea.setWrapText(true);
        textArea.setPrefColumnCount(80);
        textArea.setPrefRowCount(80);
        vbox.getChildren().add(textArea);

        Scene scene = new Scene(vbox);
        stage.setScene(scene);
        stage.show();

        start = System.currentTimeMillis();
        previous_ms = start;


    }

    //**********************************************************
    private void add_one_line(VBox vbox, TextField tf,String text)
    //**********************************************************
    {
        HBox hbox = new HBox();
        vbox.getChildren().add(hbox);

        Label label = new Label(text);
        Look_and_feel_manager.set_region_look(label,local_context.logger());

        hbox.getChildren().add(label);
        label.setAlignment(Pos.BASELINE_LEFT);
        Region spacer = new Region();
        Look_and_feel_manager.set_region_look(spacer,local_context.logger());
        HBox.setHgrow(spacer, Priority.ALWAYS);
        hbox.getChildren().add(spacer);

        hbox.getChildren().add(tf);
        tf.setAlignment(Pos.BASELINE_RIGHT);
    }


    //**********************************************************
    public void die()
    //**********************************************************
    {
        local_context.abort("Backup_console::abort()");
        ((Stage)(local_context.owner())).close();
    }

    //**********************************************************
    void update_later()
    //**********************************************************
    {
        Jfx_batch_injector.inject(this::update_,local_context);
    }


    //**********************************************************
    void update_()
    //**********************************************************
    {
        //logger.log("updating !");
        number_of_folders_processed.setText(String.valueOf(stats.done_dir_count.longValue()));
        number_of_files_processed.setText(String.valueOf(stats.files_checked.longValue()));
        number_of_files_skipped.setText(String.valueOf(stats.files_skipped.longValue()));
        number_of_files_copied.setText(String.valueOf(stats.files_copied.longValue()));
        number_of_bytes_copied.setText(Strings.create_nice_bytes_string((long) stats.bytes_copied.longValue()));
        number_of_bytes_to_be_processed.setText(Strings.create_nice_bytes_string(stats.source_byte_count));
        number_of_bytes_processed.setText(Strings.create_nice_bytes_string((long)stats.number_of_bytes_processed.longValue()));

        long now = System.currentTimeMillis();
        double delta_t = (double)(now-start)/1000.0;
        {
            double speed = (double) stats.number_of_bytes_processed.doubleValue() / delta_t;
            application_bytes_per_second.setText(Strings.create_nice_bytes_per_second_string((long) speed));
            long remaining_ms = (long)((double)(stats.source_byte_count-stats.number_of_bytes_processed.doubleValue())/speed);
            remaining_time.setText(Strings.create_nice_remaining_time_string(1000*remaining_ms));
        }
        {
            double read_speed = (double) stats.number_of_bytes_read.doubleValue() / delta_t;
            bytes_read_per_second_for_file_bit_level_compare.setText(Strings.create_nice_bytes_per_second_string((long) read_speed));
        }
        {

            long now_bytes = stats.number_of_bytes_processed.longValue();
            long recently_elapsed = now-previous_ms;
            if ( recently_elapsed > MILLISECONDS)
            {
                double recently_elapsed_d = (double) recently_elapsed/1000.0;
                int last_minute_speed = (int)((double) (now_bytes - previous_bytes) / recently_elapsed_d);
                last_minute_bytes_per_second.setText(Strings.create_nice_bytes_per_second_string(last_minute_speed));
                previous_bytes = now_bytes;
                previous_ms = now;
                if ( last_minute_speed < min_speed)
                {
                    min_speed = last_minute_speed;
                    min_last_minute_bytes_per_second.setText(Strings.create_nice_bytes_per_second_string(min_speed));

                }
                if ( last_minute_speed > max_speed)
                {
                    max_speed = last_minute_speed;
                    max_last_minute_bytes_per_second.setText(Strings.create_nice_bytes_per_second_string(max_speed));

                }
            }
        }


    }
}
