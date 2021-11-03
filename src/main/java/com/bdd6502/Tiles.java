package com.bdd6502;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class Tiles extends DisplayLayer {
    int addressRegisters = 0x9e00, addressExRegisters = 0x01;
    int addressScreen = 0x2000, addressExScreen = 0x80;
    int addressPlane0 = 0x2000, addressExPlane0 = 0x40;
    int addressPlane1 = 0x4000, addressExPlane1 = 0x40;
    int addressPlane2 = 0x8000, addressExPlane2 = 0x40;
    byte screenData[] = new byte[0x1000];
    byte colourData[] = new byte[0x1000];
    byte plane0[] = new byte[0x2000];
    byte plane1[] = new byte[0x2000];
    byte plane2[] = new byte[0x2000];
    byte plane3[] = new byte[0x2000];
    boolean enableTiles = false;
    int scrollX = 0, scrollY = 0;
    int backgroundColour = 0;
    int latchedDisplayV = 0;

    public Tiles() {
    }

    public Tiles(int addressRegisters, int addressExScreen, int addressExPlane0) {
        assertThat(addressRegisters, is(greaterThanOrEqualTo(0x8000)));
        assertThat(addressRegisters, is(lessThan(0xc000)));
        assertThat(addressRegisters & 0x7ff, is(equalTo(0x600)));
        assertThat(addressExScreen, is(not(equalTo(addressExPlane0))));
        this.addressRegisters = addressRegisters;
        this.addressExScreen = addressExScreen;
        this.addressExPlane0 = addressExPlane0;
        this.addressExPlane1 = addressExPlane0;
        this.addressExPlane2 = addressExPlane0;
    }

    @Override
    public void writeData(int address, int addressEx, byte data) {
        if (MemoryBus.addressActive(addressEx, addressExRegisters) && address == addressRegisters + 0x00) {
            if ((data & 0x10) > 0) {
                enableTiles = true;
            } else {
                enableTiles = false;
            }
        }
        if (MemoryBus.addressActive(addressEx, addressExRegisters) && address == addressRegisters + 0x01) {
            scrollX = (scrollX & 0x0f00) | (data & 0xff);
        }
        if (MemoryBus.addressActive(addressEx, addressExRegisters) && address == addressRegisters + 0x02) {
            scrollX = (scrollX & 0x00ff) | ((data & 0x0f) << 8);
        }
        if (MemoryBus.addressActive(addressEx, addressExRegisters) && address == addressRegisters + 0x03) {
            scrollY = (scrollY & 0x0f00) | (data & 0xff);
        }
        if (MemoryBus.addressActive(addressEx, addressExRegisters) && address == addressRegisters + 0x04) {
            scrollY = (scrollY & 0x00ff) | ((data & 0x0f) << 8);
        }
        if (MemoryBus.addressActive(addressEx, addressExRegisters) && address == addressRegisters + 0x07) {
            backgroundColour = data & 0xff;
//            System.out.println("BG change " + data + " " + display.displayH + " " + display.displayV + " " + display.getFrameNumberForSync());
        }

        if (MemoryBus.addressActive(addressEx, addressExScreen) && MemoryBus.addressActive(address, addressScreen)) {
            busContention = display.getBusContentionPixels();
            if (MemoryBus.addressActive(address, 0x1000)) {
                colourData[address & 0xfff] = data;
            } else {
                screenData[address & 0xfff] = data;
            }
        }

        // This selection logic is because the actual address line is used to select the memory, not a decoder
        if (MemoryBus.addressActive(addressEx, addressExPlane0)) {
            busContention = display.getBusContentionPixels();
            if (MemoryBus.addressActive(address, addressPlane0)) {
                plane0[address & 0x1fff] = data;
            }
            if (MemoryBus.addressActive(address, addressPlane1)) {
                plane1[address & 0x1fff] = data;
            }
            if (MemoryBus.addressActive(address, addressPlane2)) {
                plane2[address & 0x1fff] = data;
            }
            if(is16Colours && MemoryBus.addressLower8KActive(address)) {
                plane3[address & 0x1fff] = data;
            }
        }
    }

    @Override
    public void setAddressBus(int address, int addressEx) {
        memoryAsserted = false;
        if (MemoryBus.addressActive(addressEx, addressExScreen) && MemoryBus.addressActive(address, addressScreen)) {
            memoryAsserted = true;
        }
        if (MemoryBus.addressActive(addressEx, addressExPlane0) && MemoryBus.addressActive(address, addressPlane0)) {
//            memoryAsserted = true;
        }
        if (MemoryBus.addressActive(addressEx, addressExPlane1) && MemoryBus.addressActive(address, addressPlane1)) {
//            memoryAsserted = true;
        }
        if (MemoryBus.addressActive(addressEx, addressExPlane2) && MemoryBus.addressActive(address, addressPlane2)) {
//            memoryAsserted = true;
        }
    }

    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync, boolean _doLineStart) {
        if (!enableTiles) {
            return 0;
        }

        if (withOverscan) {
            latchedDisplayV = displayV;
        } else {
            if ((displayH & 0x188) == 0) {
                latchedDisplayV = displayV;
            }
        }
        int latchedDisplayV2 = latchedDisplayV;

        if (!withOverscan) {
            // Adjust for the coordinates
            if (displayH >= 0x180) {
                displayH -= 0x80;
            }
        }
        // Adjust to match real hardware
        displayH -= 8;
        if (withOverscan) {
            displayH = displayH & 0x1ff;
        } else {
            displayH = displayH & 0xff;
        }
        // Add scrolls and clamp
        displayH += scrollX;
        latchedDisplayV2 += scrollY;
        displayH &= 0x3ff;
        latchedDisplayV2 &= 0x3ff;
        int index = ((displayH >> 4) & 0x3f) + (((latchedDisplayV2 >> 4) & 0x3f) * 0x40);
        int theChar = (screenData[index]) & 0xff;
//        System.out.println(displayH + " " + displayV + " Chars index: " + Integer.toHexString(index) + " char " + Integer.toHexString(theChar));
        byte theColour = colourData[index];
        displayH &= 0x0f;
        latchedDisplayV2 &= 0x0f;
        // Include flips
        if ((theColour & 0x40) > 0) {
            displayH = 0x0f - displayH;
        }
        if ((theColour & 0x80) > 0) {
            latchedDisplayV2 = 0x0f - latchedDisplayV2;
        }
        int pixelPlane0;
        int pixelPlane1;
        int pixelPlane2;
        int pixelPlane3 = 0;
        int quadrantOffset;
        if (latchedDisplayV2 < 8) {
            if (displayH < 8) {
                quadrantOffset = 0;
            } else {
                quadrantOffset = 8;
            }
        } else {
            if (displayH < 8) {
                quadrantOffset = 16;
            } else {
                quadrantOffset = 24;
            }
        }
        displayH &= 0x7;
        latchedDisplayV2 &= 0x7;
        displayH = 0x07 - displayH;
        pixelPlane0 = plane0[(theChar << 5) + latchedDisplayV2 + quadrantOffset] & (1 << displayH);
        pixelPlane1 = plane1[(theChar << 5) + latchedDisplayV2 + quadrantOffset] & (1 << displayH);
        pixelPlane2 = plane2[(theChar << 5) + latchedDisplayV2 + quadrantOffset] & (1 << displayH);
        if (is16Colours) {
            pixelPlane3 = plane3[(theChar << 5) + latchedDisplayV2 + quadrantOffset] & (1 << displayH);
        }
        int finalPixel = 0;
        if (pixelPlane0 > 0) {
            finalPixel |= 1;
        }
        if (pixelPlane1 > 0) {
            finalPixel |= 2;
        }
        if (pixelPlane2 > 0) {
            finalPixel |= 4;
        }
        if (is16Colours) {
            if (pixelPlane3 > 0) {
                finalPixel |= 8;
            }
            finalPixel |= ((theColour & 0x0f) << 4);
        } else {
            finalPixel |= ((theColour & 0x1f) << 3);
        }

        return getByteOrContention(finalPixel);
    }
}
