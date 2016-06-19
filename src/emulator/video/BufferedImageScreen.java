package emulator.video;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;


public class BufferedImageScreen extends JPanel implements Screen {
	private static final long serialVersionUID = -7316069649903725892L;
	private static final boolean SMOOTH_RESIZE = false;
	private final BufferedImage[] bi;
	private final double ratio;
	private int biIndex = 0;

	public BufferedImageScreen (int width, int height) {
		bi = new BufferedImage[]{new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB),
								 new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)};
		ratio = (double)width / (double)height;
		setPreferredSize(new Dimension(width*2, height*2));
		setBackground(Color.BLACK);
	}

	@Override
	public void blit() {
		biIndex = (biIndex + 1) % 2;
		repaint();
	}
	
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		if(SMOOTH_RESIZE) {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		}
		int w, h, osx, osy;
		if(getHeight() * ratio < getWidth()) {
			h = getHeight();
			w = (int)(h * ratio);
			osy = 0;
			osx = (getWidth() - w) / 2;
		} else {
			w = getWidth();
			h = (int)(w / ratio);
			osx = 0;
			osy = (getHeight() - h) / 2;
		}
		g2.drawImage(bi[(biIndex+1)%2], osx, osy, w, h, null);
		g2.dispose();
		g.dispose();
	}

	@Override
	public JPanel getPanel() {
		return this;
	}

	@Override
	public void setPixel(int x, int y, int rgb) {
		bi[biIndex].setRGB(x, y, rgb);
	}

	@Override
	public void saveScreenShot() {
		int time = (int) (System.currentTimeMillis() / 60000L);
		File outputImage = new File("res/img"+time+".png");
		try {
			ImageIO.write(bi[0], "png", outputImage);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
