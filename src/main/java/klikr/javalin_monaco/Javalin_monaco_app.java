package klikr.javalin_monaco;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import klikr.util.log.Logger;
import klikr.util.log.Simple_logger;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A JavaFX application that integrates with Monaco Editor running in a local browser.
 */
public class Javalin_monaco_app extends Application
{

    private TextArea the_TextArea;


    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        Logger logger = new Simple_logger();
        Javalin_monaco javalin_monaco = new Javalin_monaco("Editor",this, logger);

        // 1. Setup JavaFX UI
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        Label infoLabel = new Label(
            "Edit text in the browser"
        );
        the_TextArea = new TextArea("sample text");
        the_TextArea.setWrapText(true);



        Button open_button = new Button("Open Monaco Editor in Browser");
        open_button.setOnAction(e -> javalin_monaco.open_browser());

        root.getChildren().addAll(infoLabel, open_button, the_TextArea);

        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("JavaFX <-> Monaco Bridge");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> javalin_monaco.stop_javalin_server());
        primaryStage.show();

        Supplier<String> text_source = ()->
        {
            String[] ss = new String[1];
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() ->
            {
                ss[0] = the_TextArea.getText();
                latch.countDown();
            });
            try
            {
                latch.await();
                return ss[0];
            }
            catch (InterruptedException e) {
                System.out.println(e);
            }
            return "";
        };

        Consumer<String> text_sink = (String s) ->
        {
            Platform.runLater(()->the_TextArea.setText(s));
        };

        javalin_monaco.start_javalin_server(text_source, text_sink);
    }





}
