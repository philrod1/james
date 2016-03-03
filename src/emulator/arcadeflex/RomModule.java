package emulator.arcadeflex;

public class RomModule {
	
	public String name;	/* name of the file to load */
	public int offset;	/* offset to load it to */
	public int length;	/* length of the file */
	public int crc;
            
	public RomModule(String name, int offset, int length) { 
		this.name = name;
		this.offset = offset;
		this.length = length;
	}
	
	public RomModule(String name, int offset, int length, int crc) { 
		this.name = name;
		this.offset = offset;
		this.length = length;
		this.crc = crc;
	}
};