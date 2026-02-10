package klikr.audio.simple_player;

import klikr.search.Finder;
import klikr.search.Finder_frame;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;


//**********************************************************
public class Folder_navigator implements Navigator
//**********************************************************
{
    public final Logger logger;
    private final Supplier<Path> get_current;
    private final Consumer<Path> play;
    //**********************************************************
    public Folder_navigator(Supplier<Path> get_current, Consumer<Path> play, Logger logger)
    //**********************************************************
    {
        this.logger = logger;
        this.get_current = get_current;
        this.play = play;

    }

    //**********************************************************
    @Override
    public void previous()
    //**********************************************************
    {
        if ( get_current.get() == null) return;
        File folder = get_current.get().getParent().toFile();
        File[] files = folder.listFiles();
        if ( files != null && files.length > 0 )
        {
            List<Path> list = new ArrayList<>();
            for (File file : files) list.add(file.toPath());
            int index = list.indexOf(get_current.get());
            previous(list, index,play,logger);
        }
    }


    //**********************************************************
    @Override
    public void next()
    //**********************************************************
    {
        if ( get_current.get() == null) return;
        File folder = get_current.get().getParent().toFile();
        File[] files = folder.listFiles();
        if ( files != null && files.length > 0 )
        {
            List<Path> list = new ArrayList<>();
            for (File file : files) list.add(file.toPath());
            int index = list.indexOf(get_current.get());
            next(list, index,play,logger);
        }
    }

    //**********************************************************
    public static void previous(List<Path> list, int index, Consumer<Path> play, Logger logger)
    //**********************************************************
    {
        for(int i = 0; i < list.size(); i++)
        {
            index = index - 1;
            if (index < 0) index = list.size() - 1;
            Path file = list.get(index);
            if ( Guess_file_type.is_this_path_a_music(file,logger))
            {
                play.accept(file);
                return;
            }
        }
        logger.log("previous song failed! (no songs?)");
    }


    //**********************************************************
    public static void next(List<Path> list, int index, Consumer<Path> play, Logger logger)
    //**********************************************************
    {
        for(int i = 0; i < list.size(); i++)
        {
            index = index + 1;
            if (index >= list.size()) index = 0;
            Path p = list.get(index);
            if ( Guess_file_type.is_this_path_a_music(p,logger))
            {
                logger.log("OK, next is a song: "+p);
                play.accept(p);
                return;
            }
            else
            {
                logger.log("skipped, as not a song: "+p);
            }
        }
        logger.log("next song failed! (no songs?)");
    }
}
