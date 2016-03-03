package emulator.arcadeflex;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import emulator.memory.CharPtr;

public class FileStuff {
	public RandomAccessFile raf;
	public FileOutputStream fos;
	public FileWriter fw;
	public InputStream is;
	public String Name;

	public static FileStuff fopen(char[] name, String format) {
		String nameS = new String(name);
		return fopen(nameS, format);
	}

	public static FileStuff fopen(String name, String format) {
		FileStuff file;
		// mame.mame.dlprogress.setFileName("fetching file: "+name);
		file = new FileStuff();
		if (format.compareTo("rb") == 0) {
			try {
				file.raf = new RandomAccessFile(name, "r");
				file.Name = name;
			} catch (Exception e) {

				file = null;
				return null;
			}
			return file;
		} else if (format.compareTo("wb") == 0) {
			try {
				file.fos = new FileOutputStream(name, false);

			} catch (Exception e) {
				file = null;
				return null;
			}
			return file;
		} else if (format.compareTo("wa") == 0) {
			try {
				file.fw = new FileWriter(name, false);
			} catch (Exception e) {
				file = null;
				return null;
			}
			return file;
		}
		file = null;
		return null;
	}
	
	public void close() {
		try {
			raf.close();
			is.close();
			fos.close();
			fw.close();
		} catch (Exception e) {
		}
	}
	
	public static int fread(char[] buf, int offset, int size, int count,	FileStuff file) {
		byte bbuf[] = new byte[size * count];
		int readsize;

		try {
			readsize = file.raf.read(bbuf);
//			System.out.println("FileStuff.fread() - Readsize = " + readsize);
		} catch (Exception e) {
			e.printStackTrace();
			bbuf = null;
			return -1;
		}

		for (int i = 0; i < readsize; i++) {
			buf[offset + i] = (char) ((bbuf[i] + 256) & 0xFF);
		}
		bbuf = null;
		return readsize;
	}

	public static int fread(char[] buf, int size, int count, FileStuff file) {
		return fread(buf, 0, size, count, file);
	}

	public static void fseek(FileStuff file, int pos) {
		if (file.raf != null) {
			try {
				file.raf.seek(pos);
			} catch (IOException ex) {
				file = null;
			}

		}
	}

	public static void fwrite(char[] buf, int offset, int size, int count,
			FileStuff file) {
		byte bbuf[] = new byte[size * count];

		for (int i = 0; i < size * count; i++) {
			bbuf[i] = (byte) (buf[offset + i] & 0xFF);
		}
		try {
			file.fos.write(bbuf);
		} catch (Exception e) {
			bbuf = null;
			return;
		}

		bbuf = null;
	}

	public static void fwrite(char[] buf, int size, int count, FileStuff file) {
		fwrite(buf, 0, size, count, file);
	}

	public static void fwrite(char buf, int size, int count, FileStuff file) {
		byte bbuf[] = new byte[size * count];

		bbuf[0] = (byte) (buf & 0xFF);
		try {
			file.fos.write(bbuf);
		} catch (Exception e) {
			bbuf = null;
			return;
		}

		bbuf = null;
	}
	
	public static char[][] readroms(RomModule[] romp, String basename, int maxMemoryRegions) {
		int region;
		char[][] memoryRegions = new char[maxMemoryRegions][];

		for (region = 0; region < memoryRegions.length; region++) {
			memoryRegions[region] = null;
		}

		region = 0;
		int _ptr = 0;

		while (romp[_ptr].name != null || romp[_ptr].offset != 0
				|| romp[_ptr].length != 0) {
			int region_size;
			String name;

			if (romp[_ptr].name != null || romp[_ptr].length != 0) {
				System.out.println(
						"Error in RomModule definition: expecting ROM_REGION");
				return null;
			}

			region_size = romp[_ptr].offset;
			if ((memoryRegions[region] = new char[region_size]) == null) {
				System.out.println("Unable to allocate " + region_size
						+ " bytes of RAM");
				return null;
			}

			memset(memoryRegions[region], 0, region_size);

			_ptr++;

			while (romp[_ptr].length != 0) {
				FileStuff f;

				if (romp[_ptr].name == null) {
					System.out.println(
							"Error in RomModule definition: ROM_CONTINUE not preceded by ROM_LOAD");
					return null;
				}
				if (romp[_ptr].name.compareTo("-1") == 0) {
					System.out.println(
							"Error in RomModule definition: ROM_RELOAD not preceded by ROM_LOAD");
					return null;
				}
				name = basename + "/" + romp[_ptr].name;
				name = name.trim();
				f = FileStuff.fopen(name, "rb");
				do {
					CharPtr c;
					int i;
					int length = romp[_ptr].length & ~0x80000000;
					if ((romp[_ptr].name != null)
							&& (romp[_ptr].name.compareTo("-1") == 0)) {
						FileStuff.fseek(f, 0);
					}
					if (romp[_ptr].offset + length > region_size) {
						System.out.println("Error in RomModule definition: "
								+ name + " out of memory region space");
						f.close();
						return null;
					}
					if ((romp[_ptr].length & 0x80000000) != 0) {
						char[] temp;
						temp = new char[length];

						if (FileStuff.fread(temp, 0, 1, length, f) != length) {
							System.out.println("Unable to read ROM " + name);
							temp = null;
							f.close();
						}

						/* copy the ROM data and calculate the checksum */
						// c = Machine.memory_region[region] +
						// romp[_ptr].offset;
						c = new CharPtr(memoryRegions[region],
								romp[_ptr].offset);
						for (i = 0; i < length; i += 2) {
							c.write(i * 2, temp[i]);
							c.write(i * 2 + 2, temp[i + 1]);
						}

						temp = null;
					} else {
						if (FileStuff.fread(memoryRegions[region],
								romp[_ptr].offset, 1, length, f) != length) {
							System.out.println("Unable to read ROM - " + name);
							f.close();
						}
					}
					_ptr++;
				} while (romp[_ptr].length != 0
						&& (romp[_ptr].name == null || romp[_ptr].name
								.compareTo("-1") == 0));
				f.close();
			}

			region++;
		}

		return memoryRegions;
	}

	public static void memset(char[] buf, int value, int size) {
		for (int mem = 0; mem < size; mem++) {
			buf[mem] = (char) value;
		}
	}
}