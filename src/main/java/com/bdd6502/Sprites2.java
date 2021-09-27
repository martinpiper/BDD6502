package com.bdd6502;

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
    boolean spriteEnable = false;
    final int kNumSprites = 64;
    int spriteX[] = new int[kNumSprites];
    int spriteY[] = new int[kNumSprites];
    int spriteScaleExtentX[] = new int[kNumSprites];
    int spriteSizeY[] = new int[kNumSprites];
    int spriteScaleXInv[] = new int[kNumSprites];
    int spriteScaleYInv[] = new int[kNumSprites];
    int spriteFrame[] = new int[kNumSprites];
    int spritePalette[] = new int[kNumSprites];

    boolean prevMSBH = false;

    int drawingSpriteIndex = 0;
    int drawingSpriteState = 0;
    int currentSpriteX = 0 , currentSpriteY = 0 , currentSpriteFrame = 0 , currentSpritePalette = 0;
    int currentSpriteScaleX = 0 , currentSpriteScaleY = 0;
    int currentSpriteScaleXInv = 0 , currentSpriteScaleYInv = 0;
    int currentSpriteXPixel = 0;
    int currentSpriteYPixel = 0;
    int insideHeight = 0;
    int fetchingPixel = 0;
    int onScreen = 0;


    int calculatedRasters[][] = new int[2][512];

    public Sprites2() {
    }

    public Sprites2(int addressRegisters, int addressExPlane0) {
        assertThat(addressRegisters, is(greaterThanOrEqualTo(0x8000)));
        assertThat(addressRegisters, is(lessThan(0xc000)));
        assertThat(addressRegisters & 0x7ff, is(equalTo(0x0000)));
        this.addressRegisters = addressRegisters;
        this.addressExPlane0 = addressExPlane0;
    }

    @Override
    public void writeData(int address, int addressEx, byte data) {
        if (MemoryBus.addressActive(addressEx, addressExRegisters) && address == (addressRegisters + 0x100)) {
            if ((data & 0x01) > 0) {
                spriteEnable = true;
            } else {
                spriteEnable = false;
            }
        }

        if (MemoryBus.addressActive(addressEx, addressExRegisters) && address >= (addressRegisters + 0x200) && address < (addressRegisters + 0x400)) {
            busContention = display.getBusContentionPixels();
            int spriteIndex = (address - (addressRegisters + 0x200)) / 0x08;
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
        if (MemoryBus.addressActive(addressEx, addressExRegisters) && address >= (addressRegisters + 0x100) && address < (addressRegisters + 0x200)) {
//            memoryAsserted = true;
        }
        if (MemoryBus.addressActive(addressEx, addressExPlane0) && MemoryBus.addressActive(address, addressPlane0)) {
//            memoryAsserted = true;
        }
    }

    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync) {
        // Time to the rising edge of the MSB, displayV is stable for the whole scan line at this point.
        // Otherwise for any other position the hardware would need to latch and alternate, which actually is another option...
        boolean msbH = (displayH & 0x100) == 0x100;
        if (!spriteEnable || (!prevMSBH && msbH)) {
            fetchingPixel = 0;
            drawingSpriteIndex = 0;
            drawingSpriteState = 0;
        }
        prevMSBH = msbH;
        onScreen = displayV & 1;

        handleSpriteSchedule(displayH, displayV);

        // Output calculated data
        int finalPixel = calculatedRasters[onScreen][fetchingPixel];
        // And progressively clear the output pixel, like the hardware does
        calculatedRasters[onScreen][fetchingPixel++] = 0;

        return finalPixel;
    }

    void handleSpriteSchedule(int displayH, int displayV) {
        if(drawingSpriteIndex >= spriteScaleExtentX.length) {
            return;
        }
        int offScreen = 1 - onScreen;

        // Reading Y first gives time to calculate vertical extent and skip the sprite before drawing the span
        switch (drawingSpriteState) {
            case 0:
            case 2:
            case 4:
            case 6:
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
                currentSpriteY = spriteY[drawingSpriteIndex] | ((currentSpritePalette & 0x10) << 4);
                drawingSpriteState++;
                break;

            case 5:
                currentSpriteScaleY = spriteSizeY[drawingSpriteIndex];
                drawingSpriteState++;
                break;

            case 7:
                // Perform Y extent check, the wait is for the calculation to succeed due to multiply 16 (shift 4!!) lookup and add, and advance drawingSpriteIndex if it isn't going to be drawn
                insideHeight = (displayV - currentSpriteY);
                // Note, unsigned comparison with low bits
                insideHeight &= 0x1ff;
                if (insideHeight >= currentSpriteScaleY) {
                    drawingSpriteState = 0;
                    drawingSpriteIndex++;
                    return;
                }
                currentSpriteX = spriteX[drawingSpriteIndex] | ((currentSpritePalette & 0x20) << 3);
                drawingSpriteState++;
                break;

            case 9:
                currentSpriteScaleX = spriteScaleExtentX[drawingSpriteIndex];
                drawingSpriteState++;
                break;


            case 11:
                currentSpriteScaleYInv = spriteScaleYInv[drawingSpriteIndex];
                drawingSpriteState++;
                break;

            case 13:
                // Lookup table calculation
                currentSpriteYPixel = (currentSpriteScaleYInv/2) + (insideHeight * currentSpriteScaleYInv);

                currentSpriteScaleXInv = spriteScaleXInv[drawingSpriteIndex];
                currentSpriteXPixel = currentSpriteScaleXInv / 2;
                drawingSpriteState++;
                break;

            case 15:
                currentSpriteFrame = spriteFrame[drawingSpriteIndex];
                drawingSpriteState++;
                break;

            case 16:
                int pixelX = (currentSpriteXPixel >> 4) & 0x0f;
                int pixelY = (currentSpriteYPixel >> 4) & 0x0f;
                if ((currentSpritePalette & 0x80) > 0) {
                    pixelX = 15 - pixelX;
                }
                if ((currentSpritePalette & 0x40) > 0) {
                    pixelY = 15 - pixelY;
                }
                pixelX &= 0x0f;
                pixelY &= 0x0f;
                currentSpriteX = currentSpriteX & 0x1ff;
                // Drawing pixels...
                int theColour = 0;
                // Selector
                int internalFrame = (currentSpriteFrame & 0x3f)>>1;
                int internalAddress = (internalFrame * 0x100) + pixelX + (pixelY << 4);
                switch (currentSpriteFrame & 0xc0) {
                    case 0x00:
                    default:
                        theColour = plane0[internalAddress];
                        break;
                    case 0x40:
                        theColour = plane1[internalAddress];
                    break;
                    case 0x80:
                        theColour = plane2[internalAddress];
                        break;
                    case 0xc0:
                        theColour = plane3[internalAddress];
                        break;
                }
                // Selector
                if ((currentSpriteFrame & 0x01) > 0) {
                    theColour >>= 4;
                }
                theColour &= 0x0f;
                theColour |= ((currentSpritePalette & 0x0f) << 4);

                if ((theColour & 0x0f) != 0) {
                    calculatedRasters[offScreen][currentSpriteX] = theColour;
                }

                // Update coordinates after pixel draw...
                currentSpriteX++;
                currentSpriteXPixel += currentSpriteScaleXInv;
                // Can be a carry check...
                if ((currentSpriteXPixel >> 4) >= currentSpriteScaleX) {
                    drawingSpriteState = 0;
                    drawingSpriteIndex++;
                    return;
                }

                break;
        }
    }
}
