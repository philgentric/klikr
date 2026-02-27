package klikr.change.undo;

import javafx.application.Application;
import javafx.stage.Stage;
import klikr.util.Shared_services;
import klikr.util.log.Exceptions_in_threads_catcher;
import klikr.util.log.Logger;
import klikr.util.log.Simple_logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Undo_test_app extends Application
{

    @Override
    public void start(Stage stage) throws Exception
    {
        Shared_services.init("Undo_test_app",stage);
        Logger logger = Shared_services.logger();
        Exceptions_in_threads_catcher.set_exceptions_in_threads_catcher(logger);

        Undo_core undo_core = new Undo_core("../undo_test",null,logger);

        for ( int i = 0; i < 1200 ; i++)
        {
            Undo_item ui = new Undo_item(new ArrayList<>(), LocalDateTime.now().minusDays(378), UUID.randomUUID(),logger);
            undo_core.add(ui);
        }

        for ( int i = 0; i < 1200 ; i++)
        {
            Undo_item ui = new Undo_item(new ArrayList<>(), LocalDateTime.now().minusDays(25), UUID.randomUUID(),logger);
            undo_core.add(ui);
        }

        for ( int i = 0; i < 103 ; i++)
        {
            Undo_item ui = new Undo_item(new ArrayList<>(), LocalDateTime.now().minusDays(1), UUID.randomUUID(),logger);
            undo_core.add(ui);
        }

        stage.show();
        List<Undo_item> l = undo_core.read_all_undo_items_from_disk();

        logger.log("remaining : "+l.size());

    }
}
