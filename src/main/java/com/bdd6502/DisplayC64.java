package com.bdd6502;

import com.loomcom.symon.devices.C64ColourRAM;
import com.loomcom.symon.devices.C64VICII;
import com.loomcom.symon.devices.Device;
import com.loomcom.symon.devices.UserPortTo24BitAddress;
import org.apache.commons.lang3.RandomUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class DisplayC64 {

    JFrame window;
    QuickDrawPanel panel;

    public void setTheVICII(Device theVICII) {
        this.theVICII = theVICII;
    }

    Device theVICII = null;

    public void setTheColourRAM(Device theColourRAM) {
        this.theColourRAM = theColourRAM;
    }

    Device theColourRAM = null;

    public void setTheRAM(Device theRAM) {
        this.theRAM = theRAM;
    }

    Device theRAM = null;

    int displayWidth = 504;
    int displayHeight = 312;
    int cyclesPerLine = 63;

    public int getFrameNumberForSync() {
        return frameNumberForSync;
    }

    int frameNumberForSync = 0;
    boolean vSync = false;

    public int getDisplayH() {
        return displayH;
    }

    public int getDisplayV() {
        return displayV;
    }

    public boolean isvSync() {
        return vSync;
    }

    int displayH = 0, displayV = 0;

    // https://www.c64-wiki.com/wiki/Color
    // Almost certain someone somewhere is going to disagree with these colours, they're for debug only, deal with it.
    int palette[] = {getRGB(0,0,0) , getRGB(255, 255, 255), getRGB(136, 0, 0), getRGB(170, 255, 238),
        getRGB(204, 68, 204), getRGB(0, 204, 85), getRGB(	0, 0, 170), getRGB(238, 238, 119),
        getRGB(221, 136, 85), getRGB(102, 68, 0), getRGB(	255, 119, 119), getRGB(51, 51, 51),
        getRGB(119, 119, 119), getRGB(170, 255, 102), getRGB(0, 136, 255), getRGB(187, 187, 187)
    };

    String leafFilename = null;

    static int getRGB(int r, int g , int b) {
        Color colour = new Color(r,g,b);
        return colour.getRGB();
    }


    public DisplayC64() throws IOException {
    }

    public void InitWindow() {
        InitWindow(800, (800 * displayHeight) / displayWidth);
    }

    public void InitWindow(int width, int height) {
        // Testing window drawing in a loop for eventual graphics updates
        window = new JFrame();
        window.getContentPane().setPreferredSize(new Dimension(width, height));
        window.pack();

        panel = new QuickDrawPanel(displayWidth, displayHeight);
        window.add(panel);

        window.setVisible(true);
        window.setTitle("C64 VIC II");
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

    public void calculatePixelsUntilVSync() {
        do {
            calculatePixel();
        } while (!vSync);
    }

    public void calculatePixelsUntil(int waitH, int waitV) {
        do {
            calculatePixel();
        } while (!(displayH == waitH && displayV == waitV));
    }

    public void calculatePixelsFor(int num) {
        while(num > 0) {
            calculatePixel();
            num--;
        }
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
        int tempx = displayH + 1;
        int tempy = displayV;
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

    int kVIC2SpriteXBorderLeft = 24;
    int kVIC2SpriteXBorderLeft38 = 31;
    int kVIC2SpriteXBorderRight = 256+88;
    int kVIC2SpriteXBorderRight38 = kVIC2SpriteXBorderRight-8;
    int kVIC2SpriteYBorderTop = 50;
    int kVIC2SpriteYBorderTop24 = 54;
    int kVIC2SpriteYBorderBottom = 250;

    public void calculatePixel() {
        vSync = false;

        // One pixel delay from U95:A
        boolean enablePixels = true;
        if ((displayH < kVIC2SpriteXBorderLeft)) {
            enablePixels = false;
        }
        else if (displayH >= kVIC2SpriteXBorderRight) {
            enablePixels = false;
        }
        else if (displayV < kVIC2SpriteYBorderTop) {
            enablePixels = false;
        }
        else if (displayV >= kVIC2SpriteYBorderBottom) {
            enablePixels = false;
        }

        if (enablePixels) {
            // TODO: Chars/bitmaps/sprites/background
            panel.fastSetRGB(displayH, displayV, RandomUtils.nextInt(0, 256));
        } else {
            // TODO: Border colour
            panel.fastSetRGB(displayH, displayV, 0);
        }

        displayH++;
        if (displayH >= displayWidth) {
            displayH = 0;
            displayV++;
        }
        if (displayV >= displayHeight) {
            displayV = 0;
            vSync = true;

            // Save the frame
            frameNumberForSync++;
            if (leafFilename != null && !leafFilename.isEmpty()) {
                try {
                    File file = new File(leafFilename + String.format("%06d", frameNumberForSync++) + ".bmp");
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

            RepaintWindow();
        }
    }

    public void writeDebugBMPsToLeafFilename(String leafFilename) {
        this.leafFilename = leafFilename;
    }

    public JFrame getWindow() {
        return window;
    }
}
