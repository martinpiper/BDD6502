package com.bdd6502;

public abstract class DisplayLayer extends MemoryBus {
    protected DisplayBombJack display = null;
    protected boolean is16Colours = false;

    public boolean isWithOverscan() {
        return withOverscan;
    }

    public void setWithOverscan(boolean withOverscan) {
        this.withOverscan = withOverscan;
    }

    protected boolean withOverscan = false;

    public void setDisplay(DisplayBombJack theDisplay) {
        display = theDisplay;
    }

    public abstract int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync, boolean _doLineStart, boolean enableLayer, boolean vBlank);

    protected int getByteOrContention(int value) {
        if (busContention > 0 || memoryAsserted) {
            return display.getContentionColouredPixel();
        }
        return value & 0xff;
    }

    public void make16Colours() {
        is16Colours = true;
    }

    public boolean capturingMergeLayer() {
        return false;
    }

    public void captureLayer(DisplayLayer layer) {
    }

    public String getDebug() {
        return "";
    }
}
