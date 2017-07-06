package ai.ensemble;

import ai.common.Game;
import ai.common.MOVE;
import ai.common.Maze;

import java.util.List;


public class RulesGhostDodger implements Voice {
    @Override
    public double[] getPreferences(Game game, List<MOVE> safe) {
        double[] prefs = new double[4];
        int ghostCount = 0;
        for (Game.GHOST ghost : Game.GHOST.values()) {
            if (!game.isEdible(ghost)) {
                ghostCount++;
                for (MOVE move : safe) {
                    prefs[move.ordinal()] += Math.max(1, game.getMaze().distancePath(game.pacman.getTilePosition(), game.getTilePosition(ghost), move) - 10);
                }
            } else {
                for (MOVE move : safe) {
                    prefs[move.ordinal()] = 40;
                }
            }
        }

        for (MOVE move : safe) {
            prefs[move.ordinal()] = prefs[move.ordinal()] / (ghostCount * 10);
        }
        System.out.println();
        return prefs;
    }
}
