package com.bdd6502;

public class Mode7 extends DisplayLayer {
    int busContention = 0;
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

    public Mode7() {
    }

    public Mode7(int addressRegisters, int addressExRegisters, int addressMap, int addressExMap, int addressTiles0, int addressExTiles0, int addressTiles1, int addressExTiles1) {
        assert (addressExRegisters == 0x01);
        assert (addressExMap == addressExTiles0);
        assert (addressExTiles0 == addressExTiles1);
        this.addressRegisters = addressRegisters;
        this.addressExRegisters = addressExRegisters;
        this.addressMap = addressMap;
        this.addressExMap = addressExMap;
        this.addressTiles0 = addressTiles0;
        this.addressExTiles0 = addressExTiles0;
        this.addressTiles1 = addressTiles1;
        this.addressExTiles1 = addressExTiles1;
    }

    @Override
    public void writeData(int address, int addressEx, byte data) {
        if (DisplayBombJack.addressActive(addressEx, 0x01) && address == addressRegisters) {
            dx = (dx & 0xffff00) | (data & 0xff);
        }
        if (DisplayBombJack.addressActive(addressEx, 0x01) && address == addressRegisters + 0x01) {
            dx = (dx & 0xff00ff) | ((data & 0xff) << 8);
        }
        if (DisplayBombJack.addressActive(addressEx, 0x01) && address == addressRegisters + 0x02) {
            dx = (dx & 0x00ffff) | ((data & 0xff) << 16);
        }
        if (DisplayBombJack.addressActive(addressEx, 0x01) && address == addressRegisters + 0x03) {
            dxy = (dxy & 0xffff00) | (data & 0xff);
        }
        if (DisplayBombJack.addressActive(addressEx, 0x01) && address == addressRegisters + 0x04) {
            dxy = (dxy & 0xff00ff) | ((data & 0xff) << 8);
        }
        if (DisplayBombJack.addressActive(addressEx, 0x01) && address == addressRegisters + 0x05) {
            dxy = (dxy & 0x00ffff) | ((data & 0xff) << 16);
        }

        if (DisplayBombJack.addressActive(addressEx, 0x01) && address == addressRegisters + 0x06) {
            dy = (dy & 0xffff00) | (data & 0xff);
        }
        if (DisplayBombJack.addressActive(addressEx, 0x01) && address == addressRegisters + 0x07) {
            dy = (dy & 0xff00ff) | ((data & 0xff) << 8);
        }
        if (DisplayBombJack.addressActive(addressEx, 0x01) && address == addressRegisters + 0x08) {
            dy = (dy & 0x00ffff) | ((data & 0xff) << 16);
        }
        if (DisplayBombJack.addressActive(addressEx, 0x01) && address == addressRegisters + 0x09) {
            dyx = (dyx & 0xffff00) | (data & 0xff);
        }
        if (DisplayBombJack.addressActive(addressEx, 0x01) && address == addressRegisters + 0x0a) {
            dyx = (dyx & 0xff00ff) | ((data & 0xff) << 8);
        }
        if (DisplayBombJack.addressActive(addressEx, 0x01) && address == addressRegisters + 0x0b) {
            dyx = (dyx & 0x00ffff) | ((data & 0xff) << 16);
        }

        if (DisplayBombJack.addressActive(addressEx, 0x01) && address == addressRegisters + 0x0c) {
            xorg = (xorg & 0xffff00) | (data & 0xff);
        }
        if (DisplayBombJack.addressActive(addressEx, 0x01) && address == addressRegisters + 0x0d) {
            xorg = (xorg & 0xff00ff) | ((data & 0xff) << 8);
        }
        if (DisplayBombJack.addressActive(addressEx, 0x01) && address == addressRegisters + 0x0e) {
            xorg = (xorg & 0x00ffff) | ((data & 0xff) << 16);
        }
        if (DisplayBombJack.addressActive(addressEx, 0x01) && address == addressRegisters + 0x0f) {
            yorg = (yorg & 0xffff00) | (data & 0xff);
        }
        if (DisplayBombJack.addressActive(addressEx, 0x01) && address == addressRegisters + 0x10) {
            yorg = (yorg & 0xff00ff) | ((data & 0xff) << 8);
        }
        if (DisplayBombJack.addressActive(addressEx, 0x01) && address == addressRegisters + 0x11) {
            yorg = (yorg & 0x00ffff) | ((data & 0xff) << 16);
        }

        if (DisplayBombJack.addressActive(addressEx, 0x01) && address == addressRegisters + 0x14) {
            backgroundColour = data & 0xff;
        }

        // This selection logic is because the actual address line is used to select the memory, not a decoder
        if (DisplayBombJack.addressActive(addressEx, addressExMap) && DisplayBombJack.addressActive(address, addressMap)) {
            busContention = display.getBusContentionPixels();
            screenData[address & 0x1fff] = data;
        }
        if (DisplayBombJack.addressActive(addressEx, addressExTiles0) && DisplayBombJack.addressActive(address, addressTiles0)) {
            busContention = display.getBusContentionPixels();
            tiles[address & 0x1fff] = data;
        }
        if (DisplayBombJack.addressActive(addressEx, addressExTiles1) && DisplayBombJack.addressActive(address, addressTiles1)) {
            busContention = display.getBusContentionPixels();
            tiles[(address & 0x1fff) + 0x2000] = data;
        }
    }

    @Override
    public int calculatePixel(int displayH, int displayV, boolean _hSync, boolean _vSync) {
        x += dx;
        yx += dyx;

        if (!previousHSync && _hSync) {
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
        }

        int xo = x + xy + xorg;
        int yo = y + yx + yorg;

        previousHSync = _hSync;
        previousVSync = _vSync;

        displayH = xo >> 8;
        displayV = yo >> 8;

        // Clamp
        displayH &= 0x3ff;
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
        if (finalPixel == 0) {
            finalPixel = backgroundColour;
        }
        if (busContention > 0) {
            finalPixel = display.getRandomColouredPixel();
        }
        if (busContention > 0) {
            busContention--;
        }
        return finalPixel;
    }
}
