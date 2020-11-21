package com.bdd6502;

public abstract class DisplayLayer extends MemoryBus {
    protected DisplayBombJack display = null;

    public void setDisplay(DisplayBombJack theDisplay) {
        display = theDisplay;
    }

    public abstract int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync);

    protected int getByteOrContention(int value) {
        if (busContention > 0 || memoryAsserted) {
            return display.getContentionColouredPixel();
        }
        return value;
    }

}
