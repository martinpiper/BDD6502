package com.bdd6502;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public abstract class MemoryBus {
    public void setBusContention(int busContention) {
        this.busContention = busContention;
    }

    protected int busContention = 0;
    protected boolean memoryAsserted = false;

    static boolean addressLower8KActive(int addressEx) {
        return addressEx < 0x2000;
    }

    public static boolean addressActive(int addressEx, int selector) {
        if ((addressEx & selector) > 0) {
            return true;
        }
        return false;
    }

    public static boolean addressActive(long addressEx, long selector) {
        if ((addressEx & selector) > 0) {
            return true;
        }
        return false;
    }

    public void ageContention() {
        if (memoryAsserted) {
            return;
        }
        if (busContention > 0) {
            busContention--;
        }
    }

    public boolean hasContention() {
        if (memoryAsserted) {
            return true;
        }
        if (busContention > 0) {
            return true;
        }
        return false;
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

    abstract public void writeData(int address, int addressEx, byte data);

    abstract public void setAddressBus(int address, int addressEx);

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

    abstract public void randomiseData(Random rand);

    static void randomiseHelper(Random rand , byte[] values) {
        if (values == null) {
            return;
        }
        for (int i = 0 ; i < values.length ; i++) {
            values[i] = (byte) rand.nextInt();
        }
    }

    static void randomiseHelper(Random rand , int[] values) {
        if (values == null) {
            return;
        }
        for (int i = 0 ; i < values.length ; i++) {
            values[i] = rand.nextInt();
        }
    }

    static void randomiseHelper(Random rand , boolean[] values) {
        if (values == null) {
            return;
        }
        for (int i = 0 ; i < values.length ; i++) {
            values[i] = rand.nextBoolean();
        }
    }
}
