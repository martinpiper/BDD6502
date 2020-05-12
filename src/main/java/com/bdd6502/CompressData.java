package com.bdd6502;

import de.quippy.javamod.multimedia.mod.ModMixer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class CompressData {
    public static void compressMusicData(String filename) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(filename + ModMixer.EVENTS_BIN));
        int [] byteFrequency = new int[256];
        // Calculate an escape byte
        for (int i = 0 ; i < bytes.length ; i++) {
            byteFrequency[Byte.toUnsignedInt(bytes[i])]++;
        }
        int minCount = byteFrequency[0];
        byte escapeByte = 0;
        for (int i = 0 ; i < byteFrequency.length ; i++) {
            if (byteFrequency[i] < minCount) {
                escapeByte = (byte) i;
                minCount = byteFrequency[i];
            }
        }

        int originalLength = bytes.length;

        System.out.println("processing input length=" + bytes.length);
        for (int i = 0; i < bytes.length; ) {
            int bestLen = 0;
            int bestPos = 0;
            if (i > 0) {
                for (int j = 0; j < i && bestLen < 255; j++) {
                    int len = 0;
                    while ((len < 255) && ((i + len) < bytes.length) && ((j + len) < i) && (bytes[j + len] == bytes[i + len])) {
                        len++;
                    }
                    if (len > bestLen) {
                        bestLen = len;
                        bestPos = j;
                    }
                }
            }
            if (bytes[i] == escapeByte) {
                System.out.println("escape byte at " + i);
                // Handle a run of escape bytes?
                int nextBytesPos = i;
                int runLength = 0;
                while ((runLength < 255) && (nextBytesPos < bytes.length) && (bytes[nextBytesPos] == escapeByte)) {
                    nextBytesPos++;
                    runLength++;
                }
                int afterLength = bytes.length - nextBytesPos;
                byte[] newBytes = new byte[i + 3 + afterLength];
                System.arraycopy(bytes,0,newBytes,0,i);
                newBytes[i++] = escapeByte;
                newBytes[i++] = 0;  // 0 reserved for output the escape byte
                newBytes[i++] = (byte)runLength;
                if (runLength > 1) {
                    runLength = runLength;
                }
                System.arraycopy(bytes,nextBytesPos,newBytes,i, afterLength);
                bytes = newBytes;
                continue;
            }
            // If there is a good pattern match that will save data, then...
            if (bestLen > 6) {
//				System.out.println("for " + i + " found bestLen=" + bestLen + " bestPos=" + bestPos);
                int nextBytesPos = i + bestLen;
                int afterLength = bytes.length - nextBytesPos;
                byte[] newBytes = new byte[i + 4 + afterLength];
                System.arraycopy(bytes,0,newBytes,0,i);
                newBytes[i++] = escapeByte;
                newBytes[i++] = (byte)bestLen;  // 0 reserved for output the escape byte
                newBytes[i++] = (byte)bestPos;
                newBytes[i++] = (byte)(bestPos>>8);
                System.arraycopy(bytes,nextBytesPos,newBytes,i, afterLength);
                bytes = newBytes;
                continue;
            }
            i++;
        }

        System.out.println("saving=" + (originalLength - bytes.length));
        byte[] headerBytes = new byte[4];
        headerBytes[0] = (byte)originalLength;
        headerBytes[1] = (byte)(originalLength >> 8);
        headerBytes[2] = (byte)(originalLength >> 16);
        headerBytes[3] = escapeByte;

        Files.write(Paths.get(filename + ModMixer.EVENTS_CMP),headerBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        Files.write(Paths.get(filename + ModMixer.EVENTS_CMP),bytes, StandardOpenOption.APPEND);
    }
}
