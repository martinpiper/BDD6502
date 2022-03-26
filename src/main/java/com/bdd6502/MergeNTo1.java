package com.bdd6502;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class MergeNTo1 extends DisplayLayer {

    DisplayLayer[] displayLayers;
    int addIndex = 0;

    public MergeNTo1(int numlayers) {
        displayLayers = new DisplayLayer[numlayers];
    }

    @Override
    public void writeData(int address, int addressEx, byte data) {
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
        for (int i = 0 ; i < displayLayers.length ; i++) {
            int pixel = displayLayers[i].calculatePixel(displayH, displayV, _hSync, _vSync, _doLineStart, enableLayer);

            if (is16Colours) {
                if ((pixel & 0x0f) != 0) {
                    latchedPixel = pixel;
                }
            } else {
                if ((pixel & 0x07) != 0) {
                    latchedPixel = pixel;
                }
            }
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
}
