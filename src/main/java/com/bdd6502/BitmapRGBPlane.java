package com.bdd6502;

import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class BitmapRGBPlane extends DisplayLayer {
    int addressRegisters = 0xa300, addressExRegisters = 0x01;
    int addressExPlane0 = 0x03;
    byte plane0[] = new byte[1024 * 1024];
    byte plane1[] = new byte[1024 * 1024];

    int extraAddress = 0;

    int scrollX , scrollY;


    public BitmapRGBPlane() {
    }

    public BitmapRGBPlane(int addressRegisters, int addressExPlane0) {
        assertThat(addressRegisters, is(greaterThanOrEqualTo(0x8000)));
        assertThat(addressRegisters, is(lessThan(0xc000)));
        assertThat(addressRegisters & 0x7ff, is(equalTo(0x300)));
        this.addressRegisters = addressRegisters;
        this.addressExPlane0 = addressExPlane0;
    }

    @Override
    public boolean isAddressMatching(int address, int addressEx) {
        if (addressExActive(addressEx, addressExRegisters)) {
            return true;
        }
        if (addressExActive(addressEx, addressExPlane0)) {
            return true;
        }
        return false;
    }

    @Override
    public void writeData(int address, int addressEx, byte data) {

        // No control register logic now...
        if (addressExActive(addressEx, addressExRegisters) && address == (addressRegisters + 0x00)) {
            extraAddress = data & 0xff;
        }
        if (addressExActive(addressEx, addressExRegisters) && address == (addressRegisters + 0x01)) {
            scrollX = scrollX & 0xff00 | (data & 0xff);
        }
        if (addressExActive(addressEx, addressExRegisters) && address == (addressRegisters + 0x02)) {
            scrollX = scrollX & 0x00ff | ((data & 0xff) << 8);
        }
        if (addressExActive(addressEx, addressExRegisters) && address == (addressRegisters + 0x03)) {
            scrollY = scrollY & 0xff00 | (data & 0xff);
        }
        if (addressExActive(addressEx, addressExRegisters) && address == (addressRegisters + 0x04)) {
            scrollY = scrollY & 0x00ff | ((data & 0xff) << 8);
        }

        // This selection logic is because the actual address line is used to select the memory, not a decoder
        if (addressExActive(addressEx, addressExPlane0)) {
            int realAddress = ((extraAddress & 0xff) << 16) | (address & 0xffff);
            int theBank = realAddress & 1;
            realAddress = realAddress >> 1;
            realAddress = realAddress & (plane0.length - 1);
            if (theBank == 0) {
                plane0[realAddress] = data;
            } else {
                plane1[realAddress] = data;
            }
            setBusContention(8);
        }
    }

    @Override
    public void setAddressBus(int address, int addressEx) {
        memoryAsserted = false;
        if (addressExActive(addressEx, addressExRegisters) && address >= (addressRegisters) && address < (addressRegisters + 0x100)) {
            memoryAsserted = true;
        }
        if (addressExActive(addressEx, addressExPlane0)) {
            memoryAsserted = true;
        }
    }

    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync, boolean _doLineStart, boolean enableLayer, boolean vBlank) {
        displayH = (displayH + scrollX) & 0x3ff;
        displayV = (displayV + scrollY) & 0x3ff;
        int rgbpixel = (plane0[(displayV << 10) | displayH] & 0xff) | ((plane1[(displayV << 10) | displayH] & 0xff) << 8);

        // Matches RGB565 BMP format, but it's not the format we want because red and blue are swapped
//        rgbpixel = ((rgbpixel & 0x1f)<<11) | (((rgbpixel>>5) & 0x3f)<<5) | (((rgbpixel>>11) & 0x1f));

        return rgbpixel;
    }

    public void randomiseData(Random rand) {
        randomiseHelper(rand, plane0);
        extraAddress = rand.nextInt();
        scrollX = rand.nextInt();
        scrollY = rand.nextInt();
    }
}
