package ai.rhea;

import ai.common.MOVE;

import java.util.Random;

public class Individual implements Comparable {

    private final MOVE[] moves;
    private final MOVE start;
    private MOVE[] actions;
    private int nLegalActions; // number of legal actions
    protected double value;
    private Random rng;

    Individual(int length, MOVE[] moves, Random rng, MOVE start) {
        actions = new MOVE[length];
        actions[0] = start;
        for (int i = 1; i < length; i++) {
            actions[i] = moves[rng.nextInt(moves.length)];
        }
        this.nLegalActions = moves.length;
        this.moves = moves;
        this.rng = rng;
        this.start = start;
    }

    private void setActions(MOVE[] a) {
        System.arraycopy(a, 0, actions, 0, a.length);
    }

    public MOVE[] getActions() {
        return actions;
    }

    /**
     * Returns new individual
     *
     * @param MUT - number of genes to mutate
     * @return - new individual, mutated from this
     */
    Individual mutate(int MUT) {
        Individual b = this.copy();
        b.setActions(actions);

        int count = 0;
        if (nLegalActions > 1) { // make sure you can actually mutate
            while (count < MUT) {
                int a; // index of action to mutate

                // random mutation of one action
                a = rng.nextInt(b.actions.length);

                b.actions[a] = moves[rng.nextInt(moves.length)];

                count++;
            }
        }

        return b;
    }

    /**
     * Modifies individual
     *
     * @param CROSSOVER_TYPE - type of crossover
     */
    public void crossover(Individual parent1, Individual parent2, int CROSSOVER_TYPE) {
        if (CROSSOVER_TYPE == RHEA.POINT1_CROSS) {
            // 1-point
            int p = rng.nextInt(actions.length - 3) + 1;
            for (int i = 0; i < actions.length; i++) {
                if (i < p)
                    actions[i] = parent1.actions[i];
                else
                    actions[i] = parent2.actions[i];
            }

        } else if (CROSSOVER_TYPE == RHEA.UNIFORM_CROSS) {
            // uniform
            for (int i = 0; i < actions.length; i++) {
                if (rng.nextFloat() >= 0.5)
                    actions[i] = parent1.actions[i];
                else
                    actions[i] = parent2.actions[i];
            }
        }
    }

    @Override
    public int compareTo(Object o) {
        Individual a = this;
        Individual b = (Individual) o;
        return Double.compare(b.value, a.value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Individual)) return false;

        Individual a = this;
        Individual b = (Individual) o;

        for (int i = 0; i < actions.length; i++) {
            if (a.actions[i] != b.actions[i]) return false;
        }

        return true;
    }

    public Individual copy() {
        Individual a = new Individual(actions.length, moves, rng, start);
        a.value = value;
        a.setActions(actions);

        return a;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("" + value + ": ");
        for (MOVE action : actions) {
            s.append(action).append(" ");
        }
        return s.toString();
    }
}