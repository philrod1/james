package emulator.video;

import javax.swing.JPanel;


public interface Screen {
	JPanel getPanel();
	void blit();
	void setPixel(int x, int y, int i);
	void saveScreenShot();
}
