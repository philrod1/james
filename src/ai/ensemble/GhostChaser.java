package ai.ensemble;

import java.awt.Point;
import java.util.List;
import java.util.Random;

import ai.common.Game;
import ai.common.MOVE;
import ai.common.simulator.ghosts.Ghost;

public class GhostChaser implements Voice {

    private final Random rng = new Random();
    private final static double EPSILON = 1e-3;

    @Override
    public double[] getPreferences(Game game, List<MOVE> moves) {
        double[] results = new double[]{0,0,0,0};
        for(MOVE move : moves) {
            Point p = game.pacman.getTilePosition();
            if(game.pacman.isEnergised()) {
                int minDistance = Integer.MAX_VALUE;
                for (Game.GHOST ghost : Game.GHOST.values()) {
                    Point g = game.getTilePosition(ghost);
                    if (!game.isEdible(ghost) && game.getState(ghost) == 4) {
                        int distance = game.getMaze().distancePath(p, g, move);
                        if (distance < minDistance) {
                            minDistance = distance;
                        }
                    }
                }
                results[move.ordinal()] = 1.0 / minDistance + rng.nextDouble() * EPSILON;
            }
        }
        return results;
    }

}