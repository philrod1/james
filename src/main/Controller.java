package main;

import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;

import ai.AI;
import ai.common.Game;
import ai.ensemble.EnsembleAI;
import ai.mcts.MCTSPlayer;
import emulator.games.Pacman;
import emulator.machine.FullMachine;

public class Controller {

	private final double gameSpeed = 1.0;
	
	private final int width = 224 * 2, height = 288 * 2;

	public static void main(String[] args) {
		new Controller();
	}

	FullMachine machine;
	JFrame frame;
	Game game;
	int count = 0;

	public Controller() {
		machine = new FullMachine(new Pacman());
		frame = new JFrame("Ms. Pac-Man");
		frame.setBounds(1, 1, width, height);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(machine.getScreen());
//		frame.addKeyListener(machine.getKeyboard());
		frame.pack();
		frame.setVisible(true);
		
		game = new Game(machine);

		final AI ai = new EnsembleAI(game);

		final TimerTask task = new TimerTask() {	
			@Override
			public void run() {
				game.update();
				if (game.getState() == Game.STATE.LEVEL_COMPLETE) {
					game.log("1 1");
					machine = new FullMachine(new Pacman());
					frame.getContentPane().removeAll();
					frame.getContentPane().add(machine.getScreen());
					frame.pack();
					game.setMachine(machine);
					if (++count == 100) {
						System.exit(1);
					}
					return;
				} else if (game.getState() == Game.STATE.PLAYING && machine.memoryRead(0x4da5) > 0) {
					game.log("1 0");
					machine = new FullMachine(new Pacman());
					frame.getContentPane().removeAll();
					frame.getContentPane().add(machine.getScreen());
					frame.pack();
					game.setMachine(machine);
					if (++count == 100) {
						System.exit(1);
					}
					return;
				}
				machine.step();
			}
		};

		final Timer timer = new Timer();
		timer.scheduleAtFixedRate(task, 0, (int)(1000/machine.getFPS() * 1/gameSpeed ));
		
		final Thread aiThread = new Thread(ai);
		aiThread.start();
		
	}
	
}
