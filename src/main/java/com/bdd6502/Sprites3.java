package com.bdd6502;

import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class Sprites3 extends DisplayLayer {

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
    int spriteRotation[] = new int[kNumSprites];
    int spriteEdgeFlags[] = new int[kNumSprites];

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


    int calculatedRasters[][] = new int[2][512];

    public Sprites3() {
    }

    public Sprites3(int addressRegisters, int addressExPlane0) {
        assertThat(addressRegisters, is(greaterThanOrEqualTo(0x8000)));
        assertThat(addressRegisters, is(lessThan(0xc000)));
        assertThat(addressRegisters & 0x7ff, is(equalTo(0x200)));
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
        if (addressExActive(addressEx, addressExRegisters) && address >= (addressRegisters + 0x00) && address < (addressRegisters + 0x200)) {
            busContention = display.getBusContentionPixels();
            int spriteIndex = (address - addressRegisters) / 0x08;
            switch (address % 0x08) {
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
                case 8: {
                    spriteRotation[spriteIndex] = data & 0xff;
                    break;
                }
                case 9: {
                    spriteEdgeFlags[spriteIndex] = data & 0xff;
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
        if (addressExActive(addressEx, addressExRegisters) && address >= (addressRegisters + 0x00) && address < (addressRegisters + 0x200)) {
//            memoryAsserted = true;
        }
        if (addressExActive(addressEx, addressExPlane0) && MemoryBus.addressActive(address, addressPlane0)) {
//            memoryAsserted = true;
        }
    }

    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync, boolean _doLineStart, boolean enableLayer) {
        // Time to the rising edge of the _hSync
        if (!prevHSYNC && _hSync) {
            // Flip-flip in hardware
            onScreen = 1-onScreen;
        }
        prevHSYNC = _hSync;
        if (_doLineStart) {
            // Reset on low in hardware
            fetchingPixel = 0;
            drawingSpriteIndex = 0;
            drawingSpriteState = 0;
        }
        if (!enableLayer) {
            // Reset on low in hardware
            drawingSpriteIndex = 0;
            drawingSpriteState = 0;
        }


        handleSpriteSchedule(displayH, displayV);

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
                int pixelX = (currentSpriteXPixel >> 3) & 0x7f;
                int pixelY = (currentSpriteYPixel >> 3) & 0x7f;

                double tempRot = Math.toRadians(display.getFrameNumberForSync() * (drawingSpriteIndex / 2));
                if ((drawingSpriteIndex & 1) == 1) {
                    tempRot = Math.toRadians(-display.getFrameNumberForSync() / drawingSpriteIndex);
                }

                final double root2 = Math.sqrt(2.0f);
                // Note, reverse the rotation for the origin
                pixelX += (64.0f * Math.cos(-tempRot) - 64.0f * Math.sin(-tempRot)) / root2;
                pixelY += (64.0f * Math.sin(-tempRot) + 64.0f * Math.cos(-tempRot)) / root2;

                pixelX -= 64.0f;
                pixelY -= 64.0f;

                int rotpixelX = (int)((((double)pixelX * Math.cos(tempRot)) - ((double)pixelY * Math.sin(tempRot))) * root2);
                int rotpixelY = (int)((((double)pixelX * Math.sin(tempRot)) + ((double)pixelY * Math.cos(tempRot))) * root2);

                pixelX = rotpixelX >> 2;
                pixelY = rotpixelY >> 2;
                if (pixelX >= 0 && pixelX < 32 && pixelY >= 0 && pixelY < 32) {
                    pixelX &= 0x1f;
                    pixelY &= 0x1f;

                    if ((currentSpritePalette & 0x40) > 0) {
                        pixelX = 31 - pixelX;
                    }
                    if ((currentSpritePalette & 0x80) > 0) {
                        pixelY = 31 - pixelY;
                    }
                    // Starts drawing from the opposite half of the coordinate range first
                    if ((currentSpriteFrame & 0x40) > 0) {
                        pixelX ^= 0x10;
                    }
                    if ((currentSpriteFrame & 0x80) > 0) {
                        pixelY ^= 0x10;
                    }
                    pixelX &= 0x1f;
                    pixelY &= 0x1f;
                    currentSpriteX = currentSpriteX & 0x1ff;
                    // Drawing pixels...
                    int theColour = 0;
                    // Selector
                    int internalFrame = (currentSpriteFrame & 0x0f) >> 1;
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

        randomiseHelper(rand, spriteRotation);
        randomiseHelper(rand, spriteEdgeFlags);
    }
}
