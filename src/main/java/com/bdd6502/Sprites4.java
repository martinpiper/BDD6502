package com.bdd6502;

import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class Sprites4 extends DisplayLayer {

    int addressRegisters = 0x8800, addressExRegisters = 0x01;
    int addressExPlane0 = 0x08;
    byte[] plane0 = new byte[0x10000];
    final int kNumSprites = 128;    // TODO: Could be more
    int[] spriteX = new int[kNumSprites];
    int[] spriteY = new int[kNumSprites];
    int[] spriteSizeX = new int[kNumSprites];
    int[] spriteSizeY = new int[kNumSprites];
    int[] spriteScaleXInv = new int[kNumSprites];
    int[] spriteScaleYInv = new int[kNumSprites];
    int[] spriteAddress = new int[kNumSprites];
    int[] spriteStride = new int[kNumSprites];
    int[] spritePalette = new int[kNumSprites];

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
    int fetchingPixel = 0;
    int onScreen = 0;
    double clockMultiplier = 1.0f;


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
            int spriteIndex = (address - addressRegisters) / 0x0a;
            switch ((address - addressRegisters) % 0x0a) {
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
                    spriteSizeX[spriteIndex] = data & 0xff;
                    break;
                }
                case 5: {
                    spriteAddress[spriteIndex] = (spriteAddress[spriteIndex] & 0xff00) | (data & 0xff);
                    break;
                }
                case 6: {
                    spriteAddress[spriteIndex] = (spriteAddress[spriteIndex] & 0xff) | ((data & 0xff) << 8);
                    break;
                }
                case 7: {
                    spriteScaleYInv[spriteIndex] = data & 0xff;
                    break;
                }
                case 8: {
                    spriteScaleXInv[spriteIndex] = data & 0xff;
                    break;
                }
                case 9: {
                    spriteStride[spriteIndex] = data & 0xff;
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
    boolean delayed_doLineStart1 = false;
    boolean delayed_doLineStart2 = false;

    boolean reachedEndOfLine = false;

    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync, boolean _doLineStart, boolean enableLayer) {
        // Time to the rising edge of the _hSync
        if (!prevVSYNC && _vSync) {
            // Flip-flip in hardware
            onScreen = 1-onScreen;

            drawingSpriteIndex = 0;
            drawingSpriteState = 0;
            reachedEndOfLine = false;
        }
        prevVSYNC = _vSync;
        // To emulate the longer delayed line start
        if (_doLineStart || delayed_doLineStart1 || delayed_doLineStart2) {
            // Reset on low in hardware
            fetchingPixel = 0;
        }
        delayed_doLineStart2 = delayed_doLineStart1;
        delayed_doLineStart1 = _doLineStart;
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

        // Output calculated data
        int finalPixel = calculatedFrames[onScreen][fetchingPixel + (displayV * 512)];
        // And progressively clear the output pixel, like the hardware does
        calculatedFrames[onScreen][fetchingPixel + (displayV * 512)] = 0;
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
                currentSpritePalette = spritePalette[drawingSpriteIndex];
                drawingSpriteState++;
                break;

            case 3:
                currentSpriteY = spriteY[drawingSpriteIndex] | ((currentSpritePalette & 0x20) << 3);
                drawingSpriteState++;
                break;

            case 5:
                currentSpriteSizeY = spriteSizeY[drawingSpriteIndex];
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
                currentSpriteX = spriteX[drawingSpriteIndex] | ((currentSpritePalette & 0x10) << 4);
                drawingSpriteState++;
                break;

            case 9:
                currentSpriteSizeX = spriteSizeX[drawingSpriteIndex];
                drawingSpriteState++;
                break;

            case 13:
                currentSpriteAddress = spriteAddress[drawingSpriteIndex] << 1;
                drawingSpriteState++;
                break;

            case 15:
                currentSpriteScaleYInv = spriteScaleYInv[drawingSpriteIndex];
                drawingSpriteState++;
                break;

            case 17:
                currentSpriteScaleXInv = spriteScaleXInv[drawingSpriteIndex];
                currentSpriteYPixel = currentSpriteScaleYInv / 2;
                currentSpriteXPixel = currentSpriteScaleXInv / 2;

                drawingSpriteState++;
                break;

            case 19:
                currentSpriteStride = spriteStride[drawingSpriteIndex];
                drawingSpriteState++;
                break;

            case 21:
                // Lookup table in hardware
                currentSpriteAddressWorking = currentSpriteAddress + (((currentSpriteYPixel >> 5) & 0xff) * (currentSpriteStride+1)); // Note: Add with +1 in hardware
                currentSpriteXWorking = currentSpriteX;
                currentSpriteSizeXWorking = currentSpriteSizeX;
                drawingSpriteState++;
                break;

            case 23:
                int pixelX = (currentSpriteXPixel >> 5) & 0xff;
                if ((currentSpritePalette & 0x40) > 0) {
// TODO: Flips                    pixelX = 31 - pixelX;
                }
                if ((currentSpritePalette & 0x80) > 0) {
// TODO: Flips                    pixelY = 31 - pixelY;
                }

                // Drawing pixels...
                int theColour = 0;
                // Selector
                int internalAddress = currentSpriteAddressWorking + pixelX;
                theColour = plane0[internalAddress & 0xffff];
                // Selector
                if ((internalAddress & 0x010000) != 0) {
                    theColour >>= 4;
                }
                theColour &= 0x0f;
                theColour |= ((currentSpritePalette & 0x0f) << 4);
//                theColour = 0x01; // Debug solid colour
//                System.out.println("X " + currentSpriteXWorking + " Y " + currentSpriteY);

                currentSpriteXWorking = currentSpriteXWorking & 0x1ff;
                currentSpriteY = currentSpriteY & 0x1ff;
                if ((calculatedFrames[offScreen][(currentSpriteY * 512) + currentSpriteXWorking] & 0x0f) == 0) {
                    calculatedFrames[offScreen][(currentSpriteY * 512) + currentSpriteXWorking] = theColour;
                }

                // Update coordinates after pixel draw...
                currentSpriteXWorking++;
                currentSpriteXPixel += currentSpriteScaleXInv;

                currentSpriteSizeXWorking--;
                if (currentSpriteSizeXWorking <= 0) {
                    drawingSpriteState++;
                }

                if (currentSpriteSizeY <= 0) {
                    advanceSprite();
                    return;
                }

                break;

            case 24:
                // End of sprite row logic, in a separate state
                currentSpriteXPixel = currentSpriteScaleXInv / 2;
                currentSpriteSizeY--;
                currentSpriteY++;
                currentSpriteYPixel += currentSpriteScaleYInv;

                // Lookup table in hardware
                currentSpriteAddressWorking = currentSpriteAddress + (((currentSpriteYPixel >> 5) & 0xff) * (currentSpriteStride+1)); // Note: Add with +1 in hardware
                currentSpriteXWorking = currentSpriteX;
                currentSpriteSizeXWorking = currentSpriteSizeX;

                drawingSpriteState--;   // Might need to be an adder load instead of memory reset

                if (currentSpriteSizeY <= 0) {
                    advanceSprite();
                    return;
                }
                break;
        }
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
