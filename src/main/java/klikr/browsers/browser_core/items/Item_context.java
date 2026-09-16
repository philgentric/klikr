package klikr.browsers.browser_core.items;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import klikr.Window_type;
import klikr.browsers.browser_core.virtual_landscape.Path_comparator_source;
import klikr.browsers.browser_core.virtual_landscape.Shutdown_target;
import klikr.path_lists.Path_list_provider;
import klikr.util.Kontext;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.nio.file.Path;

//**********************************************************
public class Item_context
//**********************************************************
{
    public final boolean is_trash;
    public final Shutdown_target shutdown_target;
    public final Path top_left;
    Path_list_provider path_list_provider;
    public Path item_path;
    public final Iconifiable_item_type item_type;
    Color tag_color; //may be null
    final Scene scene;
    final Path_comparator_source path_comparator_source;
    final Application application;
    final Window_type window_type;
    public final Kontext context;

    //**********************************************************
    public Item_context(
            Path item_path,
            Path_list_provider path_list_provider,
            Iconifiable_item_type  item_type,
            boolean isTrash,
            Shutdown_target shutdownTarget,
            Color tag_color,
            Scene scene,
            Path_comparator_source pathComparatorSource,
            Application application,
            Window_type windowType,
            Path topLeft,
            Kontext context
            )
//**********************************************************
    {
        this.item_path = item_path;
        //logger.log(Stack_trace_getter.get_stack_trace("Item_context constructor, item_path: " + this.item_path));

        this.context = context;
        if (item_type != null)
        {
            this.item_type = item_type;
        }
        else
        {
            this.item_type = Iconifiable_item_type.determine(item_path,context);
        }
        //logger.log("Item_context constructor, item_type: " + this.item_type);
        is_trash = isTrash;
        if ( item_path == null) context.log(Stack_trace_getter.get_stack_trace("WARNING: null item_path" +dump_item_context()));
        shutdown_target = shutdownTarget;
        top_left = topLeft;

        this.tag_color = tag_color;
        this.scene = scene;
        path_comparator_source = pathComparatorSource;
        this.application = application;
        window_type = windowType;
        this.path_list_provider = path_list_provider;
    }

    void log(String s)
    {
        context.log(s);
    }


    //**********************************************************
    private String dump_item_context()
    //**********************************************************
    {
        String returned =  " is_trash:"+is_trash+
                ", item_type: "+item_type.name();
        if ( item_path != null) returned += ", item_path:"+item_path.toString();
        else returned += ", item_path is null";
        if ( path_list_provider != null)
        {
            returned += ", path_list_provider: "+path_list_provider.get_key();
        }
        else {
            returned += ", path_list_provider is null";
        }
        return returned;
    }

    public Window owner()
    {
        return context.owner();
    }

    public Logger logger() {
        return context.logger();
    }
}
