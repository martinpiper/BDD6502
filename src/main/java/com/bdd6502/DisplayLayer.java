package com.bdd6502;

public abstract class DisplayLayer {
    DisplayBombJack theDisplay = null;

    public void setDisplay(DisplayBombJack theDisplay) {
        this.theDisplay = theDisplay;
    }

    public abstract void writeData(int address, int addressEx, int data);
}
