package klikr.audio.simple_player;

import klikr.browser.virtual_landscape.Path_comparator_source;
import klikr.path_lists.Path_list_provider;
import klikr.properties.File_storage;
import klikr.properties.boolean_features.Feature;
import klikr.properties.boolean_features.Feature_cache;
import klikr.search.Finder;
import klikr.util.Shared_services;
import klikr.util.execute.actor.Aborter;
import klikr.util.files_and_paths.Guess_file_type;
import klikr.util.log.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static klikr.audio.old_player.Playlist.AUDIO_PLAYER_CURRENT_SONG;

//**********************************************************
public class Navigator_auto implements Navigator
//**********************************************************
{
    public final static boolean dbg = false;
    public final Logger logger;
    private final Path[] current_song = new Path[1];
    private final Supplier<Path> get_current;
    private final Consumer<Path> play;
    private final Path_list_provider path_list_provider;
    //private final Path_comparator_source path_comparator_source;
    //**********************************************************
    public Navigator_auto(
            Path start_song, Path_list_provider path_list_provider,
            //Path_comparator_source path_comparator_source,
            Logger logger)
    //**********************************************************
    {
        //this.path_comparator_source = path_comparator_source;
        this.path_list_provider = path_list_provider;
        this.logger = logger;
        this.current_song[0] = start_song;

        this.get_current = () -> current_song[0];
        this.play = path -> {
            current_song[0] = path;
            Basic_audio_player.play_song(path.toAbsolutePath().toString(),true);
        };

    }

    //**********************************************************
    @Override
    public void previous()
    //**********************************************************
    {
        List<Path> paths = path_list_provider.only_song_paths(Feature_cache.get(Feature.Show_hidden_files));
        int index;
        if ( get_current.get() == null)
        {
            index = 0;
            File_storage pm = Shared_services.main_properties();
            String song = pm.get(AUDIO_PLAYER_CURRENT_SONG);
            if ( song != null )
            {
                index = paths.indexOf(Paths.get(song));
                if (index == -1)
                {
                    logger.log("WARNING: target song NOT in playlist???");
                    index = 0;
                }
            }
        }
        else
        {
            index = paths.indexOf(get_current.get());
            if (index == -1)
            {
                // casting error !
                if (dbg) logger.log("WARNING: last played song NOT in playlist, starting playlist");
                index = 0;
            }
        }
        Folder_navigator.previous(paths,index,play,logger);
    }

    //**********************************************************
    @Override
    public void next()
    //**********************************************************
    {
        List<Path> paths = path_list_provider.only_song_paths(Feature_cache.get(Feature.Show_hidden_files));
        //logger.log("jump_to_next "+paths.size()+" songs");
        int index;
        if ( get_current.get() == null)
        {
            index = 0;
        }
        else
        {
            index = paths.indexOf(get_current.get());
        }
        Folder_navigator.next(paths,index,play,logger);

    }
}
