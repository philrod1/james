package ai;

import ai.common.Game;
import ai.common.MOVE;

public abstract class AbstractAI implements AI {
	
	protected Game game;
	private boolean running = true;

	@Override
	public void run() {
		System.out.println("Running AI");
		while(running) {
			switch(game.getState()) {
			case PLAYING:
				if(game.pacman.isAlive()) {
					while(!game.getMaze().isValidTile(game.pacman.getTilePosition())) {
						try {
							Thread.sleep(2);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					MOVE move = play();
					if(move != null)
						game.makeMove(move);
				} else {
					reset();
				}
				break;
			case LEVEL_COMPLETE:
				reset();
				break;
			case DEMO:
			case UNKNOWN:
			case KILLED:
			case COIN_IN:
			case START_PRESSED:
			case GAME_OVER:
				reset();
				game.player1Start();
				break;
			default:
				try { Thread.sleep(5); }
				catch (InterruptedException e) { running = false; }
			}
		}
		System.out.println("HERE");
	}
	
	protected abstract void reset();
	
	protected abstract MOVE play();

}
