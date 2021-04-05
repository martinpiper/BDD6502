package com.bdd6502;

import org.apache.commons.lang3.RandomUtils;

import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class GetBackground extends DisplayLayer {
    int layerIndex = 0;
    DisplayLayer layer = null;

    public GetBackground() {
    }

    public GetBackground(int layerIndex) {
        assertThat(layerIndex, is(greaterThanOrEqualTo(0)));
        this.layerIndex = layerIndex;
    }

    @Override
    public void writeData(int address, int addressEx, byte data) {
    }

    @Override
    public void setAddressBus(int address, int addressEx) {

    }

    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync) {
        if (layer == null) {
            layer = display.layers.get(layerIndex);
        }
        int finalPixel = 0;

        if (layer instanceof Tiles) {
            // Really should have an interface for a "second pixel" output
            finalPixel = ((Tiles)layer).backgroundColour;
        } else {
            // Flash the pixel, indicating the layer index is not connected properly to the pixel header input
            finalPixel = RandomUtils.nextInt(0,256);
        }

        return getByteOrContention(finalPixel);
    }
}
