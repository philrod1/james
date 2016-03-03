package ai.common.simulator.ghosts;

import java.awt.Point;

import ai.common.MOVE;
import ai.common.simulator.SimGame;

public interface Ghost {
	void setCruiseLevel(int cruiseLevel);
	void update(SimGame game);
	void setFrightened(boolean frightened);
	int getPersonalPillReleaseCount(int level);
	int getState();
	void setState(int state);
	void leaveHome();
	Point getTile();
	Point getStartPosition();
	void reverse();
	void setPreviousOrientation(int moveVal);
	void setCurrentOrientation(int moveVal);
	void setPixelPosition(Point position);
	void chomp(int i);
	void setSlow(boolean slow);
	Point getPosition();
	void incFrame();
	boolean isFrightened();
	void updatePatterns(char[] RAM);
	MOVE getOrientation();
	int getID();
	int[] getData(int pillCount);
	void updatePatterns(int[] patterns);
	String getCurrentPattern();
	int getCruiseLevel();
}
