package main;

import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.*;

import ai.AI;
import ai.common.Game;
import ai.ensemble.EnsembleAI;
import ai.mcts.MCTSPlayer;
import ai.rhea.RHEA;
import emulator.games.Pacman;
import emulator.machine.FullMachine;

public class Controller {
	static GraphicsDevice device = GraphicsEnvironment
			.getLocalGraphicsEnvironment().getScreenDevices()[0];

	private final double gameSpeed = 1.0;

	private final int width = 458, height = 606;

	public static void main(String[] args) {
		new Controller();
	}

	public Controller() {
		final JFrame frame = new JFrame("Ms. Pac-Man");
		final FullMachine machine = new FullMachine(new Pacman(), device, frame);

		SwingUtilities.invokeLater(() -> {
			frame.setBounds(1, 1, width, height);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.getContentPane().add(machine.getScreen());
			frame.addKeyListener(machine.getKeyboard());
			frame.pack();
//			device.setFullScreenWindow(frame);
			frame.setResizable(true);
			frame.setVisible(true);
		});

		final Game game = new Game(machine);
//		machine.revertToSnapshot(112);

//		final AI ai = new RHEA(game);
		final AI ai = new EnsembleAI(game);
//		final AI ai = new MCTSPlayer(game);

		final Runnable task = () -> {
			game.update();
			machine.step();
		};

		ScheduledExecutorService ses = Executors.newScheduledThreadPool(2);
		ses.scheduleAtFixedRate(task, 0, (int)((1000 / machine.getFPS()) /gameSpeed), TimeUnit.MILLISECONDS);

		ses.schedule(ai,1000, TimeUnit.MILLISECONDS);

	}


	
}
