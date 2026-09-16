// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.browsers.browser_core.locator;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import klikr.backup.Backup_service;
import klikr.util.Kontext;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.ui.Jfx_batch_injector;

import java.nio.file.Path;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

//**********************************************************
public class Monitor
//**********************************************************
{

    private LinkedBlockingQueue<String> input_queue = new LinkedBlockingQueue<>();
    private final Path top;
    private final Folders_with_large_images_locator locator;
    private final Kontext context;
    private TextArea textArea;
    Stage stage;

    //**********************************************************
    public Monitor(Path top, Folders_with_large_images_locator locator, Kontext context)
    //**********************************************************
    {
        this.top = top;
        this.locator = locator;
        this.context = context;
        start_monitoring(context);
    }

    //**********************************************************
    private void start_monitoring(Kontext context)
    //**********************************************************
    {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for(;;)
                {
                    if ( context.aborter() != null)
                    {
                        if ( context.should_abort())
                        {
                            context.log("Folders_with_large_images_locator aborted");
                            return;
                        }
                    }
                    try {
                        String x = input_queue.poll(10, TimeUnit.MINUTES);
                        Jfx_batch_injector.inject(()->textArea.setText(textArea.getText()+"\n"+x),context);
                    } catch (InterruptedException e) {
                        context.log_exception("",e);
                        return;
                    }


                }

            }
        };
        Actor_engine.execute(r,"Folder with large images: monitor finder", context.logger());
    }

    //**********************************************************
    public void show(String msg)
    //**********************************************************
    {
        input_queue.add(msg);
    }

    //**********************************************************
    public void realize()
    //**********************************************************
    {
        stage = new Stage();

        stage.setTitle("Looking for images in :"+top.toAbsolutePath());
        VBox vbox = new VBox();


        Button cancel = new Button("cancel");
        {
            cancel.setOnAction(actionEvent -> {
                context.log("Folders_with_large_images_locator CANCEL!");
                locator.cancel();
                stage.close();
            });
        }
        vbox.getChildren().add(cancel);

        textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setPrefColumnCount(80);
        textArea.setPrefRowCount(80);
        vbox.getChildren().add(textArea);

        Scene scene = new Scene(vbox);
        stage.setScene(scene);
        stage.show();
    }

    public void close() {
        Jfx_batch_injector.inject(()->stage.close(),context);
    }
}
