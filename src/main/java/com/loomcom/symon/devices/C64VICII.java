package com.loomcom.symon.devices;


import com.loomcom.symon.exceptions.MemoryAccessException;
import com.loomcom.symon.exceptions.MemoryRangeException;
import cucumber.api.Scenario;

public class C64VICII extends Device {
    private Scenario mScenario = null;
    private int upperStore = 0;

    int registers[] = new int[0x400];
    int rasterLine = 0;

    public C64VICII( Scenario scenario) throws MemoryRangeException {
        super(0xd000, 0xd3ff, "C64 VIC-II");
        mScenario = scenario;
    }

    @Override
    public void write(int address, int data) throws MemoryAccessException {
        int register = address & 0x3ff;
        registers[register] = data;
        switch (register) {
            case 0x12: // Raster
                break;
            default:
                break;
        }
    }

    @Override
    public int read(int address, boolean logRead) throws MemoryAccessException {
        int register = address & 0x3ff;
        switch (register) {
            case 0x11: // Raster
                updateRasterLine(logRead);
                if ((rasterLine & 0x100) == 0x100) {
                    return registers[register] | 0x80;
                }
                return registers[register] & 0x7f;

            case 0x12: // Raster
                updateRasterLine(logRead);
                return rasterLine & 0xff;

            default:
                return registers[register];
        }
    }

    boolean rasterLineCycle = true;

    public void setRasterLineFromDisplay(int raster) {
        rasterLineCycle = false;
        rasterLine = raster;
    }

    private void updateRasterLine(boolean logRead) {
        if (logRead && rasterLineCycle) {
            // For the kernal display mode check at $ff5e
            rasterLine++;
            if (rasterLine >= 312) {
                rasterLine = 0;
            }
            rasterLine &= 0xff;
        }
    }

    @Override
    public String toString() {
        return "<foo>";
    }
}
