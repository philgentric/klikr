package klikr.path_lists.navigator;

import klikr.Window_provider;
import klikr.path_lists.Path_list_provider;
import klikr.util.execute.actor.Aborter;

// A Navigator can just do previous/next PATH
// it can be on a folder or on a playlist
//**********************************************************
public interface Navigator
//**********************************************************
{
    void previous(Aborter aborter, Window_provider browser);
    void next(Aborter aborter, Window_provider browser);
    Path_list_provider get_path_list_provider();
}
