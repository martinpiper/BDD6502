package com.bdd6502;

import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class Sprites4 extends DisplayLayer {

    int addressRegisters = 0x8800, addressExRegisters = 0x01;
    int addressExPlane0 = 0x08;
    byte[] plane0 = new byte[0x10000];
    final int kNumSprites = 256;
    int drawingWith = 0;
    int writingTo = 1;
    int[][] spriteX = new int[2][kNumSprites];
    int[][] spriteY = new int[2][kNumSprites];
    int[][] spriteSizeX = new int[2][kNumSprites];
    int[][] spriteSizeY = new int[2][kNumSprites];
    int[][] spriteScaleXInv = new int[2][kNumSprites];
    int[][] spriteScaleYInv = new int[2][kNumSprites];
    int[][] spriteAddress = new int[2][kNumSprites];
    int[][] spriteStride = new int[2][kNumSprites];
    int[][] spritePalette = new int[2][kNumSprites];

    boolean prevVSYNC = false;

    int drawingSpriteIndex = 0;
    int drawingSpriteState = 0;
    int currentSpriteX = 0 , currentSpriteY = 0 , currentSpriteAddress = 0 , currentSpritePalette = 0;
    int currentSpriteXWorking = 0;
    int currentSpriteAddressWorking = 0;
    int currentSpriteSizeX = 0 , currentSpriteSizeY = 0;
    int currentSpriteSizeXWorking = 0;
    int currentSpriteScaleXInv = 0 , currentSpriteScaleYInv = 0;
    int currentSpriteXPixel = 0;
    int currentSpriteYPixel = 0;
    int currentSpriteStride = 0;
    int onScreen = 0;
    double clockMultiplier = 1.0f;

    boolean triggerBufferSwap = false;
    int leftBorderAdjust = 0;
    int topBorderAdjust = 0;
    int extentXPos = 255 , extentYPos = 255;


    int[][] calculatedFrames = new int[2][512*512];

    public Sprites4() {
    }

    public Sprites4(int addressRegisters, int addressExPlane0) {
        this(addressRegisters,  addressExPlane0 , 1.0);
    }

    public Sprites4(int addressRegisters, int addressExPlane0 , double clockMultiplier) {
        assertThat(clockMultiplier, is(greaterThanOrEqualTo(0.5)));
        assertThat(addressRegisters, is(greaterThanOrEqualTo(0x8000)));
        assertThat(addressRegisters, is(lessThan(0xc000)));
        assertThat(addressRegisters & 0x7ff, is(equalTo(0x000)));

        this.clockMultiplier = clockMultiplier;
        this.addressRegisters = addressRegisters;
        this.addressExPlane0 = addressExPlane0;
    }

    @Override
    public void writeData(int address, int addressEx, byte data) {
/*
        // No control register logic now...
        if (addressExActive(addressEx, addressExRegisters) && address == (addressRegisters + 0x100)) {
            if ((data & 0x01) > 0) {
            } else {
            }
        }
*/
        if (addressExActive(addressEx, addressExRegisters) && address >= (addressRegisters) && address < (addressRegisters + 0x800)) {
            busContention = display.getBusContentionPixels();
            int latchedValuesDetect = address - addressRegisters;
            if (latchedValuesDetect < 8) {
                switch (latchedValuesDetect) {
                    case 0:
                        if (!triggerBufferSwap && (data & 0x01) == 0x01)
                        {
                            triggerBufferSwap = true;
                        }
                        break;
                    case 1 :
                        leftBorderAdjust = (leftBorderAdjust & 0xff00) | (data & 0xff);
                        break;
                    case 2 :
                        leftBorderAdjust = (leftBorderAdjust & 0x00ff) | ((data & 0xff) << 8);
                        break;
                    case 3 :
                        topBorderAdjust = (topBorderAdjust & 0xff00) | (data & 0xff);
                        break;
                    case 4 :
                        topBorderAdjust = (topBorderAdjust & 0x00ff) | ((data & 0xff) << 8);
                        break;
                    case 5:
                        extentXPos = data & 0xff;
                        break;
                    case 6:
                        extentYPos = data & 0xff;
                        break;

                    default:
                        // TODO: Clipping/extent values
                        return;
                }

                return;
            }
            latchedValuesDetect -= 8;

            int spriteIndex = latchedValuesDetect / 0x0a;
            switch (latchedValuesDetect % 0x0a) {
                case 0:
                default: {
                    spritePalette[writingTo][spriteIndex] = data & 0xff;
                    break;
                }
                case 1: {
                    spriteY[writingTo][spriteIndex] = data & 0xff;
                    break;
                }
                case 2: {
                    spriteSizeY[writingTo][spriteIndex] = data & 0xff;
                    break;
                }
                case 3: {
                    spriteX[writingTo][spriteIndex] = data & 0xff;
                    break;
                }
                case 4: {
                    spriteSizeX[writingTo][spriteIndex] = data & 0xff;
                    break;
                }
                case 5: {
                    spriteAddress[writingTo][spriteIndex] = (spriteAddress[writingTo][spriteIndex] & 0xff00) | (data & 0xff);
                    break;
                }
                case 6: {
                    spriteAddress[writingTo][spriteIndex] = (spriteAddress[writingTo][spriteIndex] & 0xff) | ((data & 0xff) << 8);
                    break;
                }
                case 7: {
                    spriteScaleYInv[writingTo][spriteIndex] = data & 0xff;
                    break;
                }
                case 8: {
                    spriteScaleXInv[writingTo][spriteIndex] = data & 0xff;
                    break;
                }
                case 9: {
                    spriteStride[writingTo][spriteIndex] = data & 0xff;
                    break;
                }
            }
        }

        // This selection logic is because the actual address line is used to select the memory, not a decoder
        if (addressExActive(addressEx, addressExPlane0)) {
            busContention = display.getBusContentionPixels();
            plane0[address] = data;
        }
    }

    @Override
    public void setAddressBus(int address, int addressEx) {
        memoryAsserted = false;
        if (addressExActive(addressEx, addressExRegisters) && address >= (addressRegisters) && address < (addressRegisters + 0x800)) {
//            memoryAsserted = true;
        }
        if (addressExActive(addressEx, addressExPlane0)) {
//            memoryAsserted = true;
        }
    }

    double clockAccumulator = 0.0;

    boolean reachedEndOfLine = false;

    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync, boolean _doLineStart, boolean enableLayer) {
        // Time to the rising edge of the _hSync
        if (!prevVSYNC && _vSync) {
            // Flip-flip in hardware
            onScreen = 1-onScreen;

//            System.out.println("Reached sprite: " + drawingSpriteIndex);
            drawingSpriteIndex = 0;
            drawingSpriteState = 0;
            reachedEndOfLine = false;

            if (triggerBufferSwap) {
                drawingWith = 1 - drawingWith;
                writingTo = 1 - writingTo;
                triggerBufferSwap = false;
            }
        }
        prevVSYNC = _vSync;
        // To emulate the longer delayed line start
        if (_doLineStart) {
            // Reset on low in hardware
        }
        if (!enableLayer) {
            // Reset on low in hardware
            drawingSpriteIndex = 0;
            drawingSpriteState = 0;
            reachedEndOfLine = false;
        }

        clockAccumulator += clockMultiplier;
        while (clockAccumulator >= 1.0) {
            if (!reachedEndOfLine) {
                handleSpriteSchedule(displayH, displayV);
            }
            clockAccumulator -= 1.0;
        }

        int fetchingH = (displayH + leftBorderAdjust) & 0x1ff;
        int fetchingV = (displayV + topBorderAdjust) & 0x1ff;
        // Output calculated data
        int finalPixel = calculatedFrames[onScreen][fetchingH + (fetchingV * 512)];
        // And progressively clear the output pixel, like the hardware does
        calculatedFrames[onScreen][fetchingH + (fetchingV * 512)] = 0;

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
            case 11:
            case 12:
            case 14:
            case 16:
            case 18:
            case 20:
            case 22:
            default:
                drawingSpriteState++;
                break;

            case 1:
                // Palette bits also used for X&Y MSB
                currentSpritePalette = spritePalette[drawingWith][drawingSpriteIndex];
                drawingSpriteState++;
                break;

            case 3:
                currentSpriteY = spriteY[drawingWith][drawingSpriteIndex] | ((currentSpritePalette & 0x20) << 3);
                drawingSpriteState++;
                break;

            case 5:
                currentSpriteSizeY = spriteSizeY[drawingWith][drawingSpriteIndex];
                drawingSpriteState++;
                break;

            case 6:
                // Test for end of list
                if (currentSpriteSizeY == 0) {
                    reachedEndOfLine = true;
                    return;
                }

                drawingSpriteState++;
                break;

            case 7:
                currentSpriteX = spriteX[drawingWith][drawingSpriteIndex] | ((currentSpritePalette & 0x10) << 4);
                drawingSpriteState++;
                break;

            case 9:
                currentSpriteSizeX = spriteSizeX[drawingWith][drawingSpriteIndex];
                drawingSpriteState++;
                break;

            case 13:
                currentSpriteAddress = spriteAddress[drawingWith][drawingSpriteIndex] << 1;
                drawingSpriteState++;
                break;

            case 15:
                currentSpriteScaleYInv = spriteScaleYInv[drawingWith][drawingSpriteIndex];
                drawingSpriteState++;
                break;

            case 17:
                currentSpriteScaleXInv = spriteScaleXInv[drawingWith][drawingSpriteIndex];
                currentSpriteYPixel = currentSpriteScaleYInv / 2;
                currentSpriteXPixel = currentSpriteScaleXInv / 2;

                drawingSpriteState++;
                break;

            case 19:
                currentSpriteStride = spriteStride[drawingWith][drawingSpriteIndex];
                drawingSpriteState++;
                break;

            case 21:
                // Lookup table in hardware
                updateSpriteRow();
                drawingSpriteState++;
                break;

            case 23:
                // If it is always going to be off the bottom edge...
                if ( ((currentSpriteY/2) & 0xff) >= extentYPos && (((currentSpriteY + currentSpriteSizeY)/2) & 0xff) >= extentYPos) {
                    advanceSprite();   // Must not trigger twice for this clock...
                    return;
                }
                // If it is always going to be off the right edge...
                if ( ((currentSpriteX/2) & 0xff) >= extentXPos && (((currentSpriteX + currentSpriteSizeX)/2) & 0xff) >= extentXPos) {
                    advanceSprite();   // Must not trigger twice for this clock...
                    return;
                }
                // If we start drawing from on the screen and move off the right edge, then advance to the next row
                if ( ((currentSpriteXWorking/2) & 0xff) >= extentXPos && ((currentSpriteX/2) & 0xff) < extentXPos) {
                    drawingSpriteState++;   // Must not trigger twice for this clock...
                    return;
                }

                int pixelX = (currentSpriteXPixel >> 5) & 0xff;

                // Drawing pixels...
                int theColour = 0;
                // Selector
                int internalAddress;
                // Selector
                if ((currentSpritePalette & 0x40) > 0) {
                    internalAddress = currentSpriteAddressWorking - pixelX + 1; // +1 Adjustment for address /2, sprite widths should always be even numbers, or rather sprite data aligned to two bytes
                } else {
                    internalAddress = currentSpriteAddressWorking + pixelX;
                }
                theColour = plane0[internalAddress & 0xffff];
                // Selector
                if ((internalAddress & 0x010000) != 0) {
                    theColour >>= 4;
                }
                theColour &= 0x0f;
                theColour |= ((currentSpritePalette & 0x0f) << 4);
//                theColour = 0x01; // Debug solid colour
//                System.out.println("X " + currentSpriteXWorking + " Y " + currentSpriteY);

                int currentSpriteXWorkingAddr = currentSpriteXWorking & 0x1ff;
                currentSpriteY = currentSpriteY & 0x1ff;
                if ((calculatedFrames[offScreen][(currentSpriteY * 512) + currentSpriteXWorkingAddr] & 0x0f) == 0) {
                    calculatedFrames[offScreen][(currentSpriteY * 512) + currentSpriteXWorkingAddr] = theColour;
                }

                // Update coordinates after pixel draw...
                currentSpriteXWorking++;
                currentSpriteXPixel += currentSpriteScaleXInv;

                currentSpriteSizeXWorking--;
                if (currentSpriteSizeXWorking <= 0) {
                    drawingSpriteState++;
                }

                if (currentSpriteSizeY <= 0) {
                    advanceSprite();   // Must not trigger twice for this clock...
//                    return;
                }

                break;

            case 24:
                // End of sprite row logic, in a separate state
                currentSpriteXPixel = currentSpriteScaleXInv / 2;
                currentSpriteSizeY--;
                currentSpriteY++;
                currentSpriteYPixel += currentSpriteScaleYInv;

                // Lookup table in hardware
                updateSpriteRow();

                drawingSpriteState--;   // Might need to be an adder load instead of memory reset

                if (currentSpriteSizeY <= 0) {
                    advanceSprite();
                    return;
                }
                break;
        }
    }

    private void updateSpriteRow() {
        // Selector
        if ((currentSpritePalette & 0x80) > 0) {
            currentSpriteAddressWorking = currentSpriteAddress - (((currentSpriteYPixel >> 5) & 0xff) * (currentSpriteStride + 1)); // Note: Add with +1 in hardware
        } else {
            currentSpriteAddressWorking = currentSpriteAddress + (((currentSpriteYPixel >> 5) & 0xff) * (currentSpriteStride + 1)); // Note: Add with +1 in hardware
        }
        currentSpriteXWorking = currentSpriteX;
        currentSpriteSizeXWorking = currentSpriteSizeX;
    }

    private void advanceSprite() {
        drawingSpriteState = 0;
        drawingSpriteIndex++;
        drawingSpriteIndex &= kNumSprites-1;
    }

    @Override
    public String getDebug() {
        String debug = "";
        // TODO: Layer usage information? How many pixels drawn, maximum sprites drawn?
        if (!debug.isEmpty()) {
            debug = "Sprites4: " + debug;
        }
        return debug;
    }

    public void randomiseData(Random rand) {
        randomiseHelper(rand, plane0);

        randomiseHelper(rand, calculatedFrames[0]);
        randomiseHelper(rand, calculatedFrames[1]);

        randomiseHelper(rand, spriteX);
        randomiseHelper(rand, spriteY);
        randomiseHelper(rand, spriteSizeX);
        randomiseHelper(rand, spriteSizeY);
        randomiseHelper(rand, spriteScaleXInv);
        randomiseHelper(rand, spriteScaleYInv);
        randomiseHelper(rand, spriteAddress);
        randomiseHelper(rand, spriteStride);
        randomiseHelper(rand, spritePalette);
    }
}
