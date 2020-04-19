package com.bdd6502;

import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;

public class DisplayBombJack {

    JFrame window;
    QuickDrawPanel panel;

    LinkedList<DisplayLayer> layers = new LinkedList<>();

    int displayWidth = 384;
    int displayHeight = 264;
    int busContentionPalette = 0;
    int displayH = 0, displayV = 0;
    int displayX = 0, displayY = 0;
    int displayBitmapX = 0, displayBitmapY = 0;
    boolean enablePixels = false;
    boolean borderX = false, borderY = false;   // Set by the Tiles layer (if added)
    boolean enableDisplay = true;               // Set by the Tiles layer (if added)
    int latchedPixel = 0;
    int palette[] = new int[256];
    Random random = new Random();
    public DisplayBombJack() {
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
        window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
        if (addressActive(addressEx, 0x01) && address >= 0x9c00 && address < 0x9e00) {
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

    void calculatePixelsUntil(int waitH, int waitV) {
        do {
            calculatePixel();
        } while (!(displayH == waitH && displayV == waitV));
    }

    void calculatePixel() {
        boolean _hSync = true, _vSync = true;

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

        boolean vBlank = false;
        if (displayV < 0x10 || displayV >= 0xf0) {
            vBlank = true;
        }

        if (vBlank /*|| (displayH & 256) == 256*/) {
            enablePixels = false;
        }

        if (enableDisplay) {
            int tempy = displayBitmapY;
            // This is to correct a very strange problem with the video rendering in the last 8 pixels of the visible area
            if (displayH >= 0x180 && displayH < 0x188) {
                tempy++;
            }
            // Make sure the rendering position is in the screen
            if (displayBitmapX >= 0 && tempy >= 0 && displayBitmapX < panel.getImage().getWidth() && tempy < panel.getImage().getHeight()) {
                // Delayed due to pixel latching in the output mixer 8B2 and 7A2
                if (enablePixels) {
                    if (busContentionPalette > 0) {
                        latchedPixel = getRandomColouredPixel();
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

    int getRandomColouredPixel() {
        return random.nextInt() & 0xff;
    }
}
