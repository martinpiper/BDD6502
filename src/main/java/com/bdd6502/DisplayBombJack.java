package com.bdd6502;

import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.LinkedList;
import java.util.Random;

public class DisplayBombJack {

    DisplayMainFrame window;
    QuickDrawPanel panel;

    LinkedList<DisplayLayer> layers = new LinkedList<>();

    int frameNumber = 0;
    int displayWidth = 384;
    int displayHeight = 264;
    int busContentionPalette = 0;
    int addressPalette = 0x9c00, addressExPalette = 0x01;

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
    boolean borderX = false, borderY = false;   // Set by the Tiles layer (if added)
    boolean enableDisplay = true;               // Set by the Tiles layer (if added)
    int latchedPixel = 0;
    int palette[] = new int[256];
    Random random = new Random();
    String leafFilename = null;
    int lastDataWritten = 0;
    boolean vBlank = false;
    boolean _hSync = true, _vSync = true;
    PrintWriter debugData = null;
    int pixelsSinceLastDebugWrite = 0;
    int pixelsSinceLastDebugWriteMax = 32;

    public boolean getVSync() {
        return _vSync;
    }
    public boolean getVBlank() {
        return vBlank;
    }

    public DisplayBombJack() throws IOException {
        debugData = new PrintWriter(new FileWriter("target/debugData.txt"));
        debugData.println("; Automatically created by DisplayBombJack");
        // The address is handled by the writeData
//        debugData.println("+16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,+");
//        debugData.println("<0,1,2,3,4,5,6,7,<");
        debugData.println("d0");
    }

    static boolean addressActive(int addressEx, int selector) {
        if ((addressEx & selector) > 0) {
            return true;
        }
        return false;
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
    }

    public void writeDataFromFile(int address, int addressEx, String filename) throws IOException {
        byte[] data = FileUtils.readFileToByteArray(new File(filename));
        for (int i = 0; i < data.length; i++) {
            writeData(address + i, addressEx, data[i]);
        }
    }

    public void writeDataFromFile(int address, int addressEx, String filename, int offset, int length) throws IOException {
        byte[] data = FileUtils.readFileToByteArray(new File(filename));
        for (int i = 0; i < length; i++) {
            writeData(address + i, addressEx, data[offset + i]);
        }
    }

    public void writeData(int address, int addressEx, int data) {
        writeData(address, addressEx, (byte) data);
    }

    public void writeData(int address, int addressEx, byte data) {
        if (pixelsSinceLastDebugWrite >= pixelsSinceLastDebugWriteMax) {
            pixelsSinceLastDebugWrite = 0;
            // This check removes waits for display H/V positions during the VBLANK, the non-visible part of the frame
            // The waits during the VBLANK would not really be that important from a simulation point of view
            // This is true, as long as mode7 writes (due to resetting the internal values on _VSYNC) are completed before the end of the _VSYNC which starts later and shorter than the VBLANK
            if (enableDisplay && !vBlank) {
                debugData.println("d$0");
                debugData.printf("w$ff01ff00,$%02x%02x%02x00\n", displayV & 0xff , (displayH >> 8) & 0x01 , displayH & 0xff );
                debugData.println("d$0");
            }
        }
        debugData.printf("d$%04x%02x%02x\n" , address , addressEx , data);

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
        for (DisplayLayer layer : layers) {
            layer.writeData(address, addressEx, data);
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
        for (int i = 0 ; i < displayWidth*displayHeight ; i++) {
            calculatePixel();
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

        if (displayY >= 0 && displayY < 0x08) {
            displayV = 0xf8 + displayY;
            _vSync = false;
        } else {
            displayV = displayY - 0x08;
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
                    ImageIO.write(getImage(), "bmp", file);
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

        if (vBlank /*|| (displayH & 256) == 256*/) {
            enablePixels = false;
        }
        // The hardware syncs on -ve _VBLANK
        if (displayX == 0 && displayV == 0xf0) {
            if (enableDisplay) {
                debugData.println("d$0");
                debugData.println("^-$01");
                debugData.println("d$0");
                debugData.flush();
            }
        }


        if (enableDisplay) {
            int tempy = displayBitmapY;
            // Make sure the rendering position is in the screen
            if (displayBitmapX >= 0 && tempy >= 0 && displayBitmapX < panel.getImage().getWidth() && tempy < panel.getImage().getHeight()) {
                // Delayed due to pixel latching in the output mixer 8B2 and 7A2
                if (enablePixels) {
                    if (busContentionPalette > 0) {
                        latchedPixel = getContentionColouredPixel();
                    }
                    int realColour = palette[latchedPixel & 0xff];
                    panel.getImage().setRGB(displayBitmapX, tempy, realColour);
                } else {
                    panel.getImage().setRGB(displayBitmapX, tempy, 0);
                }
            }
        }

        latchedPixel = 0;
        boolean firstLayer = true;
        for (DisplayLayer layer : layers) {
            int pixel = layer.calculatePixel(displayH, displayV, _hSync, _vSync);
            layer.ageContention();
            // If there is pixel data in the layer then use it
            // Always use the first colour, which is the furthest layer colour
            if ((pixel & 0x07) != 0 || firstLayer) {
                latchedPixel = pixel;
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
}
