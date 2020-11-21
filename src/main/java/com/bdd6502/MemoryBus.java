package com.bdd6502;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public abstract class MemoryBus {
    protected int busContention = 0;

    static boolean addressActive(int addressEx, int selector) {
        if ((addressEx & selector) > 0) {
            return true;
        }
        return false;
    }

    public void ageContention() {
        if (busContention > 0) {
            busContention--;
        }
    }

    public void writeDataFromFile(int address, int addressEx, String filename) throws IOException {
        byte[] data = FileUtils.readFileToByteArray(new File(filename));
        for (int i = 0; i < data.length; i++) {
            writeData(address + i, addressEx, data[i]);
        }
    }

    public void writeDataFromFile(int address, int addressEx, String filename, int offset, int length) throws IOException {
        byte[] data = FileUtils.readFileToByteArray(new File(filename));
        for (int i = 0; i < length; i++) {
            writeData(address + i, addressEx, data[offset + i]);
        }
    }

    public void writeData(int address, int addressEx, int data) {
        writeData(address, addressEx, (byte) data);
    }

    abstract void writeData(int address, int addressEx, byte data);

    int sequentialValue = 0;
    protected int getByteOrContention(int value) {
        sequentialValue++;
        if (busContention > 0) {
            return value ^ sequentialValue;
        }
        return value;
    }

    public boolean extEXTWANTIRQ() {
        return false;
    }

    public void resetExtEXTWANTIRQ() {
    }
}
