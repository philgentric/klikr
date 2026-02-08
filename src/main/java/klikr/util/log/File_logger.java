// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

//SOURCES ./Simple_logger.java
package klikr.util.log;

import javafx.stage.Window;
import klikr.util.execute.actor.Actor_engine;
import klikr.util.files_and_paths.Static_files_and_paths_utilities;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

//*******************************************************
public class File_logger implements Logger
//*******************************************************
{

	private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
	//*******************************************************
	public File_logger(String prefix)
	//*******************************************************
	{

		Path file = get_tmp_file_path_in_logs(prefix);

		Runnable r = () -> {
			for(;;)
			{
				try {
					String s = queue.take();
					//System.out.println("File_logger: "+tag+" => "+s);
					FileWriter fw = new FileWriter(file.toFile(), true);
					fw.write(s+"\n");
					fw.flush();
					fw.close();
				}
				catch (InterruptedException e) {
					System.out.println("File_logger: InterruptedException "+e);
                    return;
				}
                catch (IOException e) {
					System.out.println("File_logger: IOException "+e);
                    return;
				}
			}
		};
		Actor_engine.execute(r, "File logger pump",new Simple_logger());
    }
	//*******************************************************
	@Override
	public void log( boolean also_System_out_println, String s)
	//*******************************************************
	{
		if ( also_System_out_println)
		{
			System.out.println(s);
		}
		queue.add(s);
	}

	//**********************************************************
	public static Path get_tmp_file_path_in_logs(String prefix)
	//**********************************************************
	{
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
		String uuid = LocalDateTime.now().format(dtf)+"_"+ UUID.randomUUID();
		String file_name = prefix+"_"+uuid+".txt";
		Path logs_folder = Static_files_and_paths_utilities.get_absolute_hidden_dir_on_user_home("logs", false, null,new Simple_logger());
		return logs_folder.resolve(file_name);
	}

}
