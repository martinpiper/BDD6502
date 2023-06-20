package com.bdd6502;

import com.loomcom.symon.util.HexUtil;

import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class VectorPlane extends DisplayLayer {
    int addressRegisters = 0xa100, addressExRegisters = 0x01;
    int addressPlane0 = 0x0000, addressExPlane0 = 0x02;
    byte plane0[] = new byte[0x2000];
    byte plane1[] = new byte[0x2000];
    byte plane2[] = new byte[0x2000];
    byte plane3[] = new byte[0x2000];

    boolean onScreenBank = false;
    boolean kill = false;
    int drawIndex = 0;
    int pixelCount = 0;
    int finalPixel = 0;
    int finalPixelDelay = 0;

    public VectorPlane() {
    }

    public VectorPlane(int addressRegisters, int addressExPlane0) {
        assertThat(addressRegisters, is(greaterThanOrEqualTo(0x8000)));
        assertThat(addressRegisters, is(lessThan(0xc000)));
        assertThat(addressRegisters & 0x7ff, is(equalTo(0x100)));
        this.addressRegisters = addressRegisters;
        this.addressExPlane0 = addressExPlane0;
    }

    @Override
    public void writeData(int address, int addressEx, byte data) {

        // No control register logic now...
        if (MemoryBus.addressActive(addressEx, addressExRegisters) && address == (addressRegisters + 0x00)) {
            if ((data & 0x01) > 0) {
                onScreenBank = true;
            } else {
                onScreenBank = false;
            }

            if ((data & 0x80) > 0) {
                kill = true;
            } else {
                kill = false;
            }
        }

        // This selection logic is because the actual address line is used to select the memory, not a decoder
        if (MemoryBus.addressActive(addressEx, addressExPlane0)) {
            if(!MemoryBus.addressActive(address, 0x8000)) {
                // The RAMs will only be in contention if they are being used to draw the frame
                // This is not strictly hardware accurate if the visible bank is switched during the contention period, but for emulation it will suffice
                // To be completely accurate would need two independent contentions for each bank
                if (!onScreenBank) {
                    busContention = display.getBusContentionPixels();
                }

                // HW: Select bits
                int addressShifted = address >> 1;
                if (!MemoryBus.addressActive(address, 0x01)) {
                    plane0[addressShifted & 0x1fff] = data;
                } else {
                    plane1[addressShifted & 0x1fff] = data;
                }
            } else {
                if (onScreenBank) {
                    busContention = display.getBusContentionPixels();
                }

                // HW: Select bits
                int addressShifted = address >> 1;
                if (!MemoryBus.addressActive(address, 0x01)) {
                    plane2[addressShifted & 0x1fff] = data;
                } else {
                    plane3[addressShifted & 0x1fff] = data;
                }
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
        finalPixelDelay = finalPixel;
        if (!_vSync || !enableLayer || kill) {
            drawIndex = 0;
            pixelCount = 0;
            finalPixel = 0;
            finalPixelDelay = 0;
        }
        if (displayH == 0 && displayV == 0) {
            // This is a fix to align emulation with the observed hardware
            // _EVSYNC and _EHSYNC are both low when H,V == 0,0
            drawIndex = 0;
            pixelCount = 0;
            finalPixel = 0;
            finalPixelDelay = 0;
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
                    finalPixel = getByteOrContention(plane0[drawIndex]);
                    pixelCount = getByteOrContention(plane1[drawIndex]);
//                    System.out.println("VectorPlane: x=" + displayH + " y=" + displayV + " addr=" + HexUtil.wordToHex(drawIndex*2) + " finalPixel=" + HexUtil.byteToHex(finalPixel) + " pixelCount=" + HexUtil.byteToHex(pixelCount));
                } else {
                    finalPixel = getByteOrContention(plane2[drawIndex]);
                    pixelCount = getByteOrContention(plane3[drawIndex]);
//                    System.out.println("VectorPlane: x=" + displayH + " y=" + displayV + " addr=" + HexUtil.wordToHex(0x8000 + (drawIndex*2)) + " finalPixel=" + HexUtil.byteToHex(finalPixel) + " pixelCount=" + HexUtil.byteToHex(pixelCount));
                }
                pixelCount = ~pixelCount;

                drawIndex++;
                drawIndex &= 0x1fff;
            } else {
                pixelCount++;
            }
        }

        // HW: Note bit selection and wrap around
        pixelCount &= 0xff;

        return finalPixelDelay;
    }

    public void randomiseData(Random rand) {
        randomiseHelper(rand, plane0);
        randomiseHelper(rand, plane1);
        randomiseHelper(rand, plane2);
        randomiseHelper(rand, plane3);

        onScreenBank = rand.nextBoolean();
        kill = rand.nextBoolean();
    }
}
