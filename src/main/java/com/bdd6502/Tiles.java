package com.bdd6502;

public class Tiles extends DisplayLayer {
    int busContention = 0;
    byte screenData[] = new byte[0x1000];
    byte colourData[] = new byte[0x1000];
    byte plane0[] = new byte[0x2000];
    byte plane1[] = new byte[0x2000];
    byte plane2[] = new byte[0x2000];
    boolean enableDisplay = false;
    int scrollX = 0, scrollY = 0;
    int backgroundColour = 0;

    @Override
    public void writeData(int address, int addressEx, byte data) {
        if (DisplayBombJack.addressExActive(addressEx , 0x01) && address == 0x9e00) {
            if ((data & 0x10) > 0) {
                enableDisplay = true;
            } else {
                enableDisplay = false;
            }
        }
        if (DisplayBombJack.addressExActive(addressEx , 0x01) && address == 0x9e01) {
            scrollX = (scrollX & 0x0f00) | (data & 0xff);
        }
        if (DisplayBombJack.addressExActive(addressEx , 0x01) && address == 0x9e02) {
            scrollX = (scrollX & 0x00ff) | ((data & 0x0f)<<8);
        }
        if (DisplayBombJack.addressExActive(addressEx , 0x01) && address == 0x9e03) {
            scrollY = (scrollY & 0x0f00) | (data & 0xff);
        }
        if (DisplayBombJack.addressExActive(addressEx , 0x01) && address == 0x9e04) {
            scrollY = (scrollY & 0x00ff) | ((data & 0x0f)<<8);
        }
        if (DisplayBombJack.addressExActive(addressEx , 0x01) && address == 0x9e07) {
            backgroundColour = data & 0xff;
        }

        if (DisplayBombJack.addressExActive(addressEx , 0x80) && address >= 0x2000 && address < 0x3000) {
            busContention = display.getBusContentionPixels();
            screenData[address & 0xfff] = data;
        }
        if (DisplayBombJack.addressExActive(addressEx , 0x80) && address >= 0x3000 && address < 0x4000) {
            busContention = display.getBusContentionPixels();
            colourData[address & 0xfff] = data;
        }

        // This selection logic is because the actual address line is used to select the memory, not a decoder
        if (DisplayBombJack.addressExActive(addressEx , 0x40) && (address & 0x2000) > 0) {
            busContention = display.getBusContentionPixels();
            plane0[address & 0x1fff] = data;
        }
        if (DisplayBombJack.addressExActive(addressEx , 0x40) && (address & 0x4000) > 0) {
            busContention = display.getBusContentionPixels();
            plane1[address & 0x1fff] = data;
        }
        if (DisplayBombJack.addressExActive(addressEx , 0x40) && (address & 0x8000) > 0) {
            busContention = display.getBusContentionPixels();
            plane2[address & 0x1fff] = data;
        }
    }

    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync) {
        if (!enableDisplay) {
            return 0;
        }
        // Adjust for the extra timing
        if (displayH >= 0x180) {
            displayH -= 0x80;
        }
        // Adjust to match real hardware
        displayH -= 8;
        displayH = displayH & 0xff;
        // Add scrolls and clamp
        displayH += scrollX;
        displayV += scrollY;
        displayH &= 0x3ff;
        displayV &= 0x3ff;
        int index = ((displayH>>4) & 0x3f) + (((displayV>>4) & 0x3f) * 0x40);
        int theChar = (screenData[index]) & 0xff;
//        System.out.println(displayH + " " + displayV + " Chars index: " + Integer.toHexString(index) + " char " + Integer.toHexString(theChar));
        byte theColour = colourData[index];
        displayH &= 0x0f;
        displayV &= 0x0f;
        // Include flips
        if ((theColour & 0x40) > 0) {
            displayH = 0x0f-displayH;
        }
        if ((theColour & 0x80) > 0) {
            displayV = 0x0f-displayV;
        }
        int pixelPlane0;
        int pixelPlane1;
        int pixelPlane2;
        int quadrantOffset = 0;
        if (displayV < 8) {
            if (displayH < 8) {
                quadrantOffset = 0;
            } else {
                quadrantOffset = 8;
            }
        } else {
            if (displayH < 8) {
                quadrantOffset = 16;
            } else {
                quadrantOffset = 24;
            }
        }
        displayH &= 0x7;
        displayV &= 0x7;
        displayH = 0x07 - displayH;
        pixelPlane0 = plane0[(theChar << 5) + displayV + quadrantOffset] & (1 << displayH);
        pixelPlane1 = plane1[(theChar << 5) + displayV + quadrantOffset] & (1 << displayH);
        pixelPlane2 = plane2[(theChar << 5) + displayV + quadrantOffset] & (1 << displayH);
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
        if (finalPixel == 0) {
            // Withut this, the lack of connection from the tiles board to the layer4 header is simulated
//            finalPixel = backgroundColour;
        } else {
            finalPixel |= ((theColour & 0x1f) << 3);
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
