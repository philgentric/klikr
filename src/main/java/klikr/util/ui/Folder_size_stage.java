// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util.ui;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import klikr.util.Kontext;
import klikr.util.execute.actor.Aborter;
import klikr.util.execute.actor.Actor_engine;
import klikr.look.Font_size;
import klikr.look.Look_and_feel_manager;
import klikr.look.my_i18n.My_I18n;
import klikr.util.files_and_paths.Sizes;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import klikr.util.log.Logger;
import klikr.util.ui.progress.Progress;

import java.nio.file.Path;

//**********************************************************
public class Folder_size_stage
//**********************************************************
{

    private static final double icon_height = 100;
     private static final double size_stage_height = 3*icon_height;
    private static final double size_stage_width = 2*size_stage_height;
/*    private static double stage_x = stage_x_start;
    private static double stage_y = stage_y_start;
    private static final double stage_x_start = 10;
    private static final double stage_y_start = 10;
*/


    //**********************************************************
    public static void get_folder_size(Path path, Kontext k)
    //**********************************************************
    {
        // open a window to display what is going on and the final result
        Stage local_stage = new Stage();
        Aborter local_aborter = new Aborter("get_folder_size",k.logger());
        Kontext context = new Kontext(local_stage,local_aborter,k.logger());
        local_stage.initOwner(k.owner());
        local_stage.setX(context.getX()+100);
        local_stage.setY(context.getY()+100);
        local_stage.setHeight(size_stage_height);
        local_stage.setWidth(size_stage_width);
        VBox vbox = new VBox();
        Look_and_feel_manager.set_region_look(vbox,context.logger());
        vbox.setAlignment(javafx.geometry.Pos.CENTER);

        Progress progress = Progress.start(vbox,context);

        TextArea textarea2 = new TextArea();
        vbox.getChildren().add(textarea2);
        textarea2.setMinHeight(icon_height);

        Font_size.apply_this_font_size_to_Node(textarea2,20,context.logger());

        Scene scene = new Scene(vbox, Color.WHITE);

        local_stage.setTitle(path.toAbsolutePath().toString());
        local_stage.setScene(scene);
        local_stage.show();
        //local_stage.setAlwaysOnTop(true);


        local_stage.setOnCloseRequest(new EventHandler<WindowEvent>()
        {
            @Override
            public void handle(WindowEvent windowEvent) {
                local_aborter.abort("folder length window closing");
            }
        });

        local_stage.addEventHandler(KeyEvent.KEY_PRESSED,
                key_event -> {
                    if (key_event.getCode() == KeyCode.ESCAPE) {
                        local_stage.close();
                        local_aborter.abort("folder length window closing2");
                        key_event.consume();
                    }
                });

        Runnable r = () -> {
            // this call is blocking until tree has been explored
            Sizes sizes = Static_files_and_paths_utilities.get_sizes_on_disk_deep_concurrent(path,context);
            String bytes = Static_files_and_paths_utilities.get_1_line_string_for_byte_data_size(sizes.bytes(),context);

            Jfx_batch_injector.inject(() -> {

                progress.remove();
                if (sizes.bytes() < 0)
                {
                    textarea2.setText(path+ "\nAn error occurred, probably Access Denied, check the logs");
                }

                String folders_s = My_I18n.get_I18n_string("Folders", context);
                String files_s = My_I18n.get_I18n_string("Files", context);
                String image_s = My_I18n.get_I18n_string("Images", context);
                String bytes_s = My_I18n.get_I18n_string("Bytes", context);

                textarea2.setText(folders_s+":\t\t\t"+ sizes.folders() + "\n"+
                        files_s+":\t\t\t" + sizes.files() + "\n" +
                        image_s+":\t\t\t"+sizes.images()+ "\n" +
                        bytes_s+":\t\t\t"+bytes);
                k.log(path + " :  " + sizes.folders() + " " + folders_s + " , " + sizes.files() + " " + files_s + " , " + sizes.images() + " " + image_s+" , "+bytes+" "+bytes_s);

            },context);
        };
        Actor_engine.execute(r, "Explore tree", k.logger());

    }

}
