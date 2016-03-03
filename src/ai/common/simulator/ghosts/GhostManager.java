package ai.common.simulator.ghosts;

import ai.common.simulator.data.SimData;

public class GhostManager {

	private boolean globalMode = false;
	private int framesSincePillEaten = 0;
	private int globalPillCount = 0;
	private final int[] globalPillLimits = new int[] { 0,  7, 17, 32 };
	private final int[] ghostPillCounts  = new int[] { 0,  0,  0,  0 };
	private final Ghost[] ghosts;
	protected boolean random = true;

	public GhostManager(Ghost[] ghosts) {
		this.ghosts = ghosts;
	}
	
	public void reset(boolean globalMode, int framesSinceLastPillEaten, 
			int globalPillCount, int[] ghostPillCounts, boolean random) {
		this.random = random;
		this.globalMode = globalMode;
		this.framesSincePillEaten = framesSinceLastPillEaten;
		this.globalPillCount = globalPillCount;
		for(int i = 1 ; i < 4 ; i++) {
			this.ghostPillCounts[i] = ghostPillCounts[i];
		}
	}

	private int releaseTimeout(int level) {
		return level < 5 ? 240 : 180;
	}

	public void setGlobalMode(boolean globalMode) {
		this.globalMode = globalMode;
	}

	public void pillEaten() {
		framesSincePillEaten = 0;
		if (globalMode) {
			globalPillCount++;
		} else {
			for (int i = 1; i < 4; i++) {
				if (ghosts[i].getState() == 3) {
					ghostPillCounts[i]++;
					break;
				}
			}
		}
	}

	public void update(int level) {
		if (globalMode) {
			if (globalPillCount >= globalPillLimits[1]
					&& ghosts[1].getState() == 3) {
//				System.out.println("Releasing Pinky");
				ghosts[1].leaveHome();
				return;
			} else if (globalPillCount >= globalPillLimits[2]
					&& ghosts[2].getState() == 3) {
//				System.out.println("Releasing Inky");
				ghosts[2].leaveHome();
				return;
			} else if (globalPillCount >= globalPillLimits[3]
					&& ghosts[3].getState() == 3) {
//				System.out.println("Releasing Sue");
				globalPillCount = 0;
				globalMode = false;
				ghosts[3].leaveHome();
				return;
			}
		} else {
			for (int i = 1; i < 4; i++) {
				Ghost ghost = ghosts[i];
				if (ghost.getState() == 3) {
					if (ghostPillCounts[i] >= ghost
							.getPersonalPillReleaseCount(level)) {
						ghost.leaveHome();
						return;
					}
					break;
				}
			}
		}
		if (framesSincePillEaten > releaseTimeout(level)) {
			framesSincePillEaten = 0;
			for (int i = 1; i < 4; i++) {
				Ghost ghost = ghosts[i];
				if (ghost.getState() == 3) {
					ghost.leaveHome();
					break;
				}
			}
		} else {
			framesSincePillEaten++;
		}
	}

	public void sync(int level, char[] RAM) {
		//TODO - The real machine wraps back to 0 - should we?
		framesSincePillEaten = ((RAM[0x4d98] * 256) + RAM[0x4d97]);
		globalPillCount = RAM[0x4d9f];
		
		ghostPillCounts[0] = 0;
		ghostPillCounts[1] = RAM[0x4e0f];
		ghostPillCounts[2] = RAM[0x4e10];
		ghostPillCounts[3] = RAM[0x4e11];
		
		globalMode = RAM[0x4e12] == 1;
		
		random = RAM[0x4dc1] == 0;
		
		for(int i = 0 ; i < ghosts.length ; i++) {
			Ghost ghost = ghosts[i];
			ghost.updatePatterns(RAM);
		}
	}

	public void sync(SimData data) {
		framesSincePillEaten = data.framesSincePillEaten;
		globalPillCount = data.globalPillCount;
		
		ghostPillCounts[0] = 0;
		ghostPillCounts[1] = data.pinky.pillCount;
		ghostPillCounts[2] = data.inky.pillCount;
		ghostPillCounts[3] = data.sue.pillCount;
		
		globalMode = data.globalMode;

		ghosts[0].updatePatterns(data.blinky.getPatterns());
		ghosts[1].updatePatterns(data.pinky.getPatterns());
		ghosts[2].updatePatterns(data.inky.getPatterns());
		ghosts[3].updatePatterns(data.sue.getPatterns());
		random = data.random;
	}
	
	public int getPillCount(int gid) {
		return ghostPillCounts[gid];
	}

	public int getFramesSincePillEaten() {
		return framesSincePillEaten;
	}

	public int getGlobalPillCount() {
		return globalPillCount;
	}

	public boolean isGlobalMode() {
		return globalMode;
	}

	public boolean areGhostsRandom() {
		return random;
	}

}
