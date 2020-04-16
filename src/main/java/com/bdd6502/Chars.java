package com.bdd6502;

public class Chars extends DisplayLayer {
    int busContention = 0;
    byte screenData[] = new byte[0x400];
    byte colourData[] = new byte[0x400];
    byte charsPlane0[] = new byte[0x2000];
    byte charsPlane1[] = new byte[0x2000];
    byte charsPlane2[] = new byte[0x2000];
    @Override
    public void writeData(int address, int addressEx, byte data) {
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
        // Adjust for the extra timing
        if (displayH >= 0x180) {
            displayH -= 0x80;
        }
        displayH = displayH & 0xff;
        // -1 to match the real hardware
        int index = (((displayH>>3)-1) & 0x1f) + (((displayV>>3) & 0x1f) * 0x20);
        int theChar = (screenData[index]) & 0xff;
//        System.out.println(displayH + " " + displayV + " Chars index: " + Integer.toHexString(index) + " char " + Integer.toHexString(theChar));
        byte theColour = colourData[index];
        // Include extra chars from the colour
        theChar |= (theColour & 0x30) << 4;
        displayH &= 0x07;
        displayH = 7 - displayH;
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
        if (finalPixel != 0) {
            finalPixel |= ((theColour & 0x0f) << 3);
        }
        if (busContention > 0) {
            finalPixel = display.getRandomColouredPixel();
        }
        if (busContention > 0) {
            busContention--;
        }
        return finalPixel;
    }
}
