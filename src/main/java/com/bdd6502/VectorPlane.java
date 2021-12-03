package com.bdd6502;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class VectorPlane extends DisplayLayer {
    int addressRegisters = 0xa000, addressExRegisters = 0x01;
    int addressPlane0 = 0x2000, addressExPlane0 = 0x02;
    int addressPlane1 = 0x4000, addressExPlane1 = 0x02;
    int addressPlane2 = 0x8000, addressExPlane2 = 0x02;
    byte plane0[] = new byte[0x2000];
    byte plane1[] = new byte[0x2000];
    byte plane2[] = new byte[0x2000];
    byte plane3[] = new byte[0x2000];

    boolean prevHSYNC = false;
    boolean onScreenBank = false;
    int drawIndex = 0;
    int pixelCount = 0;
    int finalPixel = 0;

    public VectorPlane() {
    }

    public VectorPlane(int addressRegisters, int addressExPlane0) {
        assertThat(addressRegisters, is(greaterThanOrEqualTo(0x8000)));
        assertThat(addressRegisters, is(lessThan(0xc000)));
        assertThat(addressRegisters & 0x7ff, is(equalTo(0x0000)));
        this.addressRegisters = addressRegisters;
        this.addressExPlane0 = addressExPlane0;
    }

    @Override
    public void writeData(int address, int addressEx, byte data) {

        // No control register logic now...
        if (MemoryBus.addressActive(addressEx, addressExRegisters) && address == addressRegisters) {
            if ((data & 0x01) > 0) {
                onScreenBank = true;
            } else {
                onScreenBank = false;
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
            if(MemoryBus.addressLower8KActive(address)) {
                plane3[address & 0x1fff] = data;
            }
        }
    }

    @Override
    public void setAddressBus(int address, int addressEx) {
        memoryAsserted = false;
        if (MemoryBus.addressActive(addressEx, addressExRegisters) && address >= (addressRegisters) && address < (addressRegisters + 0x200)) {
//            memoryAsserted = true;
        }
        if (MemoryBus.addressActive(addressEx, addressExPlane0) && MemoryBus.addressActive(address, addressPlane0)) {
//            memoryAsserted = true;
        }
    }

    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync, boolean _doLineStart, boolean enableLayer) {
        // End of line edge advances the address
        if (prevHSYNC && !_hSync) {
            drawIndex++;
        }
        prevHSYNC = _hSync;
        if (!_vSync || !enableLayer) {
            drawIndex = 0;
            pixelCount = 0;
            finalPixel = 0;
        }
        if (!_hSync) {
            pixelCount = 0;
            finalPixel = 0;
        }

        if (_hSync && _vSync) {
            // HW: Time this to an edge so that a 0xff input value results in 256 pixels being output
            // HW: Note the pixel count invert
            if (pixelCount == 0) {
                if (!onScreenBank) {
                    finalPixel = plane3[drawIndex];
                    pixelCount = ~plane0[drawIndex];
                } else {
                    finalPixel = plane1[drawIndex];
                    pixelCount = ~plane2[drawIndex];
                }
                pixelCount &= 0xff;
                finalPixel &= 0xff;
                drawIndex++;
                drawIndex &= 0x1fff;
            } else {
                pixelCount++;
            }
        }

        // HW: Note bit selection and wrap around
        pixelCount &= 0xff;

        return finalPixel;
    }
}
