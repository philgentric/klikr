package klikr.path_lists.navigator;

import klikr.File_comparator_provider;
import klikr.Owner_provider;
import klikr.path_lists.Path_list_provider;
import klikr.util.execute.actor.Aborter;

// A Navigator can just do previous/next PATH
// it can be on a folder or on a playlist
//**********************************************************
public interface Navigator
//**********************************************************
{
    void previous(Aborter aborter, File_comparator_provider browser);
    void next(Aborter aborter, File_comparator_provider browser);
    Path_list_provider get_path_list_provider();
}
