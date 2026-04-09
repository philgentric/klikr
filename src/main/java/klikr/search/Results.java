package klikr.search;

import javafx.stage.Window;

public interface Results {
    void inject_search_results(Search_result sr, String keys, boolean is_max, Window window);

    void erase_all_non_max();

    void has_ended();
}
