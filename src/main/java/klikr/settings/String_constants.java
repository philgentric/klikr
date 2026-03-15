package klikr.settings;

import javafx.stage.Window;
import klikr.util.Shared_services;
import klikr.util.files_and_paths.Guess_file_type;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

//**********************************************************
public class String_constants
//**********************************************************
{
    public static final String ICON_SIZE = "ICON_SIZE";
    public static final String FOLDER_ICON_SIZE = "FOLDER_ICON_SIZE";
    public static final String VIDEO_SAMPLE_LENGTH = "VIDEO_SAMPLE_LENGTH";
    public static final String LANGUAGE_KEY = "LANGUAGE";
    public static final String FONT_SIZE = "FONT_SIZE";
    public static final String COLUMN_WIDTH = "Column_width"; //this must match the resource bundles
    public static final String STYLE_KEY = "STYLE";
    //public static final String CUSTOM_COLOR = "Custom_color";
    public static final String CACHE_FILE_MAX_LIFE = "CACHE_FILE_MAX_LIFE"; // in days
    public static final String USER_HOME = "user.home";
    public static final String CONF_DIR = ".klikr";
    public static final String AUDIO_PLAYER_CURRENT_SONG = "AUDIO_PLAYER_CURRENT_SONG";
    public static final String PLAYLIST_FILE_NAME = "PLAYLIST_FILE_NAME";

    public static final String PROPERTIES_FILENAME = "klikr.properties";
    public static final String TRASH_DIR = "trash";
    public static final String FACE_RECO_DIR = "face_reco";
    public static final String JAVA_VM_MAX_RAM = "max_RAM_in_GBytes"; // this is the maximum RAM that the Java VM can use, in GBytes
    public static final String PURPOSE = "Java VM max ram";
    public static final String RAM_FILENAME = "ram";
    public static final String PRIVACY_SCREEN = ".privacy_screen";
    public static final String HOW_MANY_TIMES = "HOW_MANY_TIMES";
    static final String NUMBER_OF_IMAGE_SIMILARITY_SERVERS = "NUMBER_OF_IMAGE_SIMILARITY_SERVERS";

    public static final String ULTIM = "_ultim"; // must be lowercase because we test name.toLowerCase.contains("_ultim")
    public static final String SCREEN_TOP_LEFT_X = "_SCREEN_TOP_LEFT_X";
    public static final String SCREEN_TOP_LEFT_Y = "_SCREEN_TOP_LEFT_Y";
    public static final String SCREEN_WIDTH = "_SCREEN_WIDTH";
    public static final String SCREEN_HEIGHT = "_SCREEN_HEIGHT";
    static final String DISK_CACHE_SIZE_WARNING_MEGABYTES = "DISK_CACHE_SIZE_WARNING_MEGABYTES";


    //**********************************************************
    public static String get_current_song(Window owner)
    //**********************************************************
    {
        File_storage pm = Shared_services.main_properties();
        return pm.get(String_constants.AUDIO_PLAYER_CURRENT_SONG);
    }

    //**********************************************************
    public static void save_current_song(String path, Window owner)
    //**********************************************************
    {
        File_storage pm = Shared_services.main_properties();
        pm.set_and_save(String_constants.AUDIO_PLAYER_CURRENT_SONG, path);

    }
    //**********************************************************
    public static Path get_playlist_path(Window owner)
    //**********************************************************
    {
        String playlist_file_name = Shared_services.main_properties().get(String_constants.PLAYLIST_FILE_NAME);
        if (playlist_file_name != null)
        {
            Path p = Path.of(playlist_file_name);
            if (p.isAbsolute())
            {
                if (p.toFile().exists())
                {
                    return p; // OK, loading recorded playlist after checking
                }
            }
        }

        // new empty playlist with default name
        playlist_file_name = "playlist." + Guess_file_type.KLIKR_AUDIO_PLAYLIST_EXTENSION;
        Shared_services.main_properties().set_and_save(String_constants.PLAYLIST_FILE_NAME, playlist_file_name);
        String home = System.getProperty(String_constants.USER_HOME);
        Path p = Paths.get(home, String_constants.CONF_DIR, playlist_file_name);
        return p;
    }

}
