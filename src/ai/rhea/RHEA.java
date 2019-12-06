package ai.rhea;
import ai.AbstractAI;
import ai.common.Game;
import ai.common.MOVE;
import ai.common.Maze;
import ai.common.simulator.SimGame;
import ai.common.simulator.data.SimData;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RHEA extends AbstractAI {

    private final Game.PacMan pacman;
    private MOVE move = MOVE.LEFT;
    private Point target;
    private final MOVE[] moves = MOVE.values();
    private SimGame sim;
    // Parameters
    private int POPULATION_SIZE = 10;
    private int SIMULATION_DEPTH = 10;
    private int CROSSOVER_TYPE = UNIFORM_CROSS;
    private boolean REEVALUATE = false;
    private int MUTATION = 1;
    private int TOURNAMENT_SIZE = 2;
    private int ELITISM = 1;

    // Constants
    private final long BREAK_MS = 10;
    public static final double epsilon = 1e-6;
    static final int POINT1_CROSS = 0;
    static final int UNIFORM_CROSS = 1;

    // Class vars
    private Individual[] population, nextPop;
    private int NUM_INDIVIDUALS;
    private int N_ACTIONS;
//    private HashMap<Integer, Types.ACTIONS> action_mapping;
    private Random rng;

    public RHEA(Game game) {
        System.out.println("New agent");
        rng = new Random();
        this.game = game;
        pacman = game.pacman;
        sim = new SimGame(game);
        reset();
    }

    @Override
    protected MOVE play() {
        System.out.println("RHEA playing");
        System.out.println("Get ready to move ...");

        move = act(game.getMaze());

        System.out.println("Boom! " + move);
        System.out.println();
        target = game.getMaze().getNextTile(target, move);
        System.out.println("Target = " + target);
        return move;
    }


    private MOVE act(Maze maze) {
        System.out.println("ACT TARGET = " + target);
        System.out.println("MOVE  : " + move);
        NUM_INDIVIDUALS = 0;
//        keepIterating = true;

        AtomicReference<SimData> data = new AtomicReference<>(new SimData(game.getSnapshot()));
        sim.syncToDataPoint(data.get());
        sim.advanceToTarget(target, maze);
        data.set(sim.getSimData());
        // INITIALISE POPULATION
        System.out.println("Init population");
        initPopulation(data.get(), maze);
        System.out.println("Init done");
        // RUN EVOLUTION
        System.out.println("PacMan: " + pacman.getTilePosition());
        System.out.println("Target: " + target);
        System.out.println("MOVE  : " + move);
        int n = 0;
        while (!pacman.getTilePosition().equals(target)) {
            runIteration(data.get(), maze);
            if(game.getState() != Game.STATE.PLAYING) {
                System.out.println("Resetting");
                reset();
                return MOVE.LEFT;
            }
        }
        System.out.println(population[0]);
        // RETURN ACTION
        return getBestAction(population);
    }

    /**
     * Run evolutionary process for one generation
     */
    private void runIteration(SimData data, Maze maze) {

        if (REEVALUATE) {
            for (int i = 0; i < ELITISM; i++) {
                evaluate(population[i], data, maze);
            }
        }

        if (NUM_INDIVIDUALS > 1) {
            for (int i = ELITISM; i < NUM_INDIVIDUALS; i++) {
                Individual newind = crossover(data, maze).mutate(MUTATION);

                // evaluate new individual, insert into population
                addIndividual(newind, nextPop, i, data, maze);
            }

            Arrays.sort(nextPop, (o1, o2) -> {
                if (o1 == null && o2 == null) {
                    return 0;
                }
                if (o1 == null) {
                    return 1;
                }
                if (o2 == null) {
                    return -1;
                }
                return o1.compareTo(o2);
            });

        } else if (NUM_INDIVIDUALS == 1){
            Individual newind = new Individual(SIMULATION_DEPTH, moves, rng, getRandomStartMove(data, maze)).mutate(MUTATION);
            evaluate(newind, data, maze);
            if (newind.value > population[0].value)
                nextPop[0] = newind;
        }
        population = nextPop.clone();
    }

    /**
     * Evaluates an individual by rolling the current state with the actions in the individual
     * and returning the value of the resulting state; random action chosen for the opponent
     * @return - value of last state reached
     */
    private double evaluate(Individual individual, SimData data, Maze maze) {
        sim.syncToDataPoint(data);
//        if (!sim.advanceToNextTile(move, maze)) {
//            individual.value = -10.0;
//            return -10.0;
//        }
        if (!maze.getAvailableMoves(sim.simPacman.tile).contains(individual.getActions()[0])) {
            individual.value = -10.0;
            return -10.0;
        }
        for (MOVE move : individual.getActions()) {
            if (!maze.getAvailableMoves(sim.simPacman.tile).contains(move)) {
                continue;
            }
            if (sim.simPacman.isAlive()) {
                sim.advanceToNextTile(move, maze);
            } else {
                break;
            }
        }
        individual.value = (sim.simPacman.isAlive()) ? -1000.0 : (sim.getScore() - game.getScore());
        return individual.value;
    }

    /**
     * @return - the individual resulting from crossover applied to the specified population
     */
    private Individual crossover(SimData data, Maze maze) {
        Individual newind = null;
        if (NUM_INDIVIDUALS > 1) {
            newind = new Individual(SIMULATION_DEPTH, moves, rng, getRandomStartMove(data, maze));
            Individual[] tournament = new Individual[TOURNAMENT_SIZE];
            ArrayList<Individual> list = new ArrayList<>(Arrays.asList(population));

            //Select a number of random distinct individuals for tournament and sort them based on value
            for (int i = 0; i < TOURNAMENT_SIZE; i++) {
                int index = rng.nextInt(list.size());
                tournament[i] = list.get(index);
                list.remove(index);
            }
            Arrays.sort(tournament);

            //get best individuals in tournament as parents
            if (TOURNAMENT_SIZE >= 2) {
                newind.crossover(tournament[0], tournament[1], CROSSOVER_TYPE);
            } else {
                System.out.println("WARNING: Number of parents must be LESS than tournament size.");
            }
        }
        return newind;
    }

    /**
     * Insert a new individual into the population at the specified position by replacing the old one.
     */
    private void addIndividual(Individual newind, Individual[] pop, int idx, SimData data, Maze maze) {
        evaluate(newind, data, maze);
        pop[idx] = newind.copy();
    }

    /**
     * Initialize population
     */
    private void initPopulation(SimData data, Maze maze) {
        sim.syncToDataPoint(data);
        N_ACTIONS = moves.length + 1;

        population = new Individual[POPULATION_SIZE];

        nextPop = new Individual[POPULATION_SIZE];

        for (int i = 0; i < POPULATION_SIZE; i++) {
            population[i] = new Individual(SIMULATION_DEPTH, moves, rng, getRandomStartMove(data, maze));
            evaluate(population[i], data, maze);
            NUM_INDIVIDUALS = i+1;
        }

        if (NUM_INDIVIDUALS > 1)
            Arrays.sort(population, (o1, o2) -> {
                if (o1 == null && o2 == null) {
                    return 0;
                }
                if (o1 == null) {
                    return 1;
                }
                if (o2 == null) {
                    return -1;
                }
                return o1.compareTo(o2);
            });
        for (int i = 0; i < NUM_INDIVIDUALS; i++) {
            if (population[i] != null)
                nextPop[i] = population[i].copy();
        }

    }

    private MOVE getRandomStartMove(SimData data, Maze maze) {
        sim.syncToDataPoint(data);
        List<MOVE> legalMoves = maze.getAvailableMoves(sim.simPacman.tile);
        return legalMoves.get(rng.nextInt(legalMoves.size()));
    }

    /**
     * @param pop - last population obtained after evolution
     * @return - first action of best individual in the population (found at index 0)
     */
    private MOVE getBestAction(Individual[] pop) {
        return pop[0].getActions()[0];
    }

    protected void reset() {
        target = new Point(14,24);
        move = MOVE.LEFT;
        game.makeMove(move);
    }

}