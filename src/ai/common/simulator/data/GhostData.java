package ai.common.simulator.data;


public class GhostData {
	
	public final int px, py, state;
	public final int previousOrientation;
	public final int currentOrientation;
	public final boolean frightened;
	public final int pillCount;
	public final int normal, scared, slow, elroy1, elroy2;
	public final int cruiseLevel;
	public final int ghost;
	
	public GhostData(char[] RAM, int ghost) {
		this.ghost = ghost;
		px = 256-RAM[0x0d01 + ghost * 2];
		py = RAM[0x0d00 + ghost * 2];
		previousOrientation = RAM[0x0d28 + ghost];
		currentOrientation  = RAM[0x0d2c + ghost];
		state = getGhostState(RAM, ghost);
		frightened = RAM[0x0da7 + ghost] != 0;
		switch(ghost) {
		case 0: pillCount = 0; break;
		case 1: pillCount = RAM[0x0e0f]; break;
		case 2: pillCount = RAM[0x0e10]; break;
		case 3: pillCount = RAM[0x0e11]; break;
		default: pillCount = 0;
		}
		int base = 0x4D56 + ghost * 12;
		normal = (RAM[base+1] << 24) | (RAM[base]   << 16) | (RAM[base+3]  << 8) | (RAM[base+2]);
		scared = (RAM[base+5] << 24) | (RAM[base+4] << 16) | (RAM[base+7]  << 8) | (RAM[base+6]);
		slow   = (RAM[base+9] << 24) | (RAM[base+8] << 16) | (RAM[base+11] << 8) | (RAM[base+10]);
		if(ghost==0) {
			cruiseLevel = RAM[0x0db6] + RAM[0x0db7];
			base = 0x4D52;
			elroy1 = (RAM[base+1] << 24) | (RAM[base] << 16) | (RAM[base+3] << 8) | (RAM[base+2]);
			base = 0x4D4E;
			elroy2 = (RAM[base+1] << 24) | (RAM[base] << 16) | (RAM[base+3] << 8) | (RAM[base+2]);
		} else {
			cruiseLevel = 0;
			elroy1 = 0;
			elroy2 = 0;
		}
	}
	
	public GhostData(int[] data, int ghost) {
		this.ghost = ghost;
		if(ghost == 0) {
			pillCount = 0;
			cruiseLevel = data[0] & 255;
		} else {
			pillCount = data[0] & 255;
			cruiseLevel = 0;
		}
		frightened = 			((data[0] >> 8)  & 1) == 1;
		currentOrientation = 	( data[0] >> 9)  & 3;
		previousOrientation = 	( data[0] >> 11) & 3;
		state = 				( data[0] >> 13) & 7;
		py = 					( data[0] >> 16) & 255;
		px = 					( data[0] >> 24) & 255;
		
		this.normal = data[1];
		this.scared = data[2];
		this.slow   = data[3];
		this.elroy1 = data[4];
		this.elroy2 = data[5];
	}
	
	public GhostData(int px, int py, int state, int previousOrientation,
			int currentOrientation, boolean frightened, int pillCount, int normal, 
			int scared, int slow, int elroy1, int elroy2, int cruiseLevel, int ghost) {
		this.px = px;
		this.py = py;
		this.state = state;
		this.previousOrientation = previousOrientation;
		this.currentOrientation = currentOrientation;
		this.frightened = frightened;
		this.pillCount = pillCount;
		this.normal = normal;
		this.scared = scared;
		this.slow = slow;
		this.elroy1 = elroy1;
		this.elroy2 = elroy2;
		this.cruiseLevel = cruiseLevel;
		this.ghost = ghost;
	}

	private int toTinyData() {
		int data = px;
		data <<= 8;
		data |= py;
		data <<= 3;
		data |= state;
		data <<= 2;
		data |= previousOrientation;
		data <<= 2;
		data |= currentOrientation;
		data <<= 1;
		data |= frightened ? 1 : 0;
		data <<=8;
		data |= (ghost == 0) ? cruiseLevel : pillCount;
		return data;
	}
	
	public int[] getDataArray() {
		return new int[] {
				toTinyData(),
				normal,
				scared,
				slow,
				elroy1,
				elroy2,
		};
	}
	
	public GhostData copy() {
		return new GhostData(
				px, py, state, previousOrientation, currentOrientation, frightened,
				pillCount, normal, scared, slow, elroy1, elroy2, cruiseLevel, ghost
		);
	}
	
	private int getGhostState(char[] RAM, int ghost) {
		int state = RAM[0x0dac + ghost];
		if(state == 0) {
			return 3 + RAM[0x0da0 + ghost];
		}
		return state - 1;
	}

	public int[] getPatterns() {
		return new int[]{normal, scared, slow, elroy1, elroy2};
	}
}
