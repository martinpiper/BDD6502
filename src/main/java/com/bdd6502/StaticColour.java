package com.bdd6502;

import org.apache.commons.lang3.RandomUtils;

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
    public void writeData(int address, int addressEx, byte data) {
    }

    @Override
    public void setAddressBus(int address, int addressEx) {

    }

    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync) {
        return getByteOrContention(paletteIndex);
    }
}
