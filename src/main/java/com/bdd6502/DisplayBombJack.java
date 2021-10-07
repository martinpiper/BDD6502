package com.bdd6502;

import com.loomcom.symon.devices.UserPortTo24BitAddress;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

public class DisplayBombJack extends MemoryBus {

    DisplayMainFrame window;
    QuickDrawPanel panel;

    ArrayList<DisplayLayer> layers = new ArrayList<>();
    DisplayLayer layersRaw[] = new DisplayLayer[0];

    int frameNumber = 0;
    int displayWidth = 384;
    int displayHeight = 264;
    int busContentionPalette = 0;
    int addressPalette = 0x9c00, addressExPalette = 0x01;
    int addressRegisters = 0x9e00, addressExRegisters = 0x01;
    int displayPriority = 0;  // Default to be 0, this helps ensure startup code correctly sets this option

    public int getFrameNumberForSync() {
        return frameNumberForSync;
    }

    int frameNumberForSync = 0;

    public int getDisplayH() {
        return displayH;
    }

    public int getDisplayV() {
        return displayV;
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
    int displayX = 0, displayY = 0;
    int displayBitmapX = 0, displayBitmapY = 0;
    boolean enablePixels = false;
    boolean borderX = true, borderY = true;
    boolean enableDisplay = false;  // Default to be display off, this helps ensure startup code correctly sets this option
    int latchedPixel = 0;
    int palette[] = new int[256];
    Random random = new Random();
    String leafFilename = null;
    int lastDataWritten = 0;
    boolean vBlank = false;
    boolean _hSync = true, _vSync = true;
    boolean extEXTWANTIRQFlag = false;
    PrintWriter debugData = null;
    int pixelsSinceLastDebugWrite = 0;
    int pixelsSinceLastDebugWriteMax = 32;
    boolean is16Colours = false;
    UserPortTo24BitAddress callbackAPU = null;

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

    public DisplayBombJack() throws IOException {
        //enableDebugData();
    }

    public void make16Colours() {
        is16Colours = true;
    }

    public void enableDebugData() throws IOException {
        debugData = new PrintWriter(new FileWriter("target/debugData.txt"));
        debugData.println("; Automatically created by DisplayBombJack");
        // The address is handled by the writeData
//        debugData.println("+16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,+");
//        debugData.println("<0,1,2,3,4,5,6,7,<");
        debugData.println("d0");
    }

    public void setCallbackAPU(UserPortTo24BitAddress apu) {
        callbackAPU = apu;
    }

    public int getBusContentionPixels() {
        return 0x08;
    }

    public void InitWindow() {
        InitWindow(800, 600);
    }

    public void InitWindow(int width, int height) {
        // Testing window drawing in a loop for eventual graphics updates
        window = new DisplayMainFrame();
        window.addKeyListener(window);
        //window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.getContentPane().setPreferredSize(new Dimension(width, height));
        window.pack();

        panel = new QuickDrawPanel(displayWidth, displayHeight);
        window.add(panel);

        window.setVisible(true);
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

    public boolean isVisible() {
        return window.isVisible();
    }

    public BufferedImage getImage() {
        return panel.getImage();
    }

    public void addLayer(DisplayLayer layer) {
        layer.setDisplay(this);
        layers.add(layer);
        // Profiling shows that layers.size() was taking a significant chunk of time. It shouldn't have been. Instead use this array instead.
        layersRaw = layers.toArray(new DisplayLayer[layers.size()]);
    }

    @Override
    public void writeData(int address, int addressEx, byte data) {
        if (pixelsSinceLastDebugWrite >= pixelsSinceLastDebugWriteMax) {
            pixelsSinceLastDebugWrite = 0;
            // This check removes waits for display H/V positions during the VBLANK, the non-visible part of the frame
            // The waits during the VBLANK would not really be that important from a simulation point of view
            // This is true, as long as mode7 writes (due to resetting the internal values on _VSYNC) are completed before the end of the _VSYNC which starts later and shorter than the VBLANK
            if (enableDisplay && !vBlank && debugData != null) {
                debugData.println("d$0");
                debugData.printf("w$ff03ff00,$%02x%02x%02x00\n", displayV & 0xff , (displayH >> 8) & 0x01 , displayH & 0xff );
                debugData.println("d$0");
            }
        }
        if (debugData != null) {
            debugData.printf("d$%04x%02x%02x\n", address, addressEx, data);
            debugData.flush();
        }

        lastDataWritten = data;
        if (addressActive(addressEx, addressExPalette) && address >= addressPalette && address < (addressPalette + 0x200)) {
            busContentionPalette = getBusContentionPixels();
            int index = (address & 0x1ff) >> 1;
            Color colour = new Color(palette[index]);
            if ((address & 0x01) == 0x01) {
                colour = new Color(colour.getRed(), colour.getGreen(), (data & 0x0f) << 4);
                palette[index] = colour.getRGB();
            } else {
                colour = new Color((data & 0x0f) << 4, data & 0xf0, colour.getBlue());
                palette[index] = colour.getRGB();
            }
        }

        // This logic now exists on the video layer hardware
        if (MemoryBus.addressActive(addressEx, addressExRegisters) && address == addressRegisters) {
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
            if ((data & 0x40) > 0) {
                borderX = true;
            } else {
                borderX = false;
            }
        }

        if (MemoryBus.addressActive(addressEx, addressExRegisters) && address == addressRegisters + 0x08) {
            displayPriority = data;
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

    public void calculatePixelsUntilVSync() {
        do {
            calculatePixel();
        } while (_vSync);
    }

    public void calculatePixelsUntil(int waitH, int waitV) {
        do {
            calculatePixel();
        } while (!(displayH == waitH && displayV == waitV));
    }

    public void calculateAFrame() {
        for (int i = 0 ; i < pixelsInWholeFrame() ; i++) {
            calculatePixel();
        }
    }

    public int pixelsInWholeFrame() {
        return displayWidth*displayHeight;
    }

    public void displayClearAhead()
    {
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

    public void displayClear()
    {
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

        if (callbackAPU != null) {
            // Each pixel by default, has two VIDCLK transitions, so the APU needs two ticks
            // Using JP10
            callbackAPU.calculatePixel();
            callbackAPU.calculatePixel();
        }

        // Save the frame
        if (displayX == 0 && displayY == 0) {
            frameNumberForSync++;
            pixelsSinceLastDebugWrite = 0;
        }
        if (displayX == 0 && displayY == (displayHeight-1)) {
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
                } catch (IOException e) {}
            }
        }

        // One pixel delay from U95:A
        enablePixels = true;
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

        if (borderY && (displayV >= 0xe0)) {
            enablePixels = false;
        }

        vBlank = false;
        if (displayV < 0x10 || displayV >= 0xf0) {
            vBlank = true;
        }
        if (displayH == 0x180 && displayV == 0xf0) {
            extEXTWANTIRQFlag = true;
        }

        if (vBlank /*|| (displayH & 256) == 256*/) {
            enablePixels = false;
        }
        // The hardware syncs on -ve _VBLANK
        if (displayX == 0 && displayV == 0xf0) {
            if (enableDisplay && debugData != null) {
                debugData.println("d$0");
                debugData.println("^-$01");
                debugData.println("d$0");
                debugData.flush();
            }
        }


        if (enableDisplay) {
            // Remove the problematic top most 8 pixel bug
            if (displayBitmapY == 24) {
                enablePixels = false;
            }
            int tempy = displayBitmapY;
            // Make sure the rendering position is in the screen
            if (displayBitmapX >= 0 && tempy >= 0 && displayBitmapX < panel.fastGetWidth() && tempy < panel.fastGetHeight()) {
                // Delayed due to pixel latching in the output mixer 8B2 and 7A2
                if (enablePixels) {
                    if (busContentionPalette > 0) {
                        latchedPixel = getContentionColouredPixel();
                    }
                    int realColour = palette[latchedPixel & 0xff];
                    panel.fastSetRGB(displayBitmapX, tempy, realColour);
                } else {
                    panel.fastSetRGB(displayBitmapX, tempy, 0);
                }
            }
        }

        latchedPixel = 0;
        boolean firstLayer = true;
        if (layersRaw.length <= 4) {
            int cachedPixel[] = {-1,-1,-1,-1};
            // Go backwards from the furthest plane first
            for (int i = layersRaw.length-1 ; i >= 0 ; i--) {
                int theLayer = (displayPriority >> (i*2)) & 0x03;
                theLayer = (layersRaw.length-1)-theLayer;
                if (theLayer >= 0 && theLayer < layersRaw.length) {
                    // Ensure each layer index is executed once
                    if (cachedPixel[theLayer] < 0) {
                        DisplayLayer displayLayer = layersRaw[theLayer];
                        int pixel = displayLayer.calculatePixel(displayH, displayV, _hSync, _vSync);
                        if (is16Colours) {
                            if ((pixel & 0x0f) != 0 || firstLayer) {
                                latchedPixel = pixel;
                            }
                        } else {
                            if ((pixel & 0x07) != 0 || firstLayer) {
                                latchedPixel = pixel;
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
            for (DisplayLayer layer : layersRaw) {
                int pixel = layer.calculatePixel(displayH, displayV, _hSync, _vSync);
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
            }
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

    public DisplayLayer getLastLayer() {
        return layers.get(layers.size()-1);
    }
}
