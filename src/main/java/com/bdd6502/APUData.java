package com.bdd6502;

import javax.sound.sampled.*;
import java.io.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

// Any comment with "HW:" draws attention to hardware design considerations
public class APUData extends MemoryBus {
    int addressRegisters = 0x2000, addressExRegisters = 0x02;
    int addressInstructions = 0x8000;
    int addressData = 0x4000;

    public byte[] getApuInstructions() {
        return apuInstructions;
    }

    public byte[] getApuData() {
        return apuData;
    }

    public byte[] getApuRegisters() {
        return apuRegisters;
    }

    byte apuInstructions[] = new byte[0x2000];
    byte apuData[] = new byte[0x2000];
    byte apuRegisters[] = new byte[0x2000];


    public APUData() {
    }

    @Override
    void writeData(int address, int addressEx, byte data) {
        // Some contention here as this uses banks of RAM
        if (MemoryBus.addressActive(addressEx, addressExRegisters)) {
            if (MemoryBus.addressActive(address, addressRegisters)) {
                apuRegisters[address & 0x1fff] = data;
            }
            if (MemoryBus.addressActive(address, addressInstructions)) {
                busContention = 8;
                apuInstructions[address & 0x1fff] = data;
            }
            if (MemoryBus.addressActive(address, addressData)) {
                busContention = 8;
                apuData[address & 0x1fff] = data;
            }
        }
    }

    @Override
    public void setAddressBus(int address, int addressEx) {

    }
}
