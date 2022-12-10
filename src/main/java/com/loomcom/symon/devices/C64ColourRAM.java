package com.loomcom.symon.devices;


import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;
import cucumber.api.Scenario;

public class C64ColourRAM extends Device {
    private Scenario mScenario = null;
    int ram[] = new int[0x400];

    public C64ColourRAM(Scenario scenario) throws MemoryRangeException {
        super(0xd800, 0xdbff, "C64 Colour RAM");
        mScenario = scenario;
    }

    @Override
    public void write(int address, int data) throws MemoryAccessException {
        ram[address] = data;
    }

    @Override
    public int read(int address, boolean logRead) throws MemoryAccessException {
        return ram[address] & 0x0f; // Nybble RAM
    }

    @Override
    public String toString() {
        return "<foo>";
    }
}
