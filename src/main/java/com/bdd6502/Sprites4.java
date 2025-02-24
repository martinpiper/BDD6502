package com.bdd6502;

import com.loomcom.symon.util.HexUtil;

import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class Sprites4 extends DisplayLayer {

    int addressRegisters = 0x8800, addressExRegisters = 0x01;
    int addressExPlane0 = 0x04;
    byte[] plane0 = new byte[0x100000]; // 1 MB is a sensible (ish) memory size based on hardware cost, single IC IS62C10248AL
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

    boolean prevVBlank = false;

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
    int highestSpriteSubmitted = 0;
    int lasthighestSpriteSubmitted = 0;
    int register0 = 0;
    boolean register0_calculationEnable = false;
    int leftBorderAdjust = 0;
    int topBorderAdjust = 0;
    int extentXPos = 255 , extentYPos = 255;
    int addressExtra = 0;


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
        if (addressExActive(addressEx, addressExRegisters) && address >= (addressRegisters) && address < (addressRegisters + 0x800)) {
//            System.out.println("writeData "+ HexUtil.byteToHex(data) + " to " + HexUtil.wordToHex(address) +" Reached sprite: " + drawingSpriteIndex + " reachedEndOfList " + reachedEndOfList + " triggerBufferSwap " + triggerBufferSwap + " drawingWith " + drawingWith + " writingTo " + writingTo);

            int latchedValuesDetect = address - addressRegisters;
            if (latchedValuesDetect < 8) {
                switch (latchedValuesDetect) {
                    case 0:
                        if ((register0 & 0x01) == 0x00 && (data & 0x01) == 0x01)
                        {
//                            System.out.println("** Set triggerBufferSwap : Reached sprite: " + drawingSpriteIndex + " reachedEndOfList " + reachedEndOfList + " triggerBufferSwap " + triggerBufferSwap + " drawingWith " + drawingWith + " writingTo " + writingTo);
                            triggerBufferSwap = true;
                        }
                        if ((data & 0x02) == 0x02)
                        {
                            register0_calculationEnable = true;
                        } else {
                            register0_calculationEnable = false;
                        }
                        register0 = data & 0xff;
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
                    case 7:
                        addressExtra = data & 0xff;
                        break;

                    default:
                        return;
                }

                return;
            }
            latchedValuesDetect -= 8;

            int spriteIndex = latchedValuesDetect / 0x0b;
            if (spriteIndex > highestSpriteSubmitted) {
                highestSpriteSubmitted = spriteIndex;
            }
            switch (latchedValuesDetect % 0x0b) {
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
                    spriteAddress[writingTo][spriteIndex] = (spriteAddress[writingTo][spriteIndex] & 0xffff00) | (data & 0xff);
                    break;
                }
                case 6: {
                    spriteAddress[writingTo][spriteIndex] = (spriteAddress[writingTo][spriteIndex] & 0xff00ff) | ((data & 0xff) << 8);
                    break;
                }
                case 7: {
                    spriteAddress[writingTo][spriteIndex] = (spriteAddress[writingTo][spriteIndex] & 0x00ffff) | ((data & 0xff) << 16);
                    break;
                }
                case 8: {
                    spriteScaleYInv[writingTo][spriteIndex] = data & 0xff;
                    break;
                }
                case 9: {
                    spriteScaleXInv[writingTo][spriteIndex] = data & 0xff;
                    break;
                }
                case 0xa: {
                    spriteStride[writingTo][spriteIndex] = data & 0xff;
                    break;
                }
            }
        }

        // This selection logic is because the actual address line is used to select the memory, not a decoder
        if (addressExActive(addressEx, addressExPlane0)) {
            busContention = display.getBusContentionPixels();
            plane0[address + (addressExtra << 16)] = data;
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

    boolean reachedEndOfList = false;

    int lastdrawingSpriteIndex = 0;
    int lastdrawingSpriteState = 0;
    boolean lastreachedEndOfList = false;

    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync, boolean _doLineStart, boolean enableLayer, boolean vBlank) {
        // Note: _vSync *is not* the same as vBlank which is tied to extEXTWANTIRQFlag in hardware
        if (!prevVBlank && vBlank) {
            // Flip-flop in hardware
            onScreen = 1-onScreen;

//            System.out.println("_vSync Reached sprite: " + drawingSpriteIndex + " reachedEndOfList " + reachedEndOfList + " triggerBufferSwap " + triggerBufferSwap + " drawingWith " + drawingWith + " writingTo " + writingTo);
            lastdrawingSpriteIndex = drawingSpriteIndex;
            lastdrawingSpriteState = drawingSpriteState;
            lastreachedEndOfList = reachedEndOfList;

            drawingSpriteIndex = 0;
            drawingSpriteState = 0;
            reachedEndOfList = false;

            if (triggerBufferSwap) {
                drawingWith = 1 - drawingWith;
                writingTo = 1 - drawingWith;
                triggerBufferSwap = false;
                lasthighestSpriteSubmitted = highestSpriteSubmitted;
                highestSpriteSubmitted = 0;
            }
        }
        prevVBlank = vBlank;
        // To emulate the longer delayed line start
        if (_doLineStart) {
            // Reset on low in hardware
        }
        if (!register0_calculationEnable) {
            // Reset on low in hardware
            drawingSpriteIndex = 0;
            drawingSpriteState = 0;
            reachedEndOfList = false;
        }

        clockAccumulator += clockMultiplier;
        while (clockAccumulator >= 1.0) {
            if (!reachedEndOfList) {
                handleSpriteSchedule(displayH, displayV);
            }
            clockAccumulator -= 1.0;
        }

        int fetchingH = (displayH + leftBorderAdjust - 1) & 0x1ff;
        int fetchingV = (displayV + topBorderAdjust) & 0x1ff;
        // Output calculated data
        int finalPixel = calculatedFrames[onScreen][fetchingH + (fetchingV * 512)];
        // And progressively clear the output pixel, like the hardware does
        calculatedFrames[onScreen][fetchingH + (fetchingV * 512)] = 0;

        if (displayH < 2) {
            finalPixel = display.getContentionColouredPixel();
        }

        if (!enableLayer) {
            return 0;
        }

        return finalPixel;
    }

    void handleSpriteSchedule(int displayH, int displayV) {
        int offScreen = 1 - onScreen;

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
                    reachedEndOfList = true;
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
                // Matching the hardware special cases
                if (currentSpriteSizeX == 0) {
                    currentSpriteSizeX = 256;
                }
                if (currentSpriteSizeX == 1) {
                    currentSpriteSizeX = 257;
                }
                break;

            case 13:
                currentSpriteAddress = spriteAddress[drawingWith][drawingSpriteIndex];
                drawingSpriteState++;
                // TODO: This is actually 6 clocks on the hardware, since it is 3 x 8 bit loads from RAM
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
                // Equivalent to hardware: S4_JustBeforeDrawingPixels
                // If it is always going to be off the right edge...
                // Note: Not using currentSpriteXWorking and currentSpriteSizeXWorking
                if ( ((currentSpriteX/2) & 0xff) >= extentXPos && (((currentSpriteX + currentSpriteSizeX)/2) & 0xff) >= extentXPos) {
                    advanceSprite();   // Must not trigger twice for this clock...
                    return;
                }

                // Lookup table in hardware
                updateSpriteRow();
                drawingSpriteState++;
                break;

            case 23:
                // Equivalent to hardware: S4_EnableXPosCount
                // If it is always going to be off the bottom edge...
                if ( ((currentSpriteY/2) & 0xff) >= extentYPos && (((currentSpriteY + currentSpriteSizeY)/2) & 0xff) >= extentYPos) {
                    advanceSprite();   // Must not trigger twice for this clock...
                    return;
                }

                int pixelX = (currentSpriteXPixel >> 5) & 0xff;

                // Drawing pixels...
                int theColour = 0;
                // Selector
                int internalAddress;
                // Selector
                if ((currentSpritePalette & 0x40) > 0) {
                    internalAddress = currentSpriteAddressWorking - pixelX;
                } else {
                    internalAddress = currentSpriteAddressWorking + pixelX;
                }
                theColour = plane0[(internalAddress>>1) & 0xfffff];
                // Selector
                if ((internalAddress & 0x1) != 0) {
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

                // If we start drawing from on the screen and move off the right edge, then advance to the next row
                // NOTE: Testing before the currentSpriteXWorking++ emulates the pixel clock one cycle latency for this test result
                if ( ((currentSpriteXWorking/2) & 0xff) >= extentXPos && ((currentSpriteX/2) & 0xff) < extentXPos) {
                    drawingSpriteState++;   // Must not trigger twice for this clock...
                    return;
                }

                // Update coordinates after pixel draw...
                currentSpriteXWorking++;
                currentSpriteXPixel += currentSpriteScaleXInv;

                currentSpriteSizeXWorking--;
                if (currentSpriteSizeXWorking == 0) {
                    drawingSpriteState++;   // Must not trigger twice for this clock...
                    return;
                }

                if (currentSpriteSizeY <= 0) {
                    advanceSprite();   // Must not trigger twice for this clock...
                    return;
                }

                break;

            case 24:
                // End of sprite row logic, in a separate state
                currentSpriteXPixel = currentSpriteScaleXInv / 2;
                currentSpriteSizeY--;
                currentSpriteY++;
                currentSpriteYPixel += currentSpriteScaleYInv;

                drawingSpriteState = 21;   // Might need to be an adder load instead of memory reset

                if (currentSpriteSizeY <= 0) {
                    advanceSprite();
                    return;
                }
                break;
        }
    }

    private void updateSpriteRow() {
        // Lookup table in hardware, 8 bit * 8 bit
        int multiply = ((currentSpriteYPixel >> 5) & 0xff) * (currentSpriteStride + 1); // Note: Add with +1 in hardware
        // Selector
        if ((currentSpritePalette & 0x80) > 0) {
            currentSpriteAddressWorking = currentSpriteAddress - multiply;
        } else {
            currentSpriteAddressWorking = currentSpriteAddress + multiply;
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
        String debug = "Sprites4: leftBorderAdjust " + HexUtil.wordToHex(leftBorderAdjust) + " topBorderAdjust " + HexUtil.wordToHex(topBorderAdjust)
                + " extentXPos " + HexUtil.byteToHex(extentXPos) + " extentYPos " + HexUtil.byteToHex(extentYPos) + " addressExtra " + HexUtil.byteToHex(addressExtra) + "\r";
        debug += "Sprites4: Current sprite: " + drawingSpriteIndex + " reachedEndOfList " + reachedEndOfList + " highest sprite " + highestSpriteSubmitted + " triggerBufferSwap " + triggerBufferSwap + "\r";
        debug += "Sprites4: Previous frame sprite: " + lastdrawingSpriteIndex + " highest sprite " + lasthighestSpriteSubmitted + " reachedEndOfList " + lastreachedEndOfList + "\r";

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
