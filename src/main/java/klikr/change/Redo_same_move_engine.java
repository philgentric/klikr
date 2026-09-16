// Copyright (c) 2025 Philippe Gentric
// SPDX-License-Identifier: MIT

package klikr.change;

import javafx.stage.Stage;
import klikr.util.Kontext;
import klikr.util.execute.actor.Aborter;
import klikr.util.files_and_paths.*;
import klikr.change.old_and_new.Command;
import klikr.change.old_and_new.Old_and_new_Path;
import klikr.change.old_and_new.Status;
import klikr.util.log.Logger;
import klikr.util.log.Stack_trace_getter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//**********************************************************
public class Redo_same_move_engine
//**********************************************************
{

    static public Path last_destination_folder = null;
    //**********************************************************
    public static void same_move(Path old_path, Kontext context)
//**********************************************************
    {
        if ( last_destination_folder == null)
        {
            context.log(Stack_trace_getter.get_stack_trace(Logger.error+"PANIC "));
            return;
        }
        Path new_path = Path.of(last_destination_folder.toAbsolutePath().toString(), old_path.getFileName().toString());
        context.log("Redo_same_move_engine.same_move:\n    old: "+old_path+"\n    new: "+new_path);
        Old_and_new_Path oanp = new Old_and_new_Path(old_path,new_path, Command.command_move, Status.before_command,false);
        List<Old_and_new_Path> ll = new ArrayList<>();
        ll.add(oanp);
        Moving_files.perform_safe_moves_in_a_thread(ll, true,context);
    }
}
