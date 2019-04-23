package emulator.machine;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Keyboard implements KeyListener {
	
	public static final int OSD_KEY_ESC         = KeyEvent.VK_ESCAPE;
    public static final int OSD_KEY_1           = KeyEvent.VK_1;
    public static final int OSD_KEY_2           = KeyEvent.VK_2;
    public static final int OSD_KEY_3           = KeyEvent.VK_3;
    public static final int OSD_KEY_4           = KeyEvent.VK_4;
    public static final int OSD_KEY_5           = KeyEvent.VK_5;
    public static final int OSD_KEY_6           = KeyEvent.VK_6;
    public static final int OSD_KEY_7           = KeyEvent.VK_7;
    public static final int OSD_KEY_8           = KeyEvent.VK_8;
    public static final int OSD_KEY_9           = KeyEvent.VK_9;
    public static final int OSD_KEY_0           = KeyEvent.VK_0;
    public static final int OSD_KEY_EQUALS      = KeyEvent.VK_EQUALS;
    public static final int OSD_KEY_TAB         = KeyEvent.VK_TAB;
    public static final int OSD_KEY_Q           = KeyEvent.VK_Q;
    public static final int OSD_KEY_W           = KeyEvent.VK_W;
    public static final int OSD_KEY_E           = KeyEvent.VK_E;
    public static final int OSD_KEY_R           = KeyEvent.VK_R;
    public static final int OSD_KEY_T           = KeyEvent.VK_T;
    public static final int OSD_KEY_Y           = KeyEvent.VK_Y;
    public static final int OSD_KEY_U           = KeyEvent.VK_U;
    public static final int OSD_KEY_I           = KeyEvent.VK_I;
    public static final int OSD_KEY_O           = KeyEvent.VK_O;
    public static final int OSD_KEY_P           = KeyEvent.VK_P;
    public static final int OSD_KEY_ENTER       = KeyEvent.VK_ENTER;
    public static final int OSD_KEY_CONTROL     = KeyEvent.VK_CONTROL;
    public static final int OSD_KEY_A           = KeyEvent.VK_A;
    public static final int OSD_KEY_S           = KeyEvent.VK_S;
    public static final int OSD_KEY_D           = KeyEvent.VK_D;
    public static final int OSD_KEY_F           = KeyEvent.VK_F;
    public static final int OSD_KEY_G           = KeyEvent.VK_G;
    public static final int OSD_KEY_H           = KeyEvent.VK_H;
    public static final int OSD_KEY_J           = KeyEvent.VK_J;
    public static final int OSD_KEY_K           = KeyEvent.VK_K;
    public static final int OSD_KEY_L           = KeyEvent.VK_L;
    public static final int OSD_KEY_LSHIFT      = KeyEvent.VK_SHIFT;
    public static final int OSD_KEY_Z           = KeyEvent.VK_Z;
    public static final int OSD_KEY_X           = KeyEvent.VK_X;
    public static final int OSD_KEY_C           = KeyEvent.VK_C;
    public static final int OSD_KEY_V           = KeyEvent.VK_V;
    public static final int OSD_KEY_B           = KeyEvent.VK_B;
    public static final int OSD_KEY_N           = KeyEvent.VK_N;
    public static final int OSD_KEY_M           = KeyEvent.VK_M;
    public static final int OSD_KEY_ALT         = KeyEvent.VK_ALT;
    public static final int OSD_KEY_SPACE       = KeyEvent.VK_SPACE;
    public static final int OSD_KEY_F1          = KeyEvent.VK_F1;
    public static final int OSD_KEY_F2          = KeyEvent.VK_F2;
    public static final int OSD_KEY_F3          = KeyEvent.VK_F3;
    public static final int OSD_KEY_F4          = KeyEvent.VK_F4;
    public static final int OSD_KEY_F5          = KeyEvent.VK_F5;
    public static final int OSD_KEY_F6          = KeyEvent.VK_F6;
    public static final int OSD_KEY_F7          = KeyEvent.VK_F7;
    public static final int OSD_KEY_F8          = KeyEvent.VK_F8;
    public static final int OSD_KEY_F9          = KeyEvent.VK_F9;
    public static final int OSD_KEY_F10         = KeyEvent.VK_F10;
    public static final int OSD_KEY_UP          = KeyEvent.VK_UP;
    public static final int OSD_KEY_PGUP        = KeyEvent.VK_PAGE_UP;
    public static final int OSD_KEY_MINUS_PAD   = KeyEvent.VK_MINUS;
    public static final int OSD_KEY_LEFT        = KeyEvent.VK_LEFT;
    public static final int OSD_KEY_RIGHT       = KeyEvent.VK_RIGHT;
    public static final int OSD_KEY_PLUS_PAD    = KeyEvent.VK_PLUS;
    public static final int OSD_KEY_DOWN        = KeyEvent.VK_DOWN;
    public static final int OSD_KEY_PGDN        = KeyEvent.VK_PAGE_DOWN;
    public static final int OSD_KEY_F11         = KeyEvent.VK_F11;
    public static final int OSD_KEY_F12         = KeyEvent.VK_F12;
    
    public boolean[] key = new boolean[255];
    private final FullMachine machine;
    
    public Keyboard(FullMachine machine) {
		this.machine = machine;
	}
    
    protected void poll() {
    	/*
		 * P for pause
		 */
		if (isKeyPressed(OSD_KEY_P)) {
			int p = machine.memoryRead(0x4e04);
			if(p == 2) {
				machine.memoryWrite(0x4e04, 3);
			} else {
				machine.memoryWrite(0x4e04, 2);
			}
		}
		
		/*
		 * Ctrl-Fn for save snapshot
		 */
		if(isKeyPressed(OSD_KEY_CONTROL)) {
			for(int key = OSD_KEY_F1 ; key <= OSD_KEY_F12 ; key++) {
				if (isKeyPressed(key)) {
					machine.saveSnapshot(key);
				}
			}
		}
		
		/*
		 * Fn for load saved snapshot
		 */
		for(int i = OSD_KEY_F1 ; i <= OSD_KEY_F12 ; i++) {
			if (isKeyPressed(i)) {
				machine.revertToSnapshot(i);
			}
		}

		/*
		 * Space to skip level
		 */
		if(isKeyPressed(OSD_KEY_SPACE)) {
			System.out.println("SKIP");
			machine.memoryWrite(0x4e04, 0xc);
		}
		
		/*
		 * Standard human controls
		 */
		machine.portWrite(0, (char) 0xFF);
		machine.portWrite(1, (char) 0xFF);
		
		if (isKeyPressed(OSD_KEY_3)) {
			machine.portWrite(0, (char) (machine.portRead(0) & 0xDF));
		}
		else
		if (isKeyPressed(OSD_KEY_UP)  || isKeyPressed(OSD_KEY_W)) {
			System.out.println("UP");
			machine.portWrite(0, (char) (machine.portRead(0) & 0xFE));
		}
		else
		if (isKeyPressed(OSD_KEY_LEFT)  || isKeyPressed(OSD_KEY_A)) {
			machine.portWrite(0, (char) (machine.portRead(0) & 0xFD));
		}
		else
		if (isKeyPressed(OSD_KEY_RIGHT)  || isKeyPressed(OSD_KEY_D)) {
			machine.portWrite(0, (char) (machine.portRead(0) & 0xFB));
		}
		else
		if (isKeyPressed(OSD_KEY_DOWN)  || isKeyPressed(OSD_KEY_S)) {
			machine.portWrite(0, (char) (machine.portRead(0) & 0xF7));
		}
		else
		if (isKeyPressed(OSD_KEY_1)) {
			machine.portWrite(1, (char) (machine.portRead(1) & 0xDF));
		}
    }

	public boolean isKeyPressed(int keycode) {
		 return key[keycode];
	}
    
    @Override
	public void keyPressed(KeyEvent e) {
		key[e.getKeyCode()] = true;
		e.consume();
	}
	
	@Override
	public void keyReleased(KeyEvent e) {
		key[e.getKeyCode()] = false;
		e.consume();
	}
	
	@Override
	public void keyTyped(KeyEvent e) {}
}
