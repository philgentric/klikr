package klikr.path_lists.navigator;

import javafx.stage.Window;
import klikr.File_comparator_provider;
import klikr.Owner_provider;
import klikr.path_lists.Path_list_provider;
import klikr.util.execute.actor.Aborter;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

// a Navigator for a path_list_provider
// e.g. works for folders and playlists
//**********************************************************
public class General_navigator implements Navigator
//**********************************************************
{
    public final Navigation_type navigation_type;
    public final Logger logger;
    public final Window owner;
    private final Path_list_provider path_list_provider;
    private final Consumer<Path> path_consumer_ie_player;
    private final Supplier<Path> current_path_supplier;
    //**********************************************************
    public General_navigator(Navigation_type navigation_type, Path_list_provider path_list_provider, Supplier<Path> current_path_supplier, Consumer<Path> path_consumer_ie_player, Window owner, Logger logger)
    //**********************************************************
    {
        this.owner = owner;
        this.navigation_type = navigation_type;
        this.logger = logger;
        this.path_list_provider = path_list_provider;
        this.current_path_supplier = current_path_supplier;
        this.path_consumer_ie_player = path_consumer_ie_player;
        if ( path_list_provider == null)
        {
            logger.log(Stack_trace_getter.get_stack_trace("FATAL: no path provider"));
        }
    }

    //**********************************************************
    @Override
    public void previous(Aborter aborter, File_comparator_provider file_comparator_provider)
    //**********************************************************
    {
        List<Path> paths = get_paths(aborter,file_comparator_provider);
        if ( current_path_supplier.get() == null)
        {
            logger.log("FATAL: no current_path_supplier provider");
            return;
        }
        Path previously = current_path_supplier.get();
        int index = paths.indexOf(previously);
        for(int i = 0; i < paths.size(); i++)
        {
            index = index - 1;
            if (index < 0) index = paths.size() - 1;
            Path path = paths.get(index);
            if ( Guess_file_type.is_this_path_a_music(path,logger))
            {
                path_consumer_ie_player.accept(path);
                return;
            }
        }
        logger.log("previous song failed! (no songs?)");
    }

    //**********************************************************
    @Override
    public void next(Aborter aborter, File_comparator_provider file_comparator_provider)
    //**********************************************************
    {
        List<Path> paths = get_paths(aborter, file_comparator_provider);
        if ( current_path_supplier.get() == null)
        {
            logger.log("FATAL: no current_path_supplier provider");
            return;
        }
        Path previously = current_path_supplier.get();
        int index = paths.indexOf(previously);
        for(int i = 0; i < paths.size(); i++)
        {
            index = index + 1;
            if (index >= paths.size()) index = 0;
            Path path = paths.get(index);
            if ( Guess_file_type.is_this_path_a_music(path,logger))
            {
                //logger.log("OK, next is a song: "+path);
                path_consumer_ie_player.accept(path);
                return;
            }
            else
            {
                logger.log("skipped, as not a song: "+path);
            }
        }
        logger.log("next song failed! (no songs?)");
    }

    //**********************************************************
    @Override
    public Path_list_provider get_path_list_provider()
    //**********************************************************
    {
        return path_list_provider;
    }

    //**********************************************************
    public static Path previous(List<Path> list, int index, Consumer<Path> player, Logger logger)
    //**********************************************************
    {

        return null;
    }



    //**********************************************************
    private List<Path> get_paths(Aborter aborter, File_comparator_provider file_comparator_provider)
    //**********************************************************
    {
        List<Path> paths = null;
        switch ( navigation_type)
        {
            case songs:
                paths = path_list_provider.only_song_paths(true, true, aborter);
                break;
            case images:
                paths = path_list_provider.only_image_paths(true, true, aborter);
                break;
            case all_files:
                paths = path_list_provider.only_file_paths(true, true, aborter);
                break;
        }
        if (paths == null) return new ArrayList<>();
        if ( file_comparator_provider != null) Collections.sort(paths,file_comparator_provider.get_file_comparator());
        return paths;
    }


}
