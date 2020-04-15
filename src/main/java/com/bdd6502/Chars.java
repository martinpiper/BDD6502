package com.bdd6502;

public class Chars extends DisplayLayer {
    int busContention = 0;
    int screenData[] = new int[0x400];
    int colourData[] = new int[0x400];
    int charsPlane0[] = new int[0x800];
    int charsPlane1[] = new int[0x800];
    int charsPlane2[] = new int[0x800];
    @Override
    public void writeData(int address, int addressEx, int data) {
        if (DisplayBombJack.addressExActive(addressEx , 0x01) && address >= 0x9000 && address < 0x9400) {
            busContention = display.getBusContentionPixels();
            screenData[address - 0x9000] = data;
        }
        if (DisplayBombJack.addressExActive(addressEx , 0x01) && address >= 0x9400 && address < 0x9800) {
            busContention = display.getBusContentionPixels();
            colourData[address - 0x9400] = data;
        }

        // This selection logic is because the actual address line is used to select the memory, not a decoder
        if (DisplayBombJack.addressExActive(addressEx , 0x20) && (address & 0x2000) > 0) {
            busContention = display.getBusContentionPixels();
            charsPlane0[address & 0x1fff] = data;
        }
        if (DisplayBombJack.addressExActive(addressEx , 0x20) && (address & 0x4000) > 0) {
            busContention = display.getBusContentionPixels();
            charsPlane1[address & 0x1fff] = data;
        }
        if (DisplayBombJack.addressExActive(addressEx , 0x20) && (address & 0x8000) > 0) {
            busContention = display.getBusContentionPixels();
            charsPlane2[address & 0x1fff] = data;
        }
    }

    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync) {
        int index = ((displayH>>3) & 0x1f) + (((displayV>>3) & 0x1f) * 0x20);
        int theChar = screenData[index];
        int theColour = colourData[index];
        if (theChar > 0 && theColour > 0) {
            theChar = theChar;
        }
        // Include extra chars from the colour
        theChar += theColour & 0x30;
        displayH &= 0x07;
        displayV &= 0x07;
        // Include flips
        if ((theColour & 0x40) > 0) {
            displayH = 7-displayH;
        }
        if ((theColour & 0x80) > 0) {
            displayV = 7-displayV;
        }
        int pixelPlane0 = charsPlane0[(theChar<<3) + displayV] & (1<<displayH);
        int pixelPlane1 = charsPlane1[(theChar<<3) + displayV] & (1<<displayH);
        int pixelPlane2 = charsPlane2[(theChar<<3) + displayV] & (1<<displayH);
        int finalPixel = 0;
        if (pixelPlane0 > 0) {
            finalPixel |= 1;
        }
        if (pixelPlane1 > 0) {
            finalPixel |= 2;
        }
        if (pixelPlane2 > 0) {
            finalPixel |= 4;
        }
        finalPixel |= ((theColour & 0x1f)<<3);
        if (busContention > 0) {
            busContention--;
        }
        return finalPixel;
    }
}
