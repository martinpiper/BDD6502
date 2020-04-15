package com.bdd6502;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Random;

public class DisplayBombJack {

    JFrame window;
    QuickDrawPanel panel;

    LinkedList<DisplayLayer> layers = new LinkedList<>();

    int displayWidth = 384;
    int displayHeight = 276;
    int busContentionPixels = 0x08;
    int busContentionPalette = 0;
    int displayX = 0, displayY = 0;
    boolean enablePixels = false;
    boolean borderX = false, borderY = false;
    int latchedPixel = 0;

    int palette[] = new int[256];
    Random random = new Random();

    public DisplayBombJack() {
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

    public void writeData(int address, int addressEx, int data) {
        if (addressEx == 0x01 && address >= 0x9c00 && address < 0x9e00) {
            busContentionPalette = busContentionPixels;
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

    void calculatePixel() {
        int displayH, displayV;
        boolean _hSync = true, _vSync = true;

        if (displayX >= 0 && displayX < 0x80) {
            displayH = 0x180 + displayX;
            _hSync = true;
        } else {
            displayH = displayX - 0x80;
        }
        if (displayH >= 0x1b0 && displayH < 0x1d0) {
            _hSync = false;
        }

        if (displayY >= 0 && displayY < 0x08) {
            displayV = 0xf8 + displayY;
            _vSync = false;
        } else {
            displayV = displayY - 0x08;
        }

        // Delayed due to pixel latching in the output mixer 8B2 and 7A2
        if (enablePixels) {
            int realColour = palette[latchedPixel & 0xff];
            if (busContentionPalette > 0) {
                realColour = getRandomColouredPixel();
            }
            panel.getImage().setRGB(displayX, displayY, realColour);
        } else {
            panel.getImage().setRGB(displayX, displayY, 0);
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

        // One pixel delay from U95:A
        boolean internalBorderX = false;
        // From U236 and U237:A
        if (((displayH & (4 + 16 + 32 + 64)) == 0) && ((displayH & (128 + 256)) == 128) && borderX) {
            internalBorderX = true;
        }
        boolean internalBorderY = false;
        // From U76
        if (((displayV & (16 + 32 + 64 + 128)) > (32 + 64 + 128)) && borderY) {
            internalBorderY = true;
        }
        if ((displayH & 256) == 256 && borderX) {
            internalBorderY = true;
        }

        boolean vBlank = false;
        if (displayV < 0x10 || displayV >= 0xf0) {
            vBlank = true;
        }

        if (vBlank || internalBorderX || internalBorderY || (displayH & 256) == 256) {
            enablePixels = false;
        } else {
            enablePixels = true;
        }


        displayX++;
        if (displayX >= displayWidth) {
            displayX = 0;
            displayY++;
        }
        if (displayY >= displayHeight) {
            displayY = 0;
            enablePixels = false;
        }
        if (busContentionPalette > 0) {
            busContentionPalette--;
        }
    }

    int getRandomColouredPixel() {
        return random.nextInt() & 0xffffff;
    }
}
