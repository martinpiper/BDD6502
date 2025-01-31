package com.bdd6502;

import com.loomcom.symon.devices.UserPortTo24BitAddress;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class DisplayBombJack extends MemoryBus {

    DisplayMainFrame window;
    QuickDrawPanel panel;

    ArrayList<DisplayLayer> layers = new ArrayList<>();
    DisplayLayer layersRaw[] = new DisplayLayer[0];
    boolean enableLayerFlags[] = new boolean[0];

    int frameNumber = 0;
    final int displayWidth = 384;
    final int displayHeight = 264;
    private int latchedPixelFromWhere = -1;
    private int numPaletteBanks = 0;
    private int paletteBank = 0;

    public void setDebugDisplayPixels(boolean debugDisplayPixels) {
        this.debugDisplayPixels = debugDisplayPixels;
    }

    public boolean isDebugDisplayPixels() {
        return debugDisplayPixels;
    }

    boolean debugDisplayPixels = false;
    int debugDisplayPixel[] = new int[pixelsInWholeFrame()];
    int debugDisplayPixelRGB[] = new int[pixelsInWholeFrame()];
    int debugDisplayPixelFromWhere[] = new int[pixelsInWholeFrame()];

    int busContentionPalette = 0;
    int addressPalette = 0x9c00, addressExPalette = 0x01;
    int addressRegisters = 0x9e00, addressExRegisters = 0x01;
    int displayPriority = 0;  // Default to be 0, this helps ensure startup code correctly sets this option
    int lineStartTimeDelay = 0;

    public boolean isWithOverscan() {
        return withOverscan;
    }

    public void setWithOverscan(boolean withOverscan) {
        this.withOverscan = withOverscan;
    }

    boolean withOverscan = false;

    public int getFrameNumberForSync() {
        return frameNumberForSync;
    }

    int frameNumberForSync = 0;

    public int getDisplayH() {
        return displayHExternal;
    }

    public int getDisplayV() {
        return displayVExternal;
    }

    public int getDisplayX(int cia1RasterOffsetX) {
        return displayX + cia1RasterOffsetX;
    }

    public int getDisplayYForCIA(int cia1RasterOffsetY) {
        return ((displayHeight - (displayY + cia1RasterOffsetY)) - 16) & 0x1ff;   // These tweak values are aligned with using Video_StartRasterTimers immediately after using Video_WaitVBlank
    }

    public boolean is_hSync() {
        return _hSync;
    }

    public boolean is_vSync() {
        return _vSync;
    }

    int displayH = 0, displayV = 0;
    int displayHExternal = 0, displayVExternal = 0;
    int displayX = 0, displayY = 0;
    int displayBitmapX = 0, displayBitmapY = 0;
    boolean enablePixels = false;
    boolean borderX = true, borderY = true;

    public boolean isEnableDisplay() {
        return enableDisplay;
    }

    boolean enableDisplay = false;  // Default to be display off, this helps ensure startup code correctly sets this option
    boolean enableBackground = false;
    int backgroundColour = 0;
    int latchedPixel = 0;
    int palette[][] = new int[256][256];
    byte paletteMemory[][] = new byte[256][512];
    int paletteBitsRed = 4;
    int paletteBitsGreen = 4;
    int paletteBitsBlue = 4;
    Random random = new Random();
    String leafFilename = null;
    int lastDataWritten = 0;
    boolean vBlank = false;
    boolean _hSync = true, _vSync = true;
    boolean extEXTWANTIRQFlag = false;
    PrintWriter debugData = null;
    int pixelsSinceLastDebugWrite = 0;
    int pixelsSinceLastDebugWriteMax = 16;
    boolean is16Colours = false;
    UserPortTo24BitAddress callbackUserPort = null;
    int overscanBorderExtent = 0;

    public boolean getVSync() {
        return _vSync;
    }

    public boolean getVBlank() {
        return vBlank;
    }

    @Override
    public boolean extEXTWANTIRQ() {
        return extEXTWANTIRQFlag;
    }

    @Override
    public void resetExtEXTWANTIRQ() {
        extEXTWANTIRQFlag = false;
    }

    public void calculatePixelsUntilEXTWANTIRQ() {
        while (!extEXTWANTIRQFlag) {
            calculatePixel();
        }
    }

    public DisplayBombJack() throws IOException {
        //enableDebugData();
        Arrays.fill(debugDisplayPixelFromWhere, -1);
    }

    public void make16Colours() {
        is16Colours = true;
    }

    public void enableDebugData() throws IOException {
        debugData = new PrintWriter(new FileWriter("target/debugData.txt"));
        debugData.println("; Automatically created by DisplayBombJack");
        debugData.println("d0");
    }

    public void setCallbackUserPort(UserPortTo24BitAddress userPort) {
        callbackUserPort = userPort;
    }

    public int getBusContentionPixels() {
        return 0x08;
    }

    public void InitWindow() {
        double scale = 2.0f;
        InitWindow((int)(displayWidth * scale), (int)(displayHeight * scale));
    }

    public void InitWindow(int width, int height) {
        // Testing window drawing in a loop for eventual graphics updates
        window = new DisplayMainFrame(this);
        window.addKeyListener(window);
        window.addMouseMotionListener(window);
        //window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.getContentPane().setPreferredSize(new Dimension(width, height));
        window.pack();

        panel = new QuickDrawPanel(displayWidth, displayHeight);
        window.add(panel);

        window.setVisible(true);
        window.setTitle("Press 'P' to toggle debug pixel picking");
    }

    public void RepaintWindow() {
        // Calculate a scale that fits the display without compromising the aspect ratio
        double scaleX = (double) window.getContentPane().getWidth() / (double) displayWidth;
        double scaleY = (double) window.getContentPane().getHeight() / (double) displayHeight;
        if (scaleX < scaleY) {
            panel.size.setSize(scaleX * displayWidth, scaleX * displayHeight);
        } else {
            panel.size.setSize(scaleY * displayWidth, scaleY * displayHeight);
        }

        window.repaint();
    }

    public void HandlePixelPick() {
        if (debugDisplayPixels && null != panel && window != null) {
            Point pos = panel.getMousePosition();
            if (null != pos) {

                int realX = (pos.x * displayWidth) / panel.getWidth();
                int realY = (pos.y * displayHeight) / panel.getHeight();

                window.setTitle(realX + "," + realY + " : layer "+debugDisplayPixelFromWhere[realX + (realY * displayWidth)] +" : pixel " + String.format("%02x", debugDisplayPixel[realX + (realY * displayWidth)]) + " : RGB " + String.format("%06x", debugDisplayPixelRGB[realX + (realY * displayWidth)] & 0xffffff));
            }
        }
    }

    public boolean isVisible() {
        return window.isVisible();
    }

    public BufferedImage getImage() {
        return panel.getImage();
    }

    public DisplayLayer getLastLayerAdded() {
        return lastLayerAdded;
    }

    DisplayLayer lastLayerAdded;

    public void addLayer(DisplayLayer layer) {
        lastLayerAdded = layer;
        layer.setDisplay(this);

        boolean captured = false;
        if (layersRaw.length > 0) {
            if (layersRaw[layersRaw.length - 1].capturingMergeLayer()) {
                layersRaw[layersRaw.length - 1].captureLayer(layer);
                captured = true;
            }
        }

        if (!captured) {
            layers.add(layer);
            // Profiling shows that layers.size() was taking a significant chunk of time. It shouldn't have been. Instead use this array instead.
            layersRaw = layers.toArray(new DisplayLayer[layers.size()]);
            enableLayerFlags = new boolean[layers.size()];
            // Without overscan then default layers to being enabled, with overscan default to disabled (a clear latch)
            Arrays.fill(enableLayerFlags, !withOverscan);
        }
    }

    @Override
    public void writeData(int address, int addressEx, byte data) {
        if (pixelsSinceLastDebugWrite >= pixelsSinceLastDebugWriteMax) {
            pixelsSinceLastDebugWrite = 0;
            // This check removes waits for display H/V positions during the VBLANK, the non-visible part of the frame
            // The waits during the VBLANK would not really be that important from a simulation point of view
            // This is true, as long as mode7 writes (due to resetting the internal values on _VSYNC) are completed before the end of the _VSYNC which starts later and shorter than the VBLANK
            if (enableDisplay && !vBlank && debugData != null) {
//                debugData.println("d$0");
                debugData.printf("w$ff03ff00,$%02x%02x%02x00\n", displayVExternal & 0xff, (displayHExternal >> 8) & 0x01, displayHExternal & 0xff);
//                debugData.println("d$0");
            }
        }
        if (debugData != null) {
            debugData.printf("d$%04x%02x%02x\n", address, addressEx, data);
            debugData.flush();
        }

        lastDataWritten = data;
        if (addressActive(addressEx, addressExPalette) && address >= addressPalette && address < (addressPalette + 0x200)) {
            busContentionPalette = getBusContentionPixels();
            // Update the real memory first
            paletteMemory[paletteBank][address & 0x1ff] = data;

            // Then calculate the updated colour value from the memory
            int index = (address & 0x1ff) >> 1;
            int theColourValue = ((int) paletteMemory[paletteBank][index<<1]) & 0xff;
            theColourValue |= (((int) paletteMemory[paletteBank][(index<<1)+1]) & 0xff) << 8;

            // RGB 444
//            Color colour = new Color((theColourValue & 0x0f) << 4 , ((theColourValue >> 4) & 0x0f) << 4 , ((theColourValue >> 8) & 0x0f) << 4);
            // RGB 565
            Color colour = new Color((theColourValue & ((1<<paletteBitsRed)-1)) << (8-paletteBitsRed) , ((theColourValue >> paletteBitsRed) & ((1<<paletteBitsGreen)-1)) << (8-paletteBitsGreen) , ((theColourValue >> (paletteBitsRed + paletteBitsGreen)) & ((1<<paletteBitsBlue)-1)) << (8-paletteBitsBlue));

            // Store the real colour value in the palette cache
            palette[paletteBank][index] = colour.getRGB();
        }

        // This logic now exists on the video layer hardware
        if (addressExActive(addressEx, addressExRegisters) && address == addressRegisters) {
            if ((data & 0x10) > 0) {
                enableBackground = true;
            } else {
                enableBackground = false;
            }

            if ((data & 0x20) > 0) {
                enableDisplay = true;
            } else {
                enableDisplay = false;
            }

            if ((data & 0x80) > 0) {
                borderY = true;
            } else {
                borderY = false;
            }

            if (!withOverscan) {
                if ((data & 0x40) > 0) {
                    borderX = true;
                } else {
                    borderX = false;
                }
            }
        }

        if (addressExActive(addressEx, addressExRegisters) && address == addressRegisters + 0x08) {
            displayPriority = data;
        }

        if (withOverscan) {
            if (addressExActive(addressEx, addressExRegisters) && address == addressRegisters + 0x09) {
                overscanBorderExtent = data;
            }

            if (addressExActive(addressEx, addressExRegisters) && address == addressRegisters + 0x0a) {
                for (int i = 0; i < enableLayerFlags.length && i < 8; i++) {
                    if ((data & (1 << ((enableLayerFlags.length - 1) - i))) > 0) {
                        enableLayerFlags[i] = true;
                    } else {
                        enableLayerFlags[i] = false;
                    }
                }
            }

            if (addressExActive(addressEx, addressExRegisters) && address == addressRegisters + 0x0b) {
                backgroundColour = data;
            }

            if (numPaletteBanks > 0) {
                if (addressExActive(addressEx, addressExRegisters) && address == addressRegisters + 0x0c) {
                    paletteBank = data;
                }
            }
        }

        // Handle other layer writes
        for (DisplayLayer layer : layers) {
            layer.writeData(address, addressEx, data);
        }
    }

    @Override
    public void setAddressBus(int address, int addressEx) {
        for (DisplayLayer layer : layers) {
            layer.setAddressBus(address, addressEx);
        }
    }

    public void calculatePixelsUntilVSync() throws Exception {
        int timeout = pixelsInWholeFrame() * 2;
        do {
            calculatePixel();
            timeout--;
            if (timeout < 0) {
                throw new Exception("The VSync is not triggering, the video generation is probably disabled");
            }
        } while (_vSync);
    }

    public void calculatePixelsUntil(int waitH, int waitV) throws Exception {
        int timeout = pixelsInWholeFrame() * 2;
        do {
            calculatePixel();
            timeout--;
            if (timeout < 0) {
                throw new Exception("The wait value is not triggering, it is probably out of range or the video generation is disabled");
            }
        } while (!(displayHExternal == waitH && displayVExternal == waitV));
    }

    public void calculateAFrame() {
        for (int i = 0; i < pixelsInWholeFrame(); i++) {
            calculatePixel();
        }
    }

    public int pixelsInWholeFrame() {
        return displayWidth * displayHeight;
    }

    public void displayClearAhead() {
        int grey = Color.gray.getRGB();
        int tempx = displayBitmapX + 1;
        int tempy = displayBitmapY;
        while (tempy < panel.fastGetHeight()) {
            while (tempx < panel.fastGetWidth()) {
                if (tempy >= 0 && tempx >= 0) {
                    panel.fastSetRGB(tempx, tempy, grey);
                }
                tempx++;
            }
            tempx = 0;
            tempy++;
        }
    }

    public void displayClear() {
        int grey = Color.gray.getRGB();
        int tempx = 0;
        int tempy = 0;
        while (tempy < panel.fastGetHeight()) {
            while (tempx < panel.fastGetWidth()) {
                if (tempy >= 0 && tempx >= 0) {
                    panel.fastSetRGB(tempx, tempy, grey);
                }
                tempx++;
            }
            tempx = 0;
            tempy++;
        }
    }

    int cachedPixel[] = {-1, -1, -1, -1};

    public void calculatePixel() {
        pixelsSinceLastDebugWrite++;
        _hSync = true;
        _vSync = true;

        if (displayX >= 0 && displayX < 0x80) {
            displayH = 0x180 + displayX;
        } else {
            displayH = displayX - 0x80;
        }
        if (displayH >= 0x1b0 && displayH < 0x1d0) {
            _hSync = false;
        }
        // Positive edge, new video scan line
        if (displayH == 0x1cf) {
            displayBitmapX = 0;
            displayBitmapY++;
        }

//        if (displayH == 0x180) {
        if (displayY >= 0 && displayY < 0x08) {
            displayV = 0xf8 + displayY;
            _vSync = false;
        } else {
            displayV = displayY - 0x08;
        }
//        }

        boolean doLineStart = false;
        if (withOverscan) {
            if (displayH == 0x1d0) {
                displayVExternal = displayV;
                displayHExternal = 0;
                lineStartTimeDelay = 2;
            } else if (displayH == 0x1d1) {
                displayHExternal = 0;
            } else {
                displayHExternal++;
            }
            if (lineStartTimeDelay > 0) {
                lineStartTimeDelay--;
                doLineStart = true;
            } else {
                doLineStart = false;
            }
        } else {
            displayHExternal = displayH;
            displayVExternal = displayV;
        }

        if (callbackUserPort != null) {
            // Each pixel by default, has two VIDCLK transitions, so the APU needs two ticks
            // Using JP10
            callbackUserPort.calculatePixel();
            callbackUserPort.calculatePixel();
        }

        // Save the frame
        if (displayX == 0 && displayY == 0) {
            frameNumberForSync++;
            pixelsSinceLastDebugWrite = 0;
        }
        if (displayX == 0 && displayY == (displayHeight - 1)) {
            HandlePixelPick();
            if (leafFilename != null && !leafFilename.isEmpty()) {
                try {
                    File file = new File(leafFilename + String.format("%06d", frameNumber++) + ".bmp");
                    file.mkdirs();
                    try {
                        ImageIO.write(getImage(), "bmp", file);
                    } catch (Exception e) {
                        // Try once more before really failing...
                        ImageIO.write(getImage(), "bmp", file);
                    }
                } catch (IOException e) {
                }
            }
        }

        // One pixel delay from U95:A
        enablePixels = true;
        if (withOverscan) {
            // Note: When using $2b in emulation, the very last pixel edge will be duplicated in hardware, but not in emulation which outputs a new column of pixels
            // This is due to a small difference in _HSYNC handling
            // Will be safer to use the recommended $29 for a 320 wide screen
            if (!_hSync) {
                enablePixels = false;
            }
            int localdisplayH = displayHExternal;   // Adjust for observed simulation delay
            if (displayHExternal <= 0) {
                enablePixels = false;
            }
            if ((localdisplayH & 0x100) == 0x100) {
                if (((localdisplayH & 0x7f) >> 3) > (overscanBorderExtent & 0x0f)) {
                    enablePixels = false;
                }
            }
            localdisplayH = displayHExternal - 1;   // Adjust for observed simulation delay
            if (displayHExternal <= 0) {
                enablePixels = false;
            }
            if ((localdisplayH & 0x180) == 0x000) {
                if (((localdisplayH & 0x7f) >> 3) < ((overscanBorderExtent >> 4) & 0x0f)) {
                    enablePixels = false;
                }
            }
        } else {
            if (borderX && (displayH >= 0xfe/*0x181*/)) {
                enablePixels = false;
            }
            if (!borderX && (displayH >= 0x188/*0x189*/)) {
                enablePixels = false;
            }
            if (borderX && (displayH < 0x00d)) {
                enablePixels = false;
            }
            if (!borderX && (displayH < 0x009)) {
                enablePixels = false;
            }
        }

        if (borderY && (displayVExternal >= 0xe0)) {
            enablePixels = false;
        }

        if (displayVExternal == 0xef) {
            displayVExternal = displayVExternal;
        }
        if (withOverscan) {
            if (displayVExternal < 0x10 || displayVExternal >= 0xf0) {
                if (!vBlank) {
                    extEXTWANTIRQFlag = true;
                }
                vBlank = true;
            } else {
                vBlank = false;
            }
        } else {
            vBlank = false;
            if (displayVExternal < 0x10 || displayVExternal >= 0xf0) {
                vBlank = true;
            }
            if (displayH == 0x180 && displayVExternal == 0xf0) {
                extEXTWANTIRQFlag = true;
            }
        }

        if (vBlank /*|| (displayH & 256) == 256*/) {
            enablePixels = false;
        }
        // The hardware syncs on -ve _VBLANK
        if (displayX == 0 && displayVExternal == 0xf0) {
            if (enableDisplay && debugData != null) {
                debugData.println("; Frame " + frameNumberForSync);
                debugData.println("d$0");
                debugData.println("^-$01");
                debugData.println("d$0");
                debugData.flush();
                if (callbackUserPort != null) {
                    callbackUserPort.signalVBlank();
                }
            }
        }


        if (enableDisplay) {
            // Remove the problematic top most 8 pixel bug
            if (displayBitmapY == 24) {
                enablePixels = false;
            }
            // Adjust to match emulation and simulation
            int tempy = displayBitmapY - 8;
            // Make sure the rendering position is in the screen
            if (displayBitmapX >= 0 && tempy >= 0 && displayBitmapX < panel.fastGetWidth() && tempy < panel.fastGetHeight()) {
                // Delayed due to pixel latching in the output mixer 8B2 and 7A2
                if (enablePixels) {
                    if (busContentionPalette > 0) {
                        latchedPixel = getContentionColouredPixel();
                    }
                    int realColour = palette[paletteBank][latchedPixel & 0xff];
                    if (debugDisplayPixels) {
                        debugDisplayPixel[displayBitmapX + (tempy*displayWidth)] = latchedPixel & 0xff;
                        debugDisplayPixelRGB[displayBitmapX + (tempy*displayWidth)] = realColour;
                        debugDisplayPixelFromWhere[displayBitmapX + (tempy*displayWidth)] = latchedPixelFromWhere;
                    }
                    panel.fastSetRGB(displayBitmapX, tempy, realColour);
                } else {
                    panel.fastSetRGB(displayBitmapX, tempy, 0);
                }
            }
        }

        latchedPixel = 0;
        boolean firstLayer = true;
        if (layersRaw.length <= 4) {
            cachedPixel[0] = -1;
            cachedPixel[1] = -1;
            cachedPixel[2] = -1;
            cachedPixel[3] = -1;
            // Go backwards from the furthest plane first
            for (int i = layersRaw.length - 1; i >= 0; i--) {
                int theLayer = (displayPriority >> (i * 2)) & 0x03;
                int realLayer = theLayer;
                theLayer = (layersRaw.length - 1) - theLayer;
                if (theLayer >= 0 && theLayer < layersRaw.length) {
                    // Ensure each layer index is executed once
                    if (cachedPixel[theLayer] < 0) {
                        DisplayLayer displayLayer = layersRaw[theLayer];
                        int pixel = displayLayer.calculatePixel(displayHExternal, displayVExternal, _hSync, _vSync, doLineStart, enableLayerFlags[theLayer], vBlank);
                        if (is16Colours) {
                            if ((pixel & 0x0f) != 0 || firstLayer) {
                                latchedPixel = pixel;
                                latchedPixelFromWhere = realLayer;
                            }
                        } else {
                            if ((pixel & 0x07) != 0 || firstLayer) {
                                latchedPixel = pixel;
                                latchedPixelFromWhere = realLayer;
                            }
                        }
                        cachedPixel[theLayer] = pixel;
                        firstLayer = false;
                    }
                }
            }
            // Age all layers once, regardless of the priority setting
            for (DisplayLayer layer : layersRaw) {
                layer.ageContention();
            }
        } else {
            int layerIndex = 0;
            for (DisplayLayer layer : layersRaw) {
                int pixel = layer.calculatePixel(displayHExternal, displayVExternal, _hSync, _vSync, doLineStart, enableLayerFlags[layerIndex], vBlank);
                layer.ageContention();
                // If there is pixel data in the layer then use it
                // Always use the first colour, which is the furthest layer colour
                if (is16Colours) {
                    if ((pixel & 0x0f) != 0 || firstLayer) {
                        latchedPixel = pixel;
                    }
                } else {
                    if ((pixel & 0x07) != 0 || firstLayer) {
                        latchedPixel = pixel;
                    }
                }
                firstLayer = false;
                layerIndex++;
            }
        }

        if (withOverscan && enableBackground && (latchedPixel & 0x0f) == 0) {
            latchedPixel = backgroundColour;
            latchedPixelFromWhere = -2;
        }

        displayX++;
        displayBitmapX++;
        if (displayX >= displayWidth) {
            displayX = 0;
            displayY++;
        }
        if (displayY >= displayHeight) {
            displayY = 0;
            enablePixels = false;
            displayBitmapX = 0;
            displayBitmapY = 0;
        }
        if (busContentionPalette > 0) {
            busContentionPalette--;
        }
    }

    int sequentialValue = 0;

    int getContentionColouredPixel() {
        // Introduce some colour and pattern using the part of last byte written to simulate a bus with contention
        sequentialValue++;
        if ((sequentialValue & 1) > 0) {
            return (sequentialValue & 0x0f) | (lastDataWritten & 0xf0);
        }
        return lastDataWritten;
    }

    public void writeDebugBMPsToLeafFilename(String leafFilename) {
        this.leafFilename = leafFilename;
    }

    public DisplayMainFrame getWindow() {
        return window;
    }

    public String getDebug() {
        String debug = "";
        for (int i = layersRaw.length - 1; i >= 0; i--) {
            debug += layersRaw[i].getDebug();
        }
        return debug;
    }

    public void randomiseData(Random rand) {
        for (DisplayLayer layer : layers) {
            layer.randomiseData(rand);
        }

        randomiseHelper(rand , palette);
        randomiseHelper(rand , paletteMemory);

        borderX = rand.nextBoolean();
        borderY = rand.nextBoolean();
        overscanBorderExtent = rand.nextInt();

        displayPriority = rand.nextInt();
        randomiseHelper(rand , enableLayerFlags);

        if (numPaletteBanks > 0) {
            // With randomly initialised state, we do not want the palette bank to be different when using the feature to send palette data compared to the code using the default 0 bank
//            paletteBank = rand.nextInt() & (numPaletteBanks - 1);
        }
    }

    public void setRGBColour(int r, int g, int b) {
        paletteBitsRed = r;
        paletteBitsGreen = g;
        paletteBitsBlue = b;
    }

    public void setPaletteBanks(int numBanks) {
        numPaletteBanks = numBanks;
    }
}
