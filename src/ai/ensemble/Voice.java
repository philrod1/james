package ai.ensemble;

import java.util.List;

import ai.common.Game;
import ai.common.MOVE;

public interface Voice {
	double[] getPreferences(Game game, List<MOVE> safe, boolean turbo);
}
