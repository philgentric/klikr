package klikr.audio.simple_player;

import klikr.util.execute.actor.Aborter;

public interface Navigator {
    void previous(Aborter aborter);
    void next(Aborter aborter);
}
