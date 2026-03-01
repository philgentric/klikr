package klikr.javalin_monaco;

import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import javafx.application.Application;
import javafx.application.Platform;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.log.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A Javalin bridge for a Monaco Editor running in a local browser.
 */
public class Javalin_monaco
{
    private final static boolean ultra_dbg = false;
    private final Application application;
    private final Logger logger;
    private Javalin javalin;
    private static final int port_number = 8080; // Or random port: findFreePort()
    private final Set<WsContext> connected_clients = Collections.newSetFromMap(
            new ConcurrentHashMap<>()
    );
    private final String page_title;

    public Javalin_monaco(String page_title, Application application, Logger logger)
    {
        this.application = application;
        this.page_title = page_title;
        this.logger = logger;
    }

    public static void show(Application application, Path path, Logger logger)
    {
        Javalin_monaco javalin_monaco = new Javalin_monaco("Text editor",application,logger);
        javalin_monaco.start(path);
    }

    private void start(Path path)
    {
        Supplier<String> text_source = ()->
        {
            try
            {
                List<String> lines = Files.readAllLines(path);
                StringBuilder builder = new StringBuilder();
                for ( String line : lines)
                {
                    builder.append(line).append("\n");
                }
                return  builder.toString();
            }
            catch (IOException e)
            {
                logger.log(""+e);
            }
            return "error cannot read: "+path;
        };

        Consumer<String> text_sink = (String s) ->
        {
            try {
                Files.writeString(path,s);
            } catch (IOException e) {
                logger.log(""+e);
            }
        };
        start_javalin_server(text_source,text_sink);
        open_browser();
    }



    void start_javalin_server(Supplier<String> text_source, Consumer<String> text_sink)
    {
        Runnable r = () -> {
            javalin = Javalin.create(config -> {
                // Serve the static HTML file
                config.staticFiles.add(
                        // SEE NOTE BELOW
                    "/javalin_monaco",
                    io.javalin.http.staticfiles.Location.CLASSPATH
                );
            }).start(port_number);

            // WebSocket Endpoint
            javalin.ws("/monaco-ws", ws -> {
                ws.onConnect(ctx -> {
                    ctx.session.setIdleTimeout(Duration.ofMillis(3600000L)); // 1 hour timeout
                    logger.log("WebSocket connected");
                    connected_clients.add(ctx);
                });
                ws.onMessage(ctx -> {
                    String msg = ctx.message();

                    if ("REQUEST_INIT".equals(msg))
                    {
                        // Client is fresh and wants the current text
                        ctx.send(text_source.get());
                        return;
                    }

                    if ( ultra_dbg) logger.log("Received from browser: " + msg);
                    if (text_source.get().equals(msg))
                    {
                        // already up to date
                        if ( ultra_dbg) logger.log("source already has: " + msg);
                    }
                    else
                    {
                        if ( ultra_dbg) logger.log("updating sink with: " + msg);
                        text_sink.accept(msg);
                    }
                });
                ws.onClose(ctx -> {
                    logger.log("Browser disconnected");
                    connected_clients.remove(ctx);
                });
            });

            logger.log("Javalin server started on port " + port_number);
        };

        Actor_engine.execute(r,"Javalin server",logger);
    }

    void stop_javalin_server() {
        if (javalin != null) {
            javalin.stop();
        }
    }

    void broadcast_to_browser(String text) {
        for (WsContext ctx : connected_clients) {
            if (ctx.session.isOpen()) {
                ctx.send(text);
            }
        }
    }



    public void open_browser() {
        try {
            // Double check index.html path:
            // In Javalin config above: config.staticFiles.add("/javalin_monaco/monaco", Location.CLASSPATH);
            // So http://localhost:8080/index.html should work if index.html is directly in that folder.
            String encoded_title = URLEncoder.encode(
                    page_title,
                    StandardCharsets.UTF_8
            );

            URI uri = URI.create(
                    "http://localhost:" +
                            port_number +
                            "/index.html?v=" +
                            System.currentTimeMillis() +
                            "&title=" +
                            encoded_title
            );
            application.getHostServices().showDocument(uri.toString());

            /*
            Desktop.getDesktop().browse(
                    URI.create(
                            "http://localhost:" +
                                    port_number +
                                    "/index.html?v=" +
                                    System.currentTimeMillis() +
                                    "&title=" +
                                    encoded_title
                    )
            );*/

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
