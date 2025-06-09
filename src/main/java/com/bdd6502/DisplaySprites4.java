package com.bdd6502;

import com.loomcom.symon.devices.C64VICII;
import com.loomcom.symon.devices.Device;
import com.loomcom.symon.exceptions.MemoryAccessException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import static com.bdd6502.MemoryBus.addressActive;

public class DisplaySprites4 {

    public void swapBuffers() {
        panel.swapBuffers();
    }

    class DisplayFrame extends JFrame {

    }

    DisplayFrame window;


    QuickDrawPanel panel;

    int displayWidth = 512;
    int displayHeight = 512;

    public DisplaySprites4() throws IOException {
    }

    public void InitWindow() {
        double scale = 1.0f;
        InitWindow((int)(displayWidth * scale), (int)(displayHeight * scale));
    }

    public void InitWindow(int width, int height) {
        // Testing window drawing in a loop for eventual graphics updates
        window = new DisplayFrame();
        window.getContentPane().setPreferredSize(new Dimension(width, height));
        window.pack();

        panel = new QuickDrawPanel(displayWidth, displayHeight);
        panel.setDoublebuffered();
        window.add(panel);

        window.setVisible(true);
        window.setTitle("Sprites4 debug");
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

    public int pixelsInWholeFrame() {
        return displayWidth * displayHeight;
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

    public JFrame getWindow() {
        return window;
    }

    void fastSetRGB(int x, int y, int rgb) {
        panel.fastSetRGB(x,y,rgb);
    }
}
