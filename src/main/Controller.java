package main;

import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.*;

import ai.AI;
import ai.common.Game;
import ai.ensemble.EnsembleAI;
import ai.mcts.MCTSPlayer;
import emulator.games.Pacman;
import emulator.machine.FullMachine;

public class Controller {

	public static void main(String[] args) {
		new Controller();
	}

	public Controller() {
		double gameSpeed = 1.0;
		int width = 224 * 2, height = 288 * 2;
		final FullMachine machine = new FullMachine(new Pacman());
		final JFrame frame = new JFrame("Ms. Pac-Man");
		frame.setBounds(1, 1, width, height);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.getContentPane().add(machine.getScreen());
		frame.addKeyListener(machine.getKeyboard());
		frame.pack();
		frame.setVisible(true);

		boolean invincible = false;
		boolean turbo = false;

		final Game game = new Game(machine, invincible, turbo);

		final AI ai = new EnsembleAI(game, turbo);
//		final AI ai = new MCTSPlayer(game, turbo);

		final TimerTask task = new TimerTask() {
			@Override
			public void run() {
				game.update();
				machine.step();
			}
		};

		final Timer timer = new Timer();
		timer.scheduleAtFixedRate(task, 0, (int)(1000/machine.getFPS() * 1/gameSpeed ));
		
		final Thread aiThread = new Thread(ai);
		aiThread.start();
		
	}


	
}
