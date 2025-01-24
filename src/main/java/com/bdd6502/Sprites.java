package com.bdd6502;

import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class Sprites extends DisplayLayer {

    int addressRegisters = 0x9800, addressExRegisters = 0x01;
    int addressPlane0 = 0x2000, addressExPlane0 = 0x10;
    int addressPlane1 = 0x4000, addressExPlane1 = 0x10;
    int addressPlane2 = 0x8000, addressExPlane2 = 0x10;
    byte plane0[] = new byte[0x2000];
    byte plane1[] = new byte[0x2000];
    byte plane2[] = new byte[0x2000];
    byte plane3[] = new byte[0x2000];
    int lo32 = 0, hi32 = 0;
    boolean spriteEnable = false;
    boolean skipNextSprite = false;
    int spriteX[] = new int[64];
    int spriteY[] = new int[64];
    int spriteFrame[] = new int[64];
    int spritePalette[] = new int[64];
    int fetchingPixel = 0;
    int onScreen = 0;

    int calculatedRasters[][] = new int[2][512];

    boolean calculationUsesSeparateClock = false;
    double clockMultiplier = 1.0;
    double clockAccumulator = 0.0;

    public Sprites() {
    }

    public Sprites(int addressRegisters, int addressExPlane0) {
        assertThat(addressRegisters, is(greaterThanOrEqualTo(0x8000)));
        assertThat(addressRegisters, is(lessThan(0xc000)));
        assertThat(addressRegisters & 0x7ff, is(equalTo(0x0000)));
        this.addressRegisters = addressRegisters;
        this.addressExPlane0 = addressExPlane0;
        this.addressExPlane1 = addressExPlane0;
        this.addressExPlane2 = addressExPlane0;
    }

    public Sprites(int addressRegisters, int addressExPlane0, double clockMultiplier) {
        assertThat(addressRegisters, is(greaterThanOrEqualTo(0x8000)));
        assertThat(addressRegisters, is(lessThan(0xc000)));
        assertThat(addressRegisters & 0x7ff, is(equalTo(0x0000)));
        assertThat(clockMultiplier, is(greaterThanOrEqualTo(1.0)));
        this.addressRegisters = addressRegisters;
        this.addressExPlane0 = addressExPlane0;
        this.addressExPlane1 = addressExPlane0;
        this.addressExPlane2 = addressExPlane0;
        this.clockMultiplier = clockMultiplier;
        this.calculationUsesSeparateClock = true;
    }

    @Override
    public void writeData(int address, int addressEx, byte data) {
        if (addressExActive(addressEx, addressExRegisters) && address == (addressRegisters + 0x200)) {
            lo32 = data & 0x0f;
            if (!withOverscan) {
                if ((data & 0x10) > 0) {
                    spriteEnable = true;
                } else {
                    spriteEnable = false;
                }
            }
        }
        if (addressExActive(addressEx, addressExRegisters) && address == (addressRegisters + 0x201)) {
            hi32 = data & 0x0f;
        }

        int registerOffset = 0x20;
        if (withOverscan) {
            registerOffset = 0;
        }
        if (addressExActive(addressEx, addressExRegisters) && address >= (addressRegisters + registerOffset) && address < (addressRegisters + 0x100 + registerOffset)) {
            busContention = display.getBusContentionPixels();
            int spriteIndex = (address - (addressRegisters + registerOffset)) / 4;
            switch (address & 0x03) {
                case 0:
                default: {
                    spriteFrame[spriteIndex] = data & 0xff;
                    break;
                }
                case 1: {
                    spritePalette[spriteIndex] = data & 0xff;
                    break;
                }
                case 2: {
                    spriteY[spriteIndex] = data & 0xff;
                    break;
                }
                case 3: {
                    spriteX[spriteIndex] = data & 0xff;
                    break;
                }
            }
        }

        // This selection logic is because the actual address line is used to select the memory, not a decoder
        if (addressExActive(addressEx, addressExPlane0)) {
            busContention = display.getBusContentionPixels();
            if (MemoryBus.addressActive(address, addressPlane0)) {
                plane0[address & 0x1fff] = data;
            }
            if (MemoryBus.addressActive(address, addressPlane1)) {
                plane1[address & 0x1fff] = data;
            }
            if (MemoryBus.addressActive(address, addressPlane2)) {
                plane2[address & 0x1fff] = data;
            }
            if(is16Colours && MemoryBus.addressLower8KActive(address)) {
                plane3[address & 0x1fff] = data;
            }
        }
    }

    @Override
    public void setAddressBus(int address, int addressEx) {
        memoryAsserted = false;
        if (addressExActive(addressEx, addressExRegisters) && address >= (addressRegisters + 0x20) && address < (addressRegisters + 0x80)) {
//            memoryAsserted = true;
        }
        if (addressExActive(addressEx, addressExPlane0) && MemoryBus.addressActive(address, addressPlane0)) {
//            memoryAsserted = true;
        }
        if (addressExActive(addressEx, addressExPlane1) && MemoryBus.addressActive(address, addressPlane1)) {
//            memoryAsserted = true;
        }
        if (addressExActive(addressEx, addressExPlane2) && MemoryBus.addressActive(address, addressPlane2)) {
//            memoryAsserted = true;
        }
    }

    int internalScanDisplayH = 0;
    int lastSpriteClocked = 24;
    int lastSpriteClockedPixels = 16;
    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync, boolean _doLineStart, boolean enableLayer, boolean vBlank) {
        if (withOverscan) {
            spriteEnable = enableLayer;
        }

        if (!calculationUsesSeparateClock) {
            handleSpriteSchedule(displayH, displayV);

            // Output calculated data
            // Adjust for the extra timing
            // Emulate the sprite scan read counters 6A/6B/6C/6D
            if (displayH == 0x08) {
                fetchingPixel = 0;
                onScreen = displayV & 1;
            }
        } else {
            // Output calculated data
            // Adjust for the extra timing
            if (displayH == 0x00) {
                onScreen = displayV & 1;
                int adjustmentOffset = 8;
                // Account for fractional clock skew between scanlines
                if (clockAccumulator >= 0.5) {
                    adjustmentOffset++;
                }
                lastSpriteClocked = (int)(((384 * clockMultiplier)-adjustmentOffset) / 16);
                lastSpriteClockedPixels = (int)(((384 * clockMultiplier)-adjustmentOffset) % 16);
            }

            // To emulate the longer delayed line start
            if (displayH <= 0x01) {
                internalScanDisplayH = 0;
                fetchingPixel = 0;
            }

            clockAccumulator += clockMultiplier;
            while (clockAccumulator >= 1.0) {
                handleSpriteSchedule(internalScanDisplayH, displayV);
                clockAccumulator -= 1.0;
                internalScanDisplayH++;
            }
        }

        if (withOverscan) {
            if (fetchingPixel >= 0x180 ) {
                return 0;
            }
        } else {
            if (fetchingPixel >= 0x100 ) {
                return 0;
            }
        }
        int finalPixel = calculatedRasters[onScreen][fetchingPixel];
        if (calculationUsesSeparateClock && fetchingPixel <= 1) {
            // Emulate the contention pixels at the start of the scanline due to clock differences
            finalPixel = display.getContentionColouredPixel();
        }
        // And progressively clear the output pixel, like the hardware does
        calculatedRasters[onScreen][fetchingPixel++] = 0;

        return finalPixel;
    }

    int spriteIndexReJig[] = {8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,0,1,2,3,4,5,6,7};

    void handleSpriteSchedule(int displayH, int displayV) {
        if (withOverscan) {
            if (displayH == 0x0) {
                skipNextSprite = false;
            }
        } else {
            if (displayH == 0x180) {
                skipNextSprite = false;
            }
        }
        int offScreen = 1 - (displayV & 1);
        if (!withOverscan) {
            if (displayH >= 0x180) {
            }
            if (displayH >= 0x180) {
                displayH -= 0x80;
            }
        }
        // Although the sprite registers are initially loaded at 0x180, there is a second read at 0x188
        // This accounts for the low ENABLEPIXELS at 0x189
        // So shift the entire schedule back by 8 to make the maths easier
        if (!calculationUsesSeparateClock) {
            displayH -= 0x08;
        } else {
            // Nothing to do here
        }
        // Not time yet to update the sprite
        if ((displayH < 0) || (displayH & 0x0f) != 0) {
            return;
        }
        if (skipNextSprite) {
            skipNextSprite = false;
            return;
        }
        int spriteIndex = (displayH / 16);
        if (!withOverscan) {
            spriteIndex = spriteIndexReJig[spriteIndex];
        }

//        System.out.println("Sprite: " + spriteIndex  + " line " + displayV + " offScreen " + offScreen);

        // Handle timings of sprite register reads at the appropriate time in the raster
        int theColour = getByteOrContention(spritePalette[spriteIndex]);
        boolean isMSBX = false;
        if (withOverscan) {
            if ((theColour & 0x10) > 0) {
                isMSBX = true;
            }
        }
        int spriteSizeX = 16;
        int spriteSizeXSpan = 16;
        int spriteSizeY = 16;

        // Same logic as hardware 6R, 6S, 5R, 5S, 5T, 6T
        if (withOverscan) {
            int tweakIndex = spriteIndex >> 1;
            // Note: In hardware simulation the first sprite (index 0) does not display correctly if the comparison logic is changed to allow this behaviour.
            // So the comparison logic excludes this sprite by using +5V input to the "<" cascade input for the comparators.
            if (spriteIndex > 1) {
                if ((tweakIndex <= lo32 && !(tweakIndex <= hi32)) || (tweakIndex <= hi32 && !(tweakIndex <= lo32))) {
                    spriteSizeX = 32;
                    spriteSizeXSpan = 32;
                    spriteSizeY = 32;
                    skipNextSprite = true;
                }
            }
        } else {
            int tweakIndex = (spriteIndex >> 1) + 3;
            if ((tweakIndex < lo32 && !(tweakIndex < hi32)) || (tweakIndex < hi32 && !(tweakIndex < lo32))) {
                spriteSizeX = 32;
                spriteSizeXSpan = 32;
                spriteSizeY = 32;
                skipNextSprite = true;
            }
        }
        boolean fullHeightSprite = false;
        if ((theColour & 0x20) > 0) {
            fullHeightSprite = true;
            // Simulation shows sprite size in the Y is ignored for full height sprites
            spriteSizeY = 16;
        }

        if (!calculationUsesSeparateClock) {
            if (withOverscan) {
                // Adjust last sprite for hardware behaviour
                if (spriteIndex == 22 && spriteSizeXSpan == 32) {
                    spriteSizeXSpan = 24;
                }
                else if (spriteIndex == 23 && spriteSizeXSpan == 16) {
                    spriteSizeXSpan = 8;
                }
            }
        } else {
            // Sprite width is determined by remaining clocks in the scanline
            if (spriteIndex == lastSpriteClocked-1 && spriteSizeXSpan == 32) {
                spriteSizeXSpan = 16 + lastSpriteClockedPixels;
            }
            else if (spriteIndex == lastSpriteClocked && spriteSizeXSpan == 16) {
                spriteSizeXSpan = lastSpriteClockedPixels;
            }
        }

        // Sprite Y position range check
        // +32 to adjust for expected behaviour where 0y = Bottom of the sprite on the bottom edge of the visible screen
        int deltaY = (displayV + spriteSizeY + getByteOrContention(spriteY[spriteIndex])) & 0xff;
        if (!fullHeightSprite && (deltaY >= spriteSizeY)) {
            // This paints transparent pixels, but with palette information.
            // This emulates the observed sprite display behaviour if it is the last layer
            int finalPixel = 0;
            if (is16Colours) {
                finalPixel = ((theColour & 0x0f) << 4);
            } else {
                finalPixel = ((theColour & 0x1f) << 3);
            }
            for (int pixelIndex = 0; pixelIndex < spriteSizeXSpan; pixelIndex++) {
                int finalXPos = getByteOrContention(spriteX[spriteIndex]) + pixelIndex;
                if (isMSBX) {
                    finalXPos += 0x100;
                }
                if (withOverscan) {
                    finalXPos &= 0x1ff;
                } else {
                    finalXPos &= 0xff;
                }

                if (is16Colours) {
                    if ((calculatedRasters[offScreen][finalXPos] & 0x0f) == 0) {
                        calculatedRasters[offScreen][finalXPos] = finalPixel;   // Note, no contention since bit plane shifters are forced to be reset to 0
                    }
                } else {
                    if ((calculatedRasters[offScreen][finalXPos] & 0x07) == 0) {
                        calculatedRasters[offScreen][finalXPos] = finalPixel;   // Note, no contention since bit plane shifters are forced to be reset to 0
                    }
                }
            }
            return;
        }

        // Range to max sprite size, to handle fullHeightSprite
        deltaY = deltaY & (spriteSizeY - 1);
        int realPixelIndex, realDeltaY;
        if ((theColour & 0x80) > 0) {
            realDeltaY = (spriteSizeY - 1) - deltaY;
        } else {
            realDeltaY = deltaY;
        }
        for (int pixelIndex = 0; pixelIndex < spriteSizeXSpan; pixelIndex++) {
            // Include flips
            if ((theColour & 0x40) > 0) {
                realPixelIndex = (spriteSizeX - 1) - pixelIndex;
            } else {
                realPixelIndex = pixelIndex;
            }
            int pixelPlane0;
            int pixelPlane1;
            int pixelPlane2;
            int pixelPlane3 = 0;
            int quadrantOffset = 0;
            if (spriteSizeX == 32) {
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
            int tweakFrame = getByteOrContention(spriteFrame[spriteIndex]);
            if (spriteSizeX == 32 && spriteSizeY == 32) {
                tweakFrame *= 4;
                tweakFrame &= 0xff;
            }
            // Emulate the observed simulation behaviour for this strange mode :)
            if (spriteSizeX == 32 && spriteSizeY == 16) {
                tweakFrame *= 4;
                tweakFrame += 2;
                tweakFrame &= 0xff;
            }
            int planeOffset = (tweakFrame << 5) + tileY + quadrantOffset;
            planeOffset &= 0x1fff;
            pixelPlane0 = plane0[planeOffset] & (1 << pixelShift);
            pixelPlane1 = plane1[planeOffset] & (1 << pixelShift);
            pixelPlane2 = plane2[planeOffset] & (1 << pixelShift);
            if (is16Colours) {
                pixelPlane3 = plane3[planeOffset] & (1 << pixelShift);
            }
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
            if (is16Colours) {
                if (pixelPlane3 > 0) {
                    finalPixel |= 8;
                }
            }
            finalPixel = getByteOrContention(finalPixel);
            // Like the hardware, the sprite enable forces the sprite pixel to be all transparent
            if (!spriteEnable) {
                finalPixel = 0;
            }
            if (is16Colours) {
                finalPixel |= ((theColour & 0x0f) << 4);
            } else {
                finalPixel |= ((theColour & 0x1f) << 3);
            }
            int finalXPos = (getByteOrContention(spriteX[spriteIndex]) + pixelIndex);
            if (isMSBX) {
                finalXPos += 0x100;
            }
            if (withOverscan) {
                finalXPos &= 0x1ff;
            } else {
                finalXPos &= 0xff;
            }

            // Only output the pixel if there is nothing else there
            if (is16Colours) {
                if ((calculatedRasters[offScreen][finalXPos] & 0x0f) == 0) {
                    calculatedRasters[offScreen][finalXPos] = finalPixel;
                }
            } else {
                if ((calculatedRasters[offScreen][finalXPos] & 0x07) == 0) {
                    calculatedRasters[offScreen][finalXPos] = finalPixel;
                }
            }
        }
    }

    public void randomiseData(Random rand) {
        randomiseHelper(rand , plane0);
        randomiseHelper(rand , plane1);
        randomiseHelper(rand , plane2);
        randomiseHelper(rand , plane3);

        randomiseHelper(rand , calculatedRasters[0]);
        randomiseHelper(rand , calculatedRasters[1]);

        lo32 = rand.nextInt();
        hi32 = rand.nextInt();
        spriteEnable = rand.nextBoolean();
        skipNextSprite = rand.nextBoolean();

        randomiseHelper(rand , spriteX);
        randomiseHelper(rand , spriteY);
        randomiseHelper(rand , spriteFrame);
        randomiseHelper(rand , spritePalette);
    }
}
