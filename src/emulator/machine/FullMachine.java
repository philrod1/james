package emulator.machine;

import java.awt.event.KeyListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import javax.swing.JPanel;

import emulator.games.Pacman;
import emulator.video.GenericVideo;

public class FullMachine extends PartMachine {

	private final Keyboard keyboard;
	private final GenericVideo video;
	
	public FullMachine(Pacman pacman) {
		super(pacman);
		video = pacman.getVideo();
		keyboard = new Keyboard(this);
	}
	
	@Override
	public void step() {
		super.step();
		keyboard.poll();
		video.screenRefresh();
		video.updateDisplay();
	}

	public KeyListener getKeyboard() {
		return keyboard;
	}

	public JPanel getScreen() {
		return video.getScreen().getPanel();
	}

	public int getFPS() {
		return fps;
	}
	
	public  void revertToSnapshot(int i) {
		try {
			InputStream file = new FileInputStream("res/snap" + i + ".dat");
			BufferedInputStream buffer = new BufferedInputStream(file);
			ObjectInputStream input = new ObjectInputStream(buffer);
			super.syncToSnapshot((Snapshot) input.readObject());
			input.close();
			file.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void saveSnapshot(int i) {
		try {
			OutputStream file = new FileOutputStream(new File("res/snap" + i + ".dat"));
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			output.writeObject(snapshot);
			output.close();
			System.out.println("Saved");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
