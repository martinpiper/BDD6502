package com.bdd6502;

import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class StaticColour extends DisplayLayer {
    int paletteIndex = 0;

    public StaticColour() {
    }

    public StaticColour(int paletteIndex) {
        assertThat(paletteIndex, is(greaterThanOrEqualTo(0)));
        assertThat(paletteIndex, is(lessThan(256)));
        this.paletteIndex = paletteIndex;
    }

    @Override
    public boolean isAddressMatching(int address, int addressEx) {
        return false;
    }

    @Override
    public void writeData(int address, int addressEx, byte data) {
    }

    @Override
    public void setAddressBus(int address, int addressEx) {

    }

    @Override
    public void randomiseData(Random rand) {

    }

    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync, boolean _doLineStart, boolean enableLayer, boolean vBlank) {
        return getByteOrContention(paletteIndex);
    }
}
