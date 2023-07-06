package com.bdd6502;

import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class MergeNTo1 extends DisplayLayer {
    int addressRegisters = 0xa200, addressExRegisters = 0x01;

    DisplayLayer[] displayLayers;
    int addIndex = 0;

    byte[] registers = new byte[0x02];
    boolean enableDither = false;

    public MergeNTo1(int numlayers, int addressRegisters) {
        assertThat(numlayers, is(equalTo(2)));
        assertThat(addressRegisters, is(greaterThanOrEqualTo(0x8000)));
        assertThat(addressRegisters, is(lessThan(0xc000)));
        assertThat(addressRegisters & 0x700, is(equalTo(0x200)));
        this.addressRegisters = addressRegisters;

        displayLayers = new DisplayLayer[numlayers];

        registers[0] = 0;
        registers[1] = 0;
    }

    @Override
    public void writeData(int address, int addressEx, byte data) {

        if (MemoryBus.addressActive(addressEx, addressExRegisters) && address >= (addressRegisters) && address < (addressRegisters + registers.length)) {
            registers[(address-addressRegisters) & 0x01] = data;

            if ((registers[0] & 0x04) == 0x04) {
                enableDither = true;
            } else {
                enableDither = false;
            }

        }

        for (int i = 0 ; i < displayLayers.length ; i++) {
            displayLayers[i].writeData(address,addressEx,data);
        }
    }

    @Override
    public void setAddressBus(int address, int addressEx) {
        for (int i = 0 ; i < displayLayers.length ; i++) {
            displayLayers[i].setAddressBus(address,addressEx);
        }
    }

    @Override
    public void ageContention() {
        for (int i = 0 ; i < displayLayers.length ; i++) {
            displayLayers[i].ageContention();
        }
    }

    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync, boolean _doLineStart, boolean enableLayer) {
        int latchedPixel = 0;
        // NOTE: Assume 2 layers max even though this has an array for displayLayers...
//        for (int i = 0 ; i < displayLayers.length ; i++) {
        int pixel0 = displayLayers[0].calculatePixel(displayH, displayV, _hSync, _vSync, _doLineStart, enableLayer);
        int pixel1 = displayLayers[1].calculatePixel(displayH, displayV, _hSync, _vSync, _doLineStart, enableLayer);

        switch (registers[0] & 0x03) {
            case 0:
                if (is16Colours) {
                    if ((pixel1 & 0x0f) != 0) {
                        latchedPixel = pixel1;
                    } else {
                        latchedPixel = pixel0;
                    }
                } else {
                    if ((pixel1 & 0x07) != 0) {
                        latchedPixel = pixel1;
                    } else {
                        latchedPixel = pixel0;
                    }
                }
                break;
            case 1:
                latchedPixel = pixel0 ^ pixel1;
                break;
            case 2:
                latchedPixel = pixel0 & pixel1;
                break;
            case 3:
                latchedPixel = pixel0 | pixel1;
                break;
        }

        latchedPixel ^= registers[1];

        if (enableDither && ((displayH ^ displayV) & 0x01) == 0x01) {
            latchedPixel &= 0xf0;
        }

        return latchedPixel;
    }

    @Override
    public boolean capturingMergeLayer() {
        return addIndex < displayLayers.length;
    }

    @Override
    public void captureLayer(DisplayLayer layer) {
        assertThat(addIndex , is(lessThan(displayLayers.length)));
        displayLayers[addIndex] = layer;
        addIndex++;
    }

    @Override
    public String getDebug() {
        String debug = "";
        for (int i = 0 ; i < displayLayers.length ; i++) {
            debug += displayLayers[i].getDebug();
        }
        return debug;
    }

    public void randomiseData(Random rand) {
        randomiseHelper(rand , registers);
        for (DisplayLayer displayLayer : displayLayers) {
            displayLayer.randomiseData(rand);
        }
        enableDither = rand.nextBoolean();
    }

}
