package com.bdd6502;

public abstract class DisplayLayer {
    protected int busContention = 0;
    protected DisplayBombJack display = null;

    public void setDisplay(DisplayBombJack theDisplay) {
        display = theDisplay;
    }

    public abstract void writeData(int address, int addressEx, byte data);

    public abstract int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync);

    protected int getByteOrContention(int value) {
        if (busContention > 0) {
            return display.getContentionColouredPixel();
        }
        return value;
    }

    public void ageContention() {
        if (busContention > 0) {
            busContention--;
        }
    }
}
