package com.bdd6502;

import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class MergeNTo1 extends DisplayLayer {
    int addressRegisters = 0xa200, addressExRegisters = 0x01;

    DisplayLayer[] displayLayers;
    int addIndex = 0;
    int ditherPhase = 0;

    byte[] registers = new byte[0x02];
    boolean enableDither = false;

    boolean controlVisible0 = true;
    boolean controlVisible1 = true;

    int delayedLatchedPixel = 0;

    boolean forceOut0 = false;
    boolean forceOut1 = false;
    int latchedDither = 0;


    public MergeNTo1(int numlayers, int addressRegisters) {
        assertThat(numlayers, is(equalTo(2)));
        assertThat(addressRegisters, is(greaterThanOrEqualTo(0x8000)));
        assertThat(addressRegisters, is(lessThan(0xc000)));
        assertThat(addressRegisters & 0x700, is(equalTo(0x200)));
        this.addressRegisters = addressRegisters;

        displayLayers = new DisplayLayer[numlayers];

        registers[0] = 0;
        registers[1] = 0;
        ditherPhase = 0;

        controlVisible0 = true;
        controlVisible1 = true;

        delayedLatchedPixel = 0;

        forceOut0 = false;
        forceOut1 = false;
    }

    @Override
    public void writeData(int address, int addressEx, byte data) {

        if (addressExActive(addressEx, addressExRegisters) && address >= (addressRegisters) && address < (addressRegisters + registers.length)) {
            registers[(address-addressRegisters) & 0x01] = data;

            if (MemoryBus.addressActive(registers[0] , 0x04)) {
                enableDither = true;
            } else {
                enableDither = false;
            }

            if (MemoryBus.addressActive(registers[0] , 0x80)) {
                ditherPhase = 0x01;
            } else {
                ditherPhase = 0x00;
            }

            if (MemoryBus.addressActive(registers[0] , 0x08)) {
                forceOut0 = true;
            } else {
                forceOut0 = false;
            }
            if (MemoryBus.addressActive(registers[0] , 0x10)) {
                forceOut1 = true;
            } else {
                forceOut1 = false;
            }

            if (MemoryBus.addressActive(registers[0] , 0x20)) {
                controlVisible0 = true;
            } else {
                controlVisible0 = false;
            }
            if (MemoryBus.addressActive(registers[0] , 0x40)) {
                controlVisible1 = true;
            } else {
                controlVisible1 = false;
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

    int latchedPixel = 0;
    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync, boolean _doLineStart, boolean enableLayer, boolean vBlank) {
        // Because the merge layer has quite a long processing path of combinatorial multiplexer logic, the pixel is latched to provide a pipeline to grant enough time to process
        int returnPixel = latchedPixel;

        // NOTE: Assume 2 layers max even though this has an array for displayLayers...
//        for (int i = 0 ; i < displayLayers.length ; i++) {
        int pixel0 = displayLayers[0].calculatePixel(displayH, displayV, _hSync, _vSync, _doLineStart, enableLayer & controlVisible0, vBlank);
        int pixel1 = displayLayers[1].calculatePixel(displayH, displayV, _hSync, _vSync, _doLineStart, enableLayer & controlVisible1, vBlank);

        switch (registers[0] & 0x03) {
            case 0:
                // We only have a 16 colour pixel variant for this
                if (!forceOut0 && !forceOut1) {
                    if ((pixel1 & 0x0f) != 0) {
                        latchedPixel = pixel1;
                    } else {
                        latchedPixel = pixel0;
                    }
                } else if (forceOut0 && !forceOut1) {
                    latchedPixel = pixel0;
                } else {
                    // 01 && 11
                    latchedPixel = pixel1;
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

        if (enableDither && (latchedDither == ditherPhase)) {
            latchedPixel &= 0xf0;
        }

        // dither is also latched for the pipeline
        latchedDither = ((displayH ^ displayV) & 0x01);

        return returnPixel;
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
        ditherPhase = rand.nextInt() & 0x01;

        controlVisible0 = rand.nextBoolean();
        controlVisible1 = rand.nextBoolean();

        delayedLatchedPixel = rand.nextInt();

        forceOut0 = rand.nextBoolean();
        forceOut1 = rand.nextBoolean();
    }

}
