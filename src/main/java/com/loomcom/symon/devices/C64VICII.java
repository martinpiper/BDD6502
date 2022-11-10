package com.loomcom.symon.devices;


import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;
import cucumber.api.Scenario;

public class C64VICII extends Device {
    private Scenario mScenario = null;
    private int upperStore = 0;
    boolean rasterToggle = true;

    public C64VICII( Scenario scenario) throws MemoryRangeException {
        super(0xd000, 0xd02f, "C64 VIC-II");
        mScenario = scenario;
    }

    @Override
    public void write(int address, int data) throws MemoryAccessException {
        int register = address & 0x3f;
        switch (register) {
            case 0x12: // Raster
                break;
            default:
                break;
        }
    }

    @Override
    public int read(int address, boolean logRead) throws MemoryAccessException {
        int register = address & 0x3f;
        switch (register) {
            case 0x12: // Raster
                // For the kernal display mode check at $ff5e
                rasterToggle = !rasterToggle;
                if (rasterToggle) {
                    return 0xff;
                } else {
                    return 0x00;
                }

            default:
                return 0;
        }
    }

    @Override
    public String toString() {
        return "<foo>";
    }
}