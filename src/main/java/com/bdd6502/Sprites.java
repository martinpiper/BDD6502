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
            busContention = display.getBusContentionPixels();
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
        if (displayH >= 0x188) {
            displayH -= 0x80;
            return 0;
        }
        if (displayH >= 0x180) {
            displayH -= 0x80;
        }
        displayH -= 0x08;
        if (displayH < 0) {
            return 0;
        }
        displayH = displayH & 0xff;
        int finalPixel = calculatedRasters[onScreen][displayH];
        // And progressively clear the output pixel, like the hardware does
        calculatedRasters[onScreen][displayH] = 0;

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
        // +32 to adjust for expected behaviour where 0y = Bottom of the sprite on the bottom edge of the visible screen
        int deltaY = (displayV + spriteSize + spriteY[spriteIndex]) & 0xff;
        if (!fullHeightSprite && (deltaY >= spriteSize)) {
            return;
        }

        // Range to max sprite size, to handle fullHeightSprite
        deltaY = deltaY & (spriteSize-1);
        int realPixelIndex , realDeltaY;
        if ((theColour & 0x80) > 0) {
            realDeltaY = (spriteSize-1) - deltaY;
        } else {
            realDeltaY = deltaY;
        }
        for (int pixelIndex = 0 ; pixelIndex < spriteSize ; pixelIndex++) {
            // Include flips
            if ((theColour & 0x40) > 0) {
                realPixelIndex = (spriteSize-1) - pixelIndex;
            } else {
                realPixelIndex = pixelIndex;
            }
            int pixelPlane0;
            int pixelPlane1;
            int pixelPlane2;
            int quadrantOffset = 0;
            if (spriteSize == 32) {
                // TODO: Needs expansion for 32x32 sprites
                if ((realDeltaY & 0x1f) < 8) {
                    if (realPixelIndex < 8) {
                        quadrantOffset = 0;
                    } else if (realPixelIndex < 16) {
                        quadrantOffset = 8;
                    } else if (realPixelIndex < 24) {
                        quadrantOffset = 32;
                    } else {
                        quadrantOffset = 40;
                    }
                } else if ((realDeltaY & 0x1f) < 16) {
                    if (realPixelIndex < 8) {
                        quadrantOffset = 16;
                    } else if (realPixelIndex < 16) {
                        quadrantOffset = 24;
                    } else if (realPixelIndex < 24) {
                        quadrantOffset = 48;
                    } else {
                        quadrantOffset = 56;
                    }
                } else if ((realDeltaY & 0x1f) < 24) {
                    if (realPixelIndex < 8) {
                        quadrantOffset = 64;
                    } else if (realPixelIndex < 16) {
                        quadrantOffset = 72;
                    } else if (realPixelIndex < 24) {
                        quadrantOffset = 96;
                    } else {
                        quadrantOffset = 104;
                    }
                } else {
                    if (realPixelIndex < 8) {
                        quadrantOffset = 80;
                    } else if (realPixelIndex < 16) {
                        quadrantOffset = 88;
                    } else if (realPixelIndex < 24) {
                        quadrantOffset = 112;
                    } else {
                        quadrantOffset = 120;
                    }
                }

            } else {
                if ((realDeltaY & 0x0f) < 8) {
                    if (realPixelIndex < 8) {
                        quadrantOffset = 0;
                    } else {
                        quadrantOffset = 8;
                    }
                } else {
                    if (realPixelIndex < 8) {
                        quadrantOffset = 16;
                    } else {
                        quadrantOffset = 24;
                    }
                }
            }
            int pixelShift = 0x07 - (realPixelIndex & 0x07);
            int tileY = realDeltaY & 0x07;
            int planeOffset = (spriteFrame[spriteIndex] << 5) + tileY + quadrantOffset;
            planeOffset &= 0x1fff;
            pixelPlane0 = plane0[planeOffset] & (1 << pixelShift);
            pixelPlane1 = plane1[planeOffset] & (1 << pixelShift);
            pixelPlane2 = plane2[planeOffset] & (1 << pixelShift);
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
            int finalXPos = (spriteX[spriteIndex]+pixelIndex) & 0xff;
            // Only output the pixel if there is nothing else there
            if ((calculatedRasters[offScreen][finalXPos] & 0x07) == 0){
                calculatedRasters[offScreen][finalXPos] = finalPixel;
            }
        }
    }
}
