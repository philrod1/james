package ai.common;

import java.awt.Point;

	public enum MOVE {
		UP 		{ public MOVE opposite(){return MOVE.DOWN;		 }
				  public int portValue(){ return 254;            }
				  public Point delta()  { return new Point(0,-1);}},	
		LEFT 	{ public MOVE opposite(){return MOVE.RIGHT;	     }
			  	  public int portValue(){ return 253;            }
				  public Point delta()  { return new Point(-1,0);}},	
		DOWN 	{ public MOVE opposite(){return MOVE.UP;		 }
		  		  public int portValue(){ return 247;            }
				  public Point delta()  { return new Point(0,1); }},		
		RIGHT 	{ public MOVE opposite(){return MOVE.LEFT;		 }
				  public int portValue(){ return 251;            }
				  public Point delta()  { return new Point(1,0); }};
		public abstract MOVE opposite();
		public abstract int portValue();
		public abstract Point delta();
		public static MOVE decodeLastMove(int x, int y) {
			int value = (x << 8) | y;
			switch (value) {
			case 0x0100: return LEFT;
			case 0xff00: return RIGHT;
			case 0x00ff: return UP;
			case 0x0001: return DOWN;
			default: return null;
			}
		}
	}
	
	
	

