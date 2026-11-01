// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.util;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import klikr.look.Look_and_feel;
import klikr.look.Look_and_feel_manager;
import klikr.settings.boolean_features.Feature;
import klikr.settings.boolean_features.Feature_cache;
import klikr.util.execute.Guess_OS;
import klikr.util.execute.Script_executor;
import klikr.util.log.Logger;
import klikr.util.ui.Items_with_explanation;

import java.nio.file.Path;
import java.util.List;

//**********************************************************
public enum External_application
//**********************************************************
{
    Ffmpeg,
    Ffprobe,
    GraphicsMagick,
    MediaInfo,
    Ytdlp,
    AcousticID_chromaprint_fpcalc,
    ImageMagick,
    Vips;


    //**********************************************************
    public HBox get_button(double width, double icon_size, Look_and_feel look_and_feel, Kontext context)
    //**********************************************************
    {
        EventHandler<ActionEvent> handler =  e ->
        {
            boolean enable_install_debug = Feature_cache.get(Feature.Enable_install_debug);
            List<String> lines = get_command_strings_to_install(context);
            if ( lines.isEmpty())
            {
                context.log("FATAL get_command_string_to_install() returns empty for "+name());
                return;
            }
            for (String line : lines)
            {
                Script_executor.execute(List.of(line), enable_install_debug, context);
            }
        };

        return Items_with_explanation.make_hbox_with_button_and_explanation(
                get_install_I18n_key(),
                handler,
                width,
                icon_size,
                look_and_feel,
                context);
    }



    //**********************************************************
    public List<String> get_command_strings_to_install(Kontext context)
    //**********************************************************
    {
        switch(Guess_OS.guess(context.logger()))
        {
            case MacOS -> {return get_macOS_install_commands();}
            case Linux -> {return get_Linux_install_commands(context);}
            case Windows -> {return get_Windows_install_commands();}
            case Unknown -> {return List.of();}
        }
        return List.of();
    }



    //**********************************************************
    public String get_install_I18n_key()
    //**********************************************************
    {
        // this is NOT for display: this MUST be the exact string
        // as found in the ressource bundles
        return switch (this) {
            case Ytdlp -> "Install_Youtubedownloader";
            case AcousticID_chromaprint_fpcalc -> "Install_Fpcalc";
            case ImageMagick -> "Install_Imagemagick";
            case Ffmpeg, Ffprobe -> "Install_Ffmpeg";
            case Vips -> "Install_Vips";
            case GraphicsMagick -> "Install_Graphicsmagick";
            case MediaInfo -> "Install_Mediainfo";
        };
    }

    //**********************************************************
    public List<String> get_macOS_install_commands()
    //**********************************************************
    {
        // this is NOT for display: this MUST be th exact required string in:
        // brew install <REQUIRED_STRING>
        return switch (this) {
            case Ytdlp -> List.of("brew install yt-dlp");
            case AcousticID_chromaprint_fpcalc -> List.of("brew install chromaprint");
            case ImageMagick -> List.of("brew install imagemagick");
            case Ffmpeg, Ffprobe -> List.of("brew install ffmpeg");
            case Vips -> List.of("brew install vips");
            case GraphicsMagick -> List.of("brew install gs","brew install graphicsmagick");
            case MediaInfo -> List.of("brew install mediainfo");
        };
    }

    //**********************************************************
    public List<String> get_Windows_install_commands()
    //**********************************************************
    {
        // this is NOT for display: this MUST be th exact required string in:
        // brew install <REQUIRED_STRING>
        return switch (this) {
            case Ytdlp -> List.of("choco install yt-dlp -y");
            case AcousticID_chromaprint_fpcalc -> List.of("choco install chromaprint -y");
            case ImageMagick -> List.of("choco install imagemagick -y");
            case Ffmpeg, Ffprobe -> List.of("choco install ffmpeg -y");
            case Vips -> List.of("choco install vips -y");
            case GraphicsMagick -> List.of("choco install graphicsmagick -y");
            case MediaInfo -> List.of("choco install mediainfo -y");
        };
    }


    //**********************************************************
    public List<String> get_Linux_install_commands(Kontext context)
    //**********************************************************
    {
        // this is NOT for display: this MUST be the exact required string
        return switch (this)
        {
            case Ytdlp -> List.of("brew install yt-dlp");
            case AcousticID_chromaprint_fpcalc -> List.of("brew install chromaprint");
            case ImageMagick -> List.of("brew install imagemagick");
            case Vips -> List.of("brew install vips");
            case GraphicsMagick -> List.of("brew install gs","brew install graphicsmagick");
            case MediaInfo -> List.of("brew install mediainfo");
            case Ffmpeg, Ffprobe -> special(context);
        };
    }

    //**********************************************************
    private List<String> special(Kontext context)
    //**********************************************************
    {
        {
            // super important: on Linux javaFX audio i.e. the audio player REQUIRES ffmpeg
            TextInputDialog dialog = new TextInputDialog("");
            Look_and_feel_manager.set_dialog_look(dialog,context.logger());
            dialog.initOwner(context.owner());
            dialog.setWidth(1200);
            VBox vbox = new VBox();
            //PasswordField pwf = new PasswordField();
            //vbox.getChildren().add(pwf);
            Label l1 = new Label("Installing ffmpeg is required for audio playback (and more features)");
            vbox.getChildren().add(l1);
            Label l2 = new Label("The command is:");
            vbox.getChildren().add(l2);
            Label l3 = new Label("sudo apt install ffmpeg");
            vbox.getChildren().add(l3);
            dialog.getDialogPane().setContent(vbox);

            dialog.showAndWait();
            dialog.close();
        }
        return List.of();
    }

    //**********************************************************
    public String get_command(Kontext context)
    //**********************************************************
    {
        switch(Guess_OS.guess(context.logger()))
        {
            case MacOS -> {return get_macOS_command();}
            case Linux -> {return get_Linux_command(context);}
            case Windows -> {return get_Windows_command();}
            case Unknown -> {return "";}
        }
        return "";
    }

    //**********************************************************
    private String get_Windows_command()
    //**********************************************************
    {
        return switch (this) {
            case Ytdlp -> "yt-dlp";
            case AcousticID_chromaprint_fpcalc -> "fpcalc";
            case ImageMagick -> "magick";
            case Ffmpeg -> "ffmpeg";
            case Ffprobe -> "ffprobe";
            case Vips -> "not used";
            case GraphicsMagick -> "gm";
            case MediaInfo -> "mediainfo";
        };
    }

    //**********************************************************
    private String get_Linux_command(Kontext context)
    //**********************************************************
    {
        return switch (this) {
            case Ytdlp -> "yt-dlp";
            case AcousticID_chromaprint_fpcalc -> "fpcalc";
            case ImageMagick -> "magick";
            case Ffmpeg -> "ffmpeg";
            case Ffprobe -> "ffprobe";
            case Vips -> "not used";
            case GraphicsMagick -> "gm";
            case MediaInfo -> "mediainfo";
        };
    }

    //**********************************************************
    private String get_macOS_command()
    //**********************************************************
    {
        // in macOS, an app installed with a DMG has a restricted PATH
        // especially /opt/homebrew/bin (stuff installed with brew)
        // is NOT in the path ... so we do it explicitly
        return switch (this) {
            case Ytdlp -> "/opt/homebrew/bin/yt-dlp";
            case AcousticID_chromaprint_fpcalc -> "/opt/homebrew/bin/fpcalc";
            case ImageMagick -> "/opt/homebrew/bin/magick";
            case Ffmpeg -> "/opt/homebrew/bin/ffmpeg";
            case Ffprobe -> "/opt/homebrew/bin/ffprobe";
            case Vips -> "not used";
            case GraphicsMagick -> "/opt/homebrew/bin/gm";
            case MediaInfo -> "/opt/homebrew/bin/mediainfo";
        };
    }
}
