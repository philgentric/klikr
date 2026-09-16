package klikr.util.log;

import klikr.util.Kontext;
import klikr.util.execute.Application_jar;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

//**********************************************************
public class Tmp_file_in_trash
//**********************************************************
{
    //**********************************************************
    public static Path create_copy_in_trash(String cmd, Kontext context)
    //**********************************************************
    {
        Path tmp_path = get_path_in_trash(cmd,context);
        if ( tmp_path == null)
        {
            context.log(Logger.error+"Fatal, cannot create path in trash: ->"+cmd+"<-");
            return null;
        }
        if (Files.exists(tmp_path))
        {
            context.log("NO OVERWRITE, file not copied in trash: ->"+cmd+"<-");
            return tmp_path;
        }

        String name = "/scripts/"+cmd;
        InputStream input_stream =  Application_jar.get_jar_InputStream_by_name(name);
        if ( input_stream == null)
        {
            context.log(Logger.error+"Fatal, cannot open jar stream for: ->"+name+"<-");
            return null;
        }
        context.log("OK jar stream opened for ->"+cmd+"<-");

        // create a temporary copy of this file:

        try
        {
            FileUtils.copyInputStreamToFile(input_stream, tmp_path.toFile());
            context.log("OK file copied ->"+cmd+"<-");
            return tmp_path;
        }
        catch (IOException e)
        {
            context.log("" + e);
            return null;
        }
    }


    //**********************************************************
    public static Path get_tmp_file_path_in_trash(String prefix, String extension,Kontext context)
    //**********************************************************
    {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String uuid = LocalDateTime.now().format(dtf)+"_"+ UUID.randomUUID();
        return get_path_in_trash(prefix+"_"+uuid+"."+extension,context);
    }

    //**********************************************************
    public static Path get_path_in_trash(String file_name, Kontext context)
    //**********************************************************
    {
        Path klik_trash = Static_files_and_paths_utilities.get_trash_dir_of(Path.of("").toAbsolutePath(),context);
        if ( klik_trash == null)
        {
            context.log(Stack_trace_getter.get_stack_trace(Logger.error+"Fatal "));
            return null;
        }
        return klik_trash.resolve(file_name);
    }

}
