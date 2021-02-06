package com.bdd6502;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class Chars extends DisplayLayer {
    int addressScreen = 0x9000, addressExScreen = 0x01;
    int addressColour = 0x9400, addressExColour = 0x01;
    int addressPlane0 = 0x2000, addressExPlane0 = 0x20;
    int addressPlane1 = 0x4000, addressExPlane1 = 0x20;
    int addressPlane2 = 0x8000, addressExPlane2 = 0x20;
    int addressScreenV4_0 = 0x4000, addressExScreenV4_0 = 0x80;
    byte screenData[];
    byte colourData[];
    byte plane0[] = new byte[0x2000];
    byte plane1[] = new byte[0x2000];
    byte plane2[] = new byte[0x2000];
    byte plane3[] = new byte[0x2000];
    int latchedDisplayV = 0;
    boolean memoryAssertedScreenRAM = false;
    boolean memoryAssertedPlane = false;
    int hiPalette = 0;
    boolean isV4_0 = false;
    byte screenDataV4_0[];
    int theBank = 0;
    boolean displayDisable = false;

    public Chars() {
    }

    public Chars(int addressScreen, int addressExPlane0) {
        assertThat(addressScreen, is(greaterThanOrEqualTo(0x8000)));
        assertThat(addressScreen, is(lessThan(0xc000)));
        assertThat(addressScreen & 0x7ff, is(equalTo(0x00)));
        this.addressScreen = addressScreen;
        this.addressColour = addressScreen + 0x400;
        this.addressExPlane0 = addressExPlane0;
        this.addressExPlane1 = addressExPlane0;
        this.addressExPlane2 = addressExPlane0;
        screenData = new byte[0x400];
        colourData = new byte[0x400];
    }

    public Chars(int addressScreen, int addressExScreen, int addressExPlane0) {
        assertThat(addressScreen, is(greaterThanOrEqualTo(0x8000)));
        assertThat(addressScreen, is(lessThan(0xc000)));
        assertThat(addressScreen & 0x7ff, is(equalTo(0x00)));
        this.addressScreen = addressScreen;
        this.addressColour = addressScreen + 0x400;
        this.addressExPlane0 = addressExPlane0;
        this.addressExPlane1 = addressExPlane0;
        this.addressExPlane2 = addressExPlane0;
        this.addressExScreenV4_0 = addressExScreen;
        isV4_0 = true;
        screenDataV4_0 = new byte[0x2000];
    }

    @Override
    public void writeData(int address, int addressEx, byte data) {
        if (MemoryBus.addressActive(addressEx, addressExScreen) && address >= addressScreen && address < (addressScreen + 0x1f)) {
            if (!isV4_0) {
                busContention = display.getBusContentionPixels();
            }
            if (!is16Colours) {
                hiPalette = (data & 0x01) << 4;
            }
            theBank = (data >> 6) & 0x03;

            displayDisable = MemoryBus.addressActive(data , 0x02);
        }
        if (!isV4_0) {
            if (MemoryBus.addressActive(addressEx, addressExScreen) && address >= addressScreen && address < (addressScreen + 0x400)) {
                busContention = display.getBusContentionPixels();
                screenData[address & 0x3ff] = data;
            }
            if (MemoryBus.addressActive(addressEx, addressExColour) && address >= addressColour && address < (addressColour + 0x400)) {
                busContention = display.getBusContentionPixels();
                colourData[address & 0x3ff] = data;
            }
        } else {
            if (MemoryBus.addressActive(addressEx, addressExScreenV4_0) && address >= addressScreenV4_0 && address < (addressScreenV4_0 + 0x2000)) {
                busContention = display.getBusContentionPixels();
                screenDataV4_0[address & 0x1fff] = data;
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
        memoryAssertedScreenRAM = false;
        if (MemoryBus.addressActive(addressEx, addressExScreen) && address >= addressScreen && address < (addressScreen + 0x400)) {
            memoryAssertedScreenRAM = true;
        }
        if (MemoryBus.addressActive(addressEx, addressExColour) && address >= addressColour && address < (addressColour + 0x400)) {
            memoryAssertedScreenRAM = true;
        }
        // This selection logic is because the actual address line is used to select the memory, not a decoder
        if (MemoryBus.addressActive(addressEx, addressExPlane0)) {
            memoryAssertedPlane = true;
        } else {
            memoryAssertedPlane = false;
        }
    }

    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync) {
        if (displayDisable) {
            return 0;
        }

        if ((displayH & 0x188) == 0) {
            latchedDisplayV = displayV;
        }
        // Adjust for the extra timing
        if (displayH >= 0x180) {
            displayH -= 0x80;
        }
        displayH = displayH & 0xff;
        // -1 to match the real hardware
        int index = (((displayH >> 3) - 1) & 0x1f) + (((latchedDisplayV >> 3) & 0x1f) * 0x20);
        int theChar;
        if (!isV4_0) {
            theChar = (screenData[index]) & 0xff;
        } else {
            theChar = (screenDataV4_0[index + (theBank * 0x800)]) & 0xff;
        }
//        System.out.println(displayH + " " + latchedDisplayV + " Chars index: " + Integer.toHexString(index) + " char " + Integer.toHexString(theChar));
        byte theColour;
        if (!isV4_0) {
            theColour = colourData[index];
        } else {
            theColour = screenDataV4_0[index + (theBank * 0x800) + 0x400];
        }
        if (memoryAssertedScreenRAM) {
            theChar = 0xff;
            theColour = (byte) 0xff;
        }
        // Include extra chars from the colour
        theChar |= (theColour & 0x30) << 4;
        displayH &= 0x07;
        displayH = 7 - displayH;
        int latchedDisplayV2 = latchedDisplayV;
        latchedDisplayV2 &= 0x07;
        // Include flips
        if ((theColour & 0x40) > 0) {
            displayH = 7 - displayH;
        }
        if ((theColour & 0x80) > 0) {
            latchedDisplayV2 = 7 - latchedDisplayV2;
        }
        int pixelPlane0 = plane0[(theChar << 3) + latchedDisplayV2] & (1 << displayH);
        int pixelPlane1 = plane1[(theChar << 3) + latchedDisplayV2] & (1 << displayH);
        int pixelPlane2 = plane2[(theChar << 3) + latchedDisplayV2] & (1 << displayH);
        int pixelPlane3 = 0;
        if (is16Colours) {
            pixelPlane3 = plane3[(theChar << 3) + latchedDisplayV2] & (1 << displayH);
        }
        if (memoryAssertedPlane) {
            pixelPlane0 = 0xff;
            pixelPlane1 = 0xff;
            pixelPlane2 = 0xff;
            if (is16Colours) {
                pixelPlane3 = 0xff;
            }
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
            finalPixel |= (((theColour & 0x0f)) << 4);
        } else {
            finalPixel |= (((theColour & 0x0f) | hiPalette) << 3);
        }
        return getByteOrContention(finalPixel);
    }
}
