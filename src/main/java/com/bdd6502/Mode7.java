package com.bdd6502;

import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class Mode7 extends DisplayLayer {
    int addressRegisters = 0xa000, addressExRegisters = 0x01;
    int addressMap = 0x2000, addressExMap = 0x08;
    int addressTiles0 = 0x4000, addressExTiles0 = 0x08;
    int addressTiles1 = 0x8000, addressExTiles1 = 0x08;
    byte screenData[] = new byte[0x2000];
    byte tiles[] = new byte[0x4000];
    int dx = 0, dxy = 0;
    int dy = 0, dyx = 0;
    int xorg = 0, yorg = 0;
    int backgroundColour = 0;
    boolean previousHSync = false, previousVSync = false;
    int x = 0, y = 0;
    int xy = 0, yx = 0;
    // New default state
    boolean flagDisplayEnable = false;
    boolean flagRegisterX = false;
    boolean flagRegisterXY = false;
    boolean flagRegisterY = false;
    boolean flagRegisterYX = false;

    public Mode7() {
    }

    public Mode7(int addressRegisters, int addressExMap) {
        assertThat(addressRegisters, is(greaterThanOrEqualTo(0x8000)));
        assertThat(addressRegisters, is(lessThan(0xc000)));
        assertThat(addressRegisters & 0x7ff, is(equalTo(0x00)));
        this.addressRegisters = addressRegisters;
        this.addressExMap = addressExMap;
        this.addressExTiles0 = addressExMap;
        this.addressExTiles1 = addressExMap;
    }

    @Override
    public void writeData(int address, int addressEx, byte data) {
        if (addressExActive(addressEx, addressExRegisters)) {
            if (address == addressRegisters) {
                dx = (dx & 0xffff00) | (data & 0xff);
            }
            if (address == addressRegisters + 0x01) {
                dx = (dx & 0xff00ff) | ((data & 0xff) << 8);
            }
            if (address == addressRegisters + 0x02) {
                dx = (dx & 0x00ffff) | ((data & 0xff) << 16);
            }
            if (address == addressRegisters + 0x03) {
                dxy = (dxy & 0xffff00) | (data & 0xff);
            }
            if (address == addressRegisters + 0x04) {
                dxy = (dxy & 0xff00ff) | ((data & 0xff) << 8);
            }
            if (address == addressRegisters + 0x05) {
                dxy = (dxy & 0x00ffff) | ((data & 0xff) << 16);
            }

            if (address == addressRegisters + 0x06) {
                dy = (dy & 0xffff00) | (data & 0xff);
            }
            if (address == addressRegisters + 0x07) {
                dy = (dy & 0xff00ff) | ((data & 0xff) << 8);
            }
            if (address == addressRegisters + 0x08) {
                dy = (dy & 0x00ffff) | ((data & 0xff) << 16);
            }
            if (address == addressRegisters + 0x09) {
                dyx = (dyx & 0xffff00) | (data & 0xff);
            }
            if (address == addressRegisters + 0x0a) {
                dyx = (dyx & 0xff00ff) | ((data & 0xff) << 8);
            }
            if (address == addressRegisters + 0x0b) {
                dyx = (dyx & 0x00ffff) | ((data & 0xff) << 16);
            }

            if (address == addressRegisters + 0x0c) {
                xorg = (xorg & 0xffff00) | (data & 0xff);
            }
            if (address == addressRegisters + 0x0d) {
                xorg = (xorg & 0xff00ff) | ((data & 0xff) << 8);
            }
            if (address == addressRegisters + 0x0e) {
                xorg = (xorg & 0x00ffff) | ((data & 0xff) << 16);
            }
            if (address == addressRegisters + 0x0f) {
                yorg = (yorg & 0xffff00) | (data & 0xff);
            }
            if (address == addressRegisters + 0x10) {
                yorg = (yorg & 0xff00ff) | ((data & 0xff) << 8);
            }
            if (address == addressRegisters + 0x11) {
                yorg = (yorg & 0x00ffff) | ((data & 0xff) << 16);
            }

            if (address == addressRegisters + 0x14) {
                backgroundColour = data & 0xff;
            }

            if (address == addressRegisters + 0x15) {
                flagRegisterX = false;
                flagRegisterXY = false;
                flagRegisterY = false;
                flagRegisterYX = false;
                if (!withOverscan) {
                    flagDisplayEnable = false;
                    if ((data & 0x01) > 0) {
                        flagDisplayEnable = true;
                    }
                    if ((data & 0x02) > 0) {
                        flagRegisterX = true;
                    }
                    if ((data & 0x04) > 0) {
                        flagRegisterXY = true;
                    }
                    if ((data & 0x08) > 0) {
                        flagRegisterY = true;
                    }
                    if ((data & 0x10) > 0) {
                        flagRegisterYX = true;
                    }
                } else {
                    if ((data & 0x01) > 0) {
                        flagRegisterX = true;
                    }
                    if ((data & 0x02) > 0) {
                        flagRegisterXY = true;
                    }
                    if ((data & 0x04) > 0) {
                        flagRegisterY = true;
                    }
                    if ((data & 0x08) > 0) {
                        flagRegisterYX = true;
                    }
                }
                handleRegisterFlags();
            }
        }

        // This selection logic is because the actual address line is used to select the memory, not a decoder
        if (addressExActive(addressEx, addressExMap) && MemoryBus.addressActive(address, addressMap)) {
            busContention = display.getBusContentionPixels();
            screenData[address & 0x1fff] = data;
        }
        if (addressExActive(addressEx, addressExTiles0) && MemoryBus.addressActive(address, addressTiles0)) {
            busContention = display.getBusContentionPixels();
            tiles[address & 0x1fff] = data;
        }
        if (addressExActive(addressEx, addressExTiles1) && MemoryBus.addressActive(address, addressTiles1)) {
            busContention = display.getBusContentionPixels();
            tiles[(address & 0x1fff) + 0x2000] = data;
        }
    }

    @Override
    public void setAddressBus(int address, int addressEx) {
        memoryAsserted = false;
        if (addressExActive(addressEx, addressExMap) && MemoryBus.addressActive(address, addressMap)) {
//            memoryAsserted = true;
        }
        if (addressExActive(addressEx, addressExTiles0) && MemoryBus.addressActive(address, addressTiles0)) {
//            memoryAsserted = true;
        }
        if (addressExActive(addressEx, addressExTiles1) && MemoryBus.addressActive(address, addressTiles1)) {
//            memoryAsserted = true;
        }
    }

    int finalPixelDelay0 , finalPixelDelay1 , finalPixelDelay2 , finalPixelDelay3 , finalPixelDelay4;
    int _vSyncDelay = 0;
    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync, boolean _doLineStart, boolean enableLayer, boolean vBlank) {
        if (withOverscan) {
            flagDisplayEnable = enableLayer;
        }

        x += dx;
        yx += dyx;

        if (!previousHSync && _hSync) {
            if (_vSyncDelay > 0) {
                // This models observed simulation behaviour, the cause may be the timing between _vsync and _hsync pulses...
                y = 0;
                xy = 0;
                _vSyncDelay--;
            }
            xy += dxy;
            y += dy;
        }
        if (_hSync == false) {
            yx = 0;
            x = 0;
        }
        if (_vSync == false) {
            y = 0;
            xy = 0;
            _vSyncDelay = 2;
        }

        handleRegisterFlags();

        int xo = x + xy + xorg;
        int yo = y + yx + yorg;

        previousHSync = _hSync;
        previousVSync = _vSync;

        displayH = xo >> 8;
        displayV = yo >> 8;

        // Clamp
        displayH &= 0x7ff;
        displayV &= 0x3ff;
        int index = ((displayH >> 4) & 0x7f) + (((displayV >> 4) & 0x3f) * 0x80);
        int theChar = (screenData[index]) & 0xff;
//        System.out.println(displayH + " " + displayV + " Chars index: " + Integer.toHexString(index) + " char " + Integer.toHexString(theChar));
        displayH &= 0x0f;
        displayV &= 0x0f;
        // Include flips
        if ((theChar & 0x40) > 0) {
            displayH = 0x0f - displayH;
        }
        if ((theChar & 0x80) > 0) {
            displayV = 0x0f - displayV;
        }
        theChar &= 0x3f;
        int finalPixel = tiles[(theChar << 8) + (displayV * 0x10) + displayH];
        if (finalPixel == 0 || flagDisplayEnable == false) {
            finalPixel = backgroundColour;
        }
        finalPixelDelay0 = finalPixelDelay1;
        finalPixelDelay1 = finalPixelDelay2;
        finalPixelDelay2 = finalPixelDelay3;
        finalPixelDelay3 = finalPixelDelay4;
        finalPixelDelay4 = finalPixel;
        return getByteOrContention(finalPixelDelay0);
    }

    public void handleRegisterFlags() {
        if (!flagRegisterX) {
            x = 0;
        }
        if (!flagRegisterXY) {
            xy = 0;
        }
        if (!flagRegisterY) {
            y = 0;
        }
        if (!flagRegisterYX) {
            yx = 0;
        }
    }

    public void randomiseData(Random rand) {
        randomiseHelper(rand , screenData);
        randomiseHelper(rand , tiles);

        dx = rand.nextInt();
        dxy = rand.nextInt();

        dy = rand.nextInt();
        dyx = rand.nextInt();

        xorg = rand.nextInt();
        yorg = rand.nextInt();

        backgroundColour = rand.nextInt();

        flagDisplayEnable = rand.nextBoolean();
        flagRegisterX = rand.nextBoolean();
        flagRegisterY = rand.nextBoolean();
        flagRegisterXY = rand.nextBoolean();
        flagRegisterYX = rand.nextBoolean();
    }
}
