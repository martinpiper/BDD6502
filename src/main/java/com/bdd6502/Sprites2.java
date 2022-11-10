package com.bdd6502;

import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class Sprites2 extends DisplayLayer {

    int addressRegisters = 0x9000, addressExRegisters = 0x01;
    int addressPlane0 = 0x2000, addressExPlane0 = 0x10;
    int addressPlane1 = 0x4000, addressExPlane1 = 0x10;
    int addressPlane2 = 0x8000, addressExPlane2 = 0x10;
    byte plane0[] = new byte[0x2000];
    byte plane1[] = new byte[0x2000];
    byte plane2[] = new byte[0x2000];
    byte plane3[] = new byte[0x2000];
    final int kNumSprites = 64;
    int spriteX[] = new int[kNumSprites];
    int spriteY[] = new int[kNumSprites];
    int spriteScaleExtentX[] = new int[kNumSprites];
    int spriteSizeY[] = new int[kNumSprites];
    int spriteScaleXInv[] = new int[kNumSprites];
    int spriteScaleYInv[] = new int[kNumSprites];
    int spriteFrame[] = new int[kNumSprites];
    int spritePalette[] = new int[kNumSprites];

    boolean prevHSYNC = false;

    int drawingSpriteIndex = 0;
    int drawingSpriteState = 0;
    int currentSpriteX = 0 , currentSpriteY = 0 , currentSpriteFrame = 0 , currentSpritePalette = 0;
    int currentSpriteScaleExtentX = 0 , currentSpriteSizeY = 0;
    int currentSpriteScaleXInv = 0 , currentSpriteScaleYInv = 0;
    int currentSpriteXPixel = 0;
    int currentSpriteYPixel = 0;
    int insideHeight = 0;
    int fetchingPixel = 0;
    int onScreen = 0;
    double clockMultiplier = 1.0f;


    int calculatedRasters[][] = new int[2][512];

    int debugUsedRasterTime[] = new int[1024];
    int debugMaxDisplayV = 0;

    public Sprites2() {
    }

    public Sprites2(int addressRegisters, int addressExPlane0) {
        this(addressRegisters,  addressExPlane0 , 1.0);
    }

    public Sprites2(int addressRegisters, int addressExPlane0 , double clockMultiplier) {
        assertThat(clockMultiplier, is(greaterThanOrEqualTo(0.5)));
        assertThat(addressRegisters, is(greaterThanOrEqualTo(0x8000)));
        assertThat(addressRegisters, is(lessThan(0xc000)));
        assertThat(addressRegisters & 0x7ff, is(equalTo(0x200)));

        this.clockMultiplier = clockMultiplier;
        this.addressRegisters = addressRegisters;
        this.addressExPlane0 = addressExPlane0;
    }

    @Override
    public void writeData(int address, int addressEx, byte data) {
/*
        // No control register logic now...
        if (MemoryBus.addressActive(addressEx, addressExRegisters) && address == (addressRegisters + 0x100)) {
            if ((data & 0x01) > 0) {
            } else {
            }
        }
*/
        if (MemoryBus.addressActive(addressEx, addressExRegisters) && address >= (addressRegisters + 0x00) && address < (addressRegisters + 0x200)) {
            busContention = display.getBusContentionPixels();
            int spriteIndex = (address - addressRegisters) / 0x08;
            switch (address & 0x07) {
                case 0:
                default: {
                    spritePalette[spriteIndex] = data & 0xff;
                    break;
                }
                case 1: {
                    spriteY[spriteIndex] = data & 0xff;
                    break;
                }
                case 2: {
                    spriteSizeY[spriteIndex] = data & 0xff;
                    break;
                }
                case 3: {
                    spriteX[spriteIndex] = data & 0xff;
                    break;
                }
                case 4: {
                    spriteScaleExtentX[spriteIndex] = data & 0xff;
                    break;
                }
                case 5: {
                    spriteScaleYInv[spriteIndex] = data & 0xff;
                    break;
                }
                case 6: {
                    spriteScaleXInv[spriteIndex] = data & 0xff;
                    break;
                }
                case 7: {
                    spriteFrame[spriteIndex] = data & 0xff;
                    break;
                }
            }
        }

        // This selection logic is because the actual address line is used to select the memory, not a decoder
        if (MemoryBus.addressActive(addressEx, addressExPlane0)) {
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
        if (MemoryBus.addressActive(addressEx, addressExRegisters) && address >= (addressRegisters + 0x00) && address < (addressRegisters + 0x200)) {
//            memoryAsserted = true;
        }
        if (MemoryBus.addressActive(addressEx, addressExPlane0) && MemoryBus.addressActive(address, addressPlane0)) {
//            memoryAsserted = true;
        }
    }

    double clockAccumulator = 0.0;
    boolean delayed_doLineStart1 = false;
    boolean delayed_doLineStart2 = false;

    boolean reachedEndOfLine = false;

    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync, boolean _doLineStart, boolean enableLayer) {
        // Time to the rising edge of the _hSync
        if (!prevHSYNC && _hSync) {
            // Flip-flip in hardware
            onScreen = 1-onScreen;
        }
        boolean runLogic = true;
        prevHSYNC = _hSync;
        // To emulate the longer delayed line start
        if (_doLineStart || delayed_doLineStart1 || delayed_doLineStart2) {
            // Reset on low in hardware
            fetchingPixel = 0;
            drawingSpriteIndex = 0;
            drawingSpriteState = 0;
            runLogic = false;
            reachedEndOfLine = false;
        }
        delayed_doLineStart2 = delayed_doLineStart1;
        delayed_doLineStart1 = _doLineStart;
        if (!enableLayer) {
            // Reset on low in hardware
            drawingSpriteIndex = 0;
            drawingSpriteState = 0;
        }

        clockAccumulator += clockMultiplier;
        while (clockAccumulator >= 1.0) {
            if (runLogic && !reachedEndOfLine) {
                handleSpriteSchedule(displayH, displayV);
            }
            clockAccumulator -= 1.0;
        }

        // Output calculated data
        int finalPixel = calculatedRasters[onScreen][fetchingPixel];
        // And progressively clear the output pixel, like the hardware does
        calculatedRasters[onScreen][fetchingPixel] = 0;
        fetchingPixel++;
        fetchingPixel &= 0x1ff;

        return finalPixel;
    }

    void handleSpriteSchedule(int displayH, int displayV) {
        int offScreen = 1 - onScreen;

        // Reading Y first gives time to calculate vertical extent and skip the sprite before drawing the span
        switch (drawingSpriteState) {
            // Even cycles, on the low portion of the event, on the +ve edge, the odd cycles, the load is actually completed
            case 0:
            case 2:
            case 4:
            case 8:
            case 10:
            case 12:
            case 14:
            default:
                drawingSpriteState++;
                break;

            case 1:
                // Palette bits also used for X&Y MSB
                currentSpritePalette = spritePalette[drawingSpriteIndex];
                drawingSpriteState++;
                break;

            case 3:
                currentSpriteY = spriteY[drawingSpriteIndex] | ((currentSpritePalette & 0x20) << 3);
                // To avoid the code having to invert Y across 9 bits, the hardware can do it cheaply...
                currentSpriteY = ~currentSpriteY;
                currentSpriteY &= 0x1ff;
                drawingSpriteState++;
                break;

            case 5:
                currentSpriteSizeY = spriteSizeY[drawingSpriteIndex];
                drawingSpriteState++;
                break;

            case 6:
                // On low S2_LDSPRX

                // Test for end of list
                if (currentSpriteSizeY == 0) {
                    debugUsedRasterTime[displayV] = displayH;
                    if(displayV > debugMaxDisplayV) {
                        debugMaxDisplayV = displayV;
                    }
                    reachedEndOfLine = true;
                    return;
                }
                // Perform Y extent check, the wait is for the calculation to succeed due to multiply 32 (shift 5!!) lookup and add, and advance drawingSpriteIndex if it isn't going to be drawn
                // This check uses the inverted Y, so a subtract is actually achieved.
                insideHeight = (displayV + currentSpriteY);
                // Note, unsigned comparison with low bits
                insideHeight &= 0x1ff;
                if (insideHeight >= currentSpriteSizeY) {
                    advanceSprite();
                    return;
                }

                drawingSpriteState++;
                break;

            case 7:
                currentSpriteX = spriteX[drawingSpriteIndex] | ((currentSpritePalette & 0x10) << 4);
                drawingSpriteState++;
                break;

            case 9:
                currentSpriteScaleExtentX = spriteScaleExtentX[drawingSpriteIndex];
                drawingSpriteState++;
                break;


            case 11:
                currentSpriteScaleYInv = spriteScaleYInv[drawingSpriteIndex];
                drawingSpriteState++;
                break;

            case 13:
                // Lookup table calculation "(insideHeight * currentSpriteScaleYInv)", updated for each raster drawn
                // TODO: There is a use case where "(currentSpriteScaleYInv/2)" is loaded from a value, instead of half the step value
                currentSpriteYPixel = (currentSpriteScaleYInv/2) + ((insideHeight & 0xff) * currentSpriteScaleYInv);

                currentSpriteScaleXInv = spriteScaleXInv[drawingSpriteIndex];
                // TODO: There is a use case where this is loaded from a value, instead of half the step value
                currentSpriteXPixel = currentSpriteScaleXInv / 2;
                drawingSpriteState++;
                break;

            case 15:
                currentSpriteFrame = spriteFrame[drawingSpriteIndex];
                drawingSpriteState++;
                // Note the fall through to simulate the transition from loading the data to rendering the first pixel
//                break;

            case 16:
                int pixelX = (currentSpriteXPixel >> 5) & 0x1f;
                int pixelY = (currentSpriteYPixel >> 5) & 0x1f;
                if ((currentSpritePalette & 0x40) > 0) {
                    pixelX = 31 - pixelX;
                }
                if ((currentSpritePalette & 0x80) > 0) {
                    pixelY = 31 - pixelY;
                }
                // Starts drawing from the opposite half of the coordinate range first
                if ((currentSpriteFrame &0x40) > 0) {
                    pixelX ^= 0x10;
                }
                if ((currentSpriteFrame &0x80) > 0) {
                    pixelY ^= 0x10;
                }
                pixelX &= 0x1f;
                pixelY &= 0x1f;
                currentSpriteX = currentSpriteX & 0x1ff;
                // Drawing pixels...
                int theColour = 0;
                // Selector
                int internalFrame = (currentSpriteFrame & 0x0f)>>1;
                int internalAddress = (internalFrame * 0x400) + pixelX + (pixelY << 5);
                switch (currentSpriteFrame & 0x30) {
                    case 0x00:
                    default:
                        theColour = plane0[internalAddress];
                        break;
                    case 0x10:
                        theColour = plane1[internalAddress];
                    break;
                    case 0x20:
                        theColour = plane2[internalAddress];
                        break;
                    case 0x30:
                        theColour = plane3[internalAddress];
                        break;
                }
                // Selector
                if ((currentSpriteFrame & 0x01) != 0) {
                    theColour >>= 4;
                }
                theColour &= 0x0f;
                theColour |= ((currentSpritePalette & 0x0f) << 4);

                if ((calculatedRasters[offScreen][currentSpriteX] & 0x0f) == 0) {
                    calculatedRasters[offScreen][currentSpriteX] = theColour;
                }

                // Update coordinates after pixel draw...
                currentSpriteX++;
                currentSpriteXPixel += currentSpriteScaleXInv;
                // Can be a carry check...
                if (((currentSpriteXPixel >> 5) & 0xff) >= currentSpriteScaleExtentX) {
                    advanceSprite();
                    return;
                }

                break;
        }
    }

    private void advanceSprite() {
        drawingSpriteState = 0;
        drawingSpriteIndex++;
        drawingSpriteIndex &= 0xff;
    }

    @Override
    public String getDebug() {
        String debug = "";
        for (int i = 0 ; i < debugMaxDisplayV ; i++) {
            // Only flag those lines that are really starting to get to the limit
            if (debugUsedRasterTime[i] > 320) {
                debug += "V" + i + " H" + debugUsedRasterTime[i] + "   ";
            }
        }
        if (!debug.isEmpty()) {
            debug = "Sprites2: " + debug;
        }
        return debug;
    }

    public void randomiseData(Random rand) {
        randomiseHelper(rand, plane0);
        randomiseHelper(rand, plane1);
        randomiseHelper(rand, plane2);
        randomiseHelper(rand, plane3);

        randomiseHelper(rand, calculatedRasters[0]);
        randomiseHelper(rand, calculatedRasters[1]);

        randomiseHelper(rand, spriteX);
        randomiseHelper(rand, spriteY);
        randomiseHelper(rand, spriteScaleExtentX);
        randomiseHelper(rand, spriteSizeY);
        randomiseHelper(rand, spriteScaleXInv);
        randomiseHelper(rand, spriteScaleYInv);
        randomiseHelper(rand, spriteFrame);
        randomiseHelper(rand, spritePalette);
    }
}
