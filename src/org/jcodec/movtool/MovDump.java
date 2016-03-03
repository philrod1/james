package org.jcodec.movtool;

import static org.jcodec.common.NIOUtils.readableFileChannel;
import static org.jcodec.common.NIOUtils.writableFileChannel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.jcodec.common.SeekableByteChannel;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NodeBox;

public class MovDump {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Syntax: movdump [options] <filename>");
            System.out
                    .println("Options: \n\t-f <filename> save header to a file\n\t-a <atom name> dump only a specific atom\n");
            return;
        }
        int idx = 0;
        File headerFile = null;
        String atom = null;
        while (idx < args.length) {
            if ("-f".equals(args[idx])) {
                ++idx;
                headerFile = new File(args[idx++]);
            } else if ("-a".equals(args[idx])) {
                ++idx;
                atom = args[idx++];
            } else
                break;
        }
        File source = new File(args[idx]);
        if (headerFile != null) {
            dumpHeader(headerFile, source);
        }

        if (atom == null)
            System.out.println(print(source));
        else {
            String dump = print(source, atom);
            if (dump != null)
                System.out.println(dump);
        }
    }

    private static void dumpHeader(File headerFile, File source) throws IOException, FileNotFoundException {
        SeekableByteChannel raf = null;
        SeekableByteChannel daos = null;
        try {
            raf = readableFileChannel(source);
            daos = writableFileChannel(headerFile);

            for (Atom atom : MP4Util.getRootAtoms(raf)) {
                String fourcc = atom.getHeader().getFourcc();
                if ("moov".equals(fourcc) || "ftyp".equals(fourcc)) {
                    atom.copy(raf, daos);
                }
            }
        } finally {
            raf.close();
            daos.close();
        }
    }

    public static String print(File file) throws IOException {
        return MP4Util.parseMovie(file).toString();
    }

    private static Box findDeep(NodeBox root, String atom) {
        for (Box b : root.getBoxes()) {
            if (atom.equalsIgnoreCase(b.getFourcc())) {
                return b;
            } else if (b instanceof NodeBox) {
                Box res = findDeep((NodeBox) b, atom);
                if (res != null)
                    return res;
            }
        }
        return null;
    }

    public static String print(File file, String atom) throws IOException {
        MovieBox mov = MP4Util.parseMovie(file);

        Box found = findDeep(mov, atom);
        if (found == null) {
            System.out.println("Atom " + atom + " not found.");
            return null;
        }

        return found.toString();
    }
}
