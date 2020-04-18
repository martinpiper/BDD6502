package com.bdd6502;

public class Sprites extends DisplayLayer {
    int busContention = 0;
    byte plane0[] = new byte[0x2000];
    byte plane1[] = new byte[0x2000];
    byte plane2[] = new byte[0x2000];
    int lo32 = 0 , hi32 = 0;
    boolean spriteEnable = false;
    int spriteX[] = new int[24];
    int spriteY[] = new int[24];
    int spriteFrame[] = new int[24];
    int spritePalette[] = new int[24];

    int calculatedRasters[][] = new int[2][256];

    @Override
    public void writeData(int address, int addressEx, byte data) {
        if (DisplayBombJack.addressExActive(addressEx , 0x01) && address == 0x9a00) {
            lo32 = data & 0x0f;
            if ((data & 0x10) > 0) {
                spriteEnable = true;
            } else {
                spriteEnable = false;
            }
        }
        if (DisplayBombJack.addressExActive(addressEx , 0x01) && address == 0x9a01) {
            hi32 = data & 0x0f;
        }

        if (DisplayBombJack.addressExActive(addressEx , 0x01) && address >= 0x9820 && address < 0x9880) {
            int spriteIndex = (address - 0x9820) / 4;
            switch (address & 0x03) {
                case 0:
                default:
                {
                    spriteFrame[spriteIndex] = data & 0xff;
                    break;
                }
                case 1:
                {
                    spritePalette[spriteIndex] = data & 0xff;
                    break;
                }
                case 2:
                {
                    spriteY[spriteIndex] = data & 0xff;
                    break;
                }
                case 3:
                {
                    spriteX[spriteIndex] = data & 0xff;
                    break;
                }
            }
        }

        // This selection logic is because the actual address line is used to select the memory, not a decoder
        if (DisplayBombJack.addressExActive(addressEx , 0x10) && (address & 0x2000) > 0) {
            busContention = display.getBusContentionPixels();
            plane0[address & 0x1fff] = data;
        }
        if (DisplayBombJack.addressExActive(addressEx , 0x10) && (address & 0x4000) > 0) {
            busContention = display.getBusContentionPixels();
            plane1[address & 0x1fff] = data;
        }
        if (DisplayBombJack.addressExActive(addressEx , 0x10) && (address & 0x8000) > 0) {
            busContention = display.getBusContentionPixels();
            plane2[address & 0x1fff] = data;
        }
    }

    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync) {
        if (!spriteEnable) {
            return 0;
        }

        handleSpriteSchedule(displayH,displayV);

        int onScreen = displayV & 1;
        // Output calculated data
        // Adjust for the extra timing
        if (displayH >= 0x180) {
            displayH -= 0x80;
        }
        displayH -= 0x08;
        displayH = displayH & 0xff;
        int finalPixel = calculatedRasters[onScreen][displayH];

        if (busContention > 0) {
            finalPixel = display.getRandomColouredPixel();
        }
        if (busContention > 0) {
            busContention--;
        }
        return finalPixel;
    }

    void handleSpriteSchedule(int displayH, int displayV) {
        int offScreen = 1-(displayV & 1);
        if (displayH == 0x180) {
            // Clear the raster
            for (int i =0; i < 256; i++) {
                calculatedRasters[offScreen][i] = 0;
            }
        }
        // Not time yet to update the sprite
        if ((displayH & 0x0f) != 0) {
            return;
        }
        int spriteIndex = 0;
        if (displayH >= 0x180) {
            spriteIndex = (displayH - 0x180) / 16;
        } else {
            spriteIndex = 8 + (displayH / 16);
        }

        // Handle timings of sprite register reads at the appropriate time in the raster
        int theColour = spritePalette[spriteIndex];
        int spriteSize = 16;
        if (spriteIndex >= lo32 && spriteIndex < hi32) {
            spriteSize = 32;
        }
        boolean fullHeightSprite = false;
        if ((theColour & 0x20) > 0) {
            fullHeightSprite = true;
        }

        // Sprite Y position range check
        int deltaY = (displayV + spriteY[spriteIndex] & 0xff);
        if (!fullHeightSprite && (deltaY >= spriteSize)) {
            return;
        }

        // Range to max sprite size, to handle fullHeightSprite
        deltaY = deltaY & 0x1f;
        for (int pixelIndex = 0 ; pixelIndex < spriteSize ; pixelIndex++) {
            // Include flips
            if ((theColour & 0x40) > 0) {
                displayH = 0x0f - displayH;
            }
            if ((theColour & 0x80) > 0) {
                displayV = 0x0f - displayV;
            }
            int pixelPlane0;
            int pixelPlane1;
            int pixelPlane2;
            int quadrantOffset;
            // TODO: Needs expansion for 32x32 sprites
            if (deltaY < 8) {
                if (pixelIndex < 8) {
                    quadrantOffset = 0;
                } else {
                    quadrantOffset = 8;
                }
            } else {
                if (pixelIndex < 8) {
                    quadrantOffset = 16;
                } else {
                    quadrantOffset = 24;
                }
            }
            int pixelShift = 0x07 - (pixelIndex & 0x07);
            int tileY = deltaY & 0x07;
            pixelPlane0 = plane0[(spriteFrame[spriteIndex] << 5) + tileY + quadrantOffset] & (1 << pixelShift);
            pixelPlane1 = plane1[(spriteFrame[spriteIndex] << 5) + tileY + quadrantOffset] & (1 << pixelShift);
            pixelPlane2 = plane2[(spriteFrame[spriteIndex] << 5) + tileY + quadrantOffset] & (1 << pixelShift);
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
            finalPixel |= ((theColour & 0x1f) << 3);
            calculatedRasters[offScreen][(spriteX[spriteIndex]+pixelIndex) & 0xff] = finalPixel;
        }
    }
}
