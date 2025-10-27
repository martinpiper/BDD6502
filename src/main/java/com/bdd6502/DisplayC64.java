package com.bdd6502;

import com.loomcom.symon.devices.C64ColourRAM;
import com.loomcom.symon.devices.C64VICII;
import com.loomcom.symon.devices.Device;
import com.loomcom.symon.devices.UserPortTo24BitAddress;
import com.loomcom.symon.exceptions.MemoryAccessException;
import org.apache.commons.lang3.RandomUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

import static com.bdd6502.MemoryBus.addressActive;

public class DisplayC64 {

    class DisplayFrame extends JFrame implements KeyListener {

        public boolean anyPendingKeys() {
            return !typed.isEmpty();
        }
        public KeyEvent getTyped() {
            if (typed.isEmpty()) {
                return null;
            }
            return typed.pop();
        }

        LinkedList<KeyEvent> typed = new LinkedList<>();

        @Override
        public void keyTyped(KeyEvent e) {
            typed.add(e);
        }

        @Override
        public void keyPressed(KeyEvent e) {

        }

        @Override
        public void keyReleased(KeyEvent e) {

        }
    }

    DisplayFrame window;
    QuickDrawPanel panel;
    private int forceBank = -1;

    public void setTheVICII(C64VICII theVICII) {
        this.theVICII = theVICII;
    }

    C64VICII theVICII = null;

    public void setTheColourRAM(Device theColourRAM) {
        this.theColourRAM = theColourRAM;
    }

    Device theColourRAM = null;

    public void setTheRAM(Device theRAM) {
        this.theRAM = theRAM;
    }

    Device theRAM = null;


    public void setTheIO(Device theIO) {
        this.theIO = theIO;
    }

    Device theIO = null;

    public void setTheCHARGEN(Device theCHARGEN) {
        this.theCHARGEN = theCHARGEN;
    }

    Device theCHARGEN = null;

    int displayWidth = 504;
    int displayHeight = 312;

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

    // Almost certain someone somewhere is going to disagree with these colours, they're for debug only, deal with it.
    // https://www.c64-wiki.com/wiki/Color
//    int palette[] = {getRGB(0,0,0) , getRGB(255, 255, 255), getRGB(136, 0, 0), getRGB(170, 255, 238),
//        getRGB(204, 68, 204), getRGB(0, 204, 85), getRGB(	0, 0, 170), getRGB(238, 238, 119),
//        getRGB(221, 136, 85), getRGB(102, 68, 0), getRGB(	255, 119, 119), getRGB(51, 51, 51),
//        getRGB(119, 119, 119), getRGB(170, 255, 102), getRGB(0, 136, 255), getRGB(187, 187, 187)
//    };
    // VICE\C64\vice.vpl
    int palette[] = {0x000000, 0xFFFFFF, 0x68372b, 0x70a4b2,
            0x6f3d86, 0x588d43, 0x352879, 0xb8c76f,
            0x6f4f25, 0x433900, 0x9a6759, 0x444444,
            0x6c6c6c, 0x9ad284, 0x6c5eb5, 0x959595
    };

    String leafFilename = null;

    static int getRGB(int r, int g , int b) {
        Color colour = new Color(r,g,b);
        return colour.getRGB();
    }


    public DisplayC64() throws IOException {
    }

    boolean enableKeyboardBufferHook = false;
    public void enableKeyboardBufferHook() {
        enableKeyboardBufferHook = true;
    }

    public void InitWindow() {
        double scale = 2.0f;
        InitWindow((int)(displayWidth * scale), (int)(displayHeight * scale));
    }

    public void InitWindow(int width, int height) {
        // Testing window drawing in a loop for eventual graphics updates
        window = new DisplayFrame();
        window.addKeyListener(window);
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
        refreshInternals();
        do {
            calculatePixel();
        } while (!vSync);
    }

    public void calculatePixelsUntil(int waitH, int waitV) {
        refreshInternals();
        do {
            calculatePixel();
        } while (!(displayH == waitH && displayV == waitV));
    }

    public void calculatePixelsFor(int num) {
        refreshInternals();
        while(num > 0) {
            calculatePixel();
            num--;
        }
    }

    public void calculateAFrame() {
        refreshInternals();
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

    int screenAddress = 0;
    int charsBitmapAddress = 0;
    int currentCharBits = 0;
    int currentChar = 0;
    int currentColour = 0;
    boolean isMulticolour = false;
    boolean isBitmap = false;
    boolean isCHARROM = false;
    int borderColour = 0;
    int backgroundColour = 0;
    int extraBackgroundColour1 = 0;
    int extraBackgroundColour2 = 0;
    int extraBackgroundColour3 = 0;
    int bankToAddress[] = {0xc000, 0x8000, 0x4000, 0x0000};
    private void refreshInternals() {
        try {
            if (enableKeyboardBufferHook) {
                if (window.anyPendingKeys()) {
                    int data = theRAM.read(0xc6 , false);
                    // Only inject keys when the C64 keyboard buffer is empty
                    if (data == 0) {
                        KeyEvent typed = window.getTyped();
                        if (typed != null) {
                            theRAM.write(0xc6, 1);
                            char theChar = typed.getKeyChar();
                            if (Character.isDigit(theChar)) {
                                theRAM.write(0x277, theChar);
                            } else if (theChar >= 'a' && theChar <= 'z') {
                                theRAM.write(0x277, 0x41 + (theChar - 'a'));
                            } else if (theChar >= 'A' && theChar <= 'Z') {
                                theRAM.write(0x277, 0x61 + (theChar - 'A'));
                            } else if (theChar == '\b') {
                                theRAM.write(0x277, 0x14);
                            } else if (theChar == 27) {
                                // It's escape...
                                // TODO: This isn't handling runstop
                                theRAM.write(0x277, 0x03);
                            } else if (theChar == '\n') {
                                theRAM.write(0x277, 0x0d);
                            } else if (theChar == ' ') {
                                theRAM.write(0x277, 0x20);
                            } else if (theChar == '!') {
                                theRAM.write(0x277, 0x21);
                            } else if (theChar == '"') {
                                theRAM.write(0x277, 0x22);
                            } else if (theChar == '(') {
                                theRAM.write(0x277, 0x28);
                            } else if (theChar == ')') {
                                theRAM.write(0x277, 0x29);
                            } else if (theChar == '*') {
                                theRAM.write(0x277, 0x2a);
                            } else if (theChar == '+') {
                                theRAM.write(0x277, 0x2b);
                            } else if (theChar == ',') {
                                theRAM.write(0x277, 0x2c);
                            } else if (theChar == ':') {
                                theRAM.write(0x277, 0x3a);
                            } else if (theChar == ';') {
                                theRAM.write(0x277, 0x3b);
                            } else if (theChar == '=') {
                                theRAM.write(0x277, 0x3d);
                            } else if (theChar == '?') {
                                theRAM.write(0x277, 0x3f);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        try {
            int bank = theIO.read(0x100, false) & 0x03;
            if (forceBank >= 0) {
                bank = forceBank;
            }
            int bankAddress = bankToAddress[bank];

            theVICII.setRasterLineFromDisplay(displayV);
            int VIC2ScreenControlV = theVICII.read(0x11, false);
            if (addressActive(VIC2ScreenControlV , 0b100000)) {
                isBitmap = true;
            } else {
                isBitmap = false;
            }
            int VIC2ScreenControlH = theVICII.read(0x16, false);
            if (addressActive(VIC2ScreenControlH , 0b10000)) {
                isMulticolour = true;
            } else {
                isMulticolour = false;
            }
            int VIC2MemorySetup = theVICII.read(0x18, false);
            int charMEM = VIC2MemorySetup & 0b1110;
            if (isBitmap) {
                charMEM = VIC2MemorySetup & 0b1110;
            }
            if ((bank == 1 || bank == 3) && (charMEM == 4 || charMEM == 6)) {
                isCHARROM = true;
                charsBitmapAddress = (charMEM & 0x02) * 0x400;
            } else {
                isCHARROM = false;
                charsBitmapAddress = bankAddress + (charMEM * 0x400);
            }
            int screenMEM = VIC2MemorySetup >> 4;
            screenAddress = bankAddress + (screenMEM * 0x400);

            borderColour = theVICII.read(0x20, false);
            backgroundColour = theVICII.read(0x21, false);
            extraBackgroundColour1 = theVICII.read(0x22, false);
            extraBackgroundColour2 = theVICII.read(0x23, false);
            extraBackgroundColour3 = theVICII.read(0x24, false);
        } catch (MemoryAccessException e) {
        }
    }

    private void calculatePixel() {
        vSync = false;

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
            int xpos = displayH - kVIC2SpriteXBorderLeft;
            if ((xpos & 0x07) == 0) {
                xpos /= 8;
                int ypos = displayV - kVIC2SpriteYBorderTop;
                int yLine = ypos & 0b111;
                ypos /= 8;
                try {
                    currentChar = theRAM.read(screenAddress + (ypos * 40) + xpos, false);

                    if (isCHARROM && theCHARGEN != null) {
                        if (isBitmap) {
                            currentCharBits = theCHARGEN.read((charsBitmapAddress + ((xpos + (ypos * 40)) * 8) + yLine) & 0xfff, false);
                        } else {
                            currentCharBits = theCHARGEN.read((charsBitmapAddress + (currentChar * 8) + yLine) & 0xfff, false);
                        }
                    } else {
                        if (isBitmap) {
                            currentCharBits = theRAM.read(charsBitmapAddress + ((xpos + (ypos * 40)) * 8) + yLine, false);
                        } else {
                            currentCharBits = theRAM.read(charsBitmapAddress + (currentChar * 8) + yLine, false);
                        }
                    }

                    currentColour = theColourRAM.read((ypos * 40) + xpos, false) & 0xf;
                } catch (MemoryAccessException e) {
                }
            }
            if (isMulticolour) {
                if ((currentCharBits & 0b11000000) == 0) {
                    panel.fastSetRGB2W(displayH, displayV, palette[backgroundColour & 0xf]);
                } else if ((currentCharBits & 0b11000000) == 0b01000000) {
                    if (isBitmap) {
                        panel.fastSetRGB2W(displayH, displayV, palette[(currentChar >> 4) & 0xf]);
                    } else {
                        panel.fastSetRGB2W(displayH, displayV, palette[extraBackgroundColour1 & 0x0f]);
                    }
                } else if ((currentCharBits & 0b11000000) == 0b10000000) {
                    if (isBitmap) {
                        panel.fastSetRGB2W(displayH, displayV, palette[currentChar & 0xf]);
                    } else {
                        panel.fastSetRGB2W(displayH, displayV, palette[extraBackgroundColour2 & 0x0f]);
                    }
                } else {
                    if (isBitmap) {
                        panel.fastSetRGB2W(displayH, displayV, palette[currentColour & 0xf]);
                    } else {
                        panel.fastSetRGB2W(displayH, displayV, palette[currentColour & 0x07]);
                    }
                }
            } else {
                if ((currentCharBits & 0b10000000) == 0) {
                    if (isBitmap) {
                        panel.fastSetRGB(displayH, displayV, palette[currentChar & 0xf]);
                    } else {
                        panel.fastSetRGB(displayH, displayV, palette[backgroundColour & 0x0f]);
                    }
                } else {
                    if (isBitmap) {
                        panel.fastSetRGB(displayH, displayV, palette[(currentChar >> 4) & 0xf]);
                    } else {
                        panel.fastSetRGB(displayH, displayV, palette[currentColour & 0x0f]);
                    }
                }
            }
            if (isMulticolour) {
                currentCharBits = currentCharBits << 2;
            } else {
                currentCharBits = currentCharBits << 1;
            }
        } else {
            panel.fastSetRGB(displayH, displayV, palette[borderColour & 0x0f]);
            if (isMulticolour) {
                panel.fastSetRGB(displayH+1, displayV, palette[borderColour & 0x0f]);
            }
        }
        if (isMulticolour) {
            displayH+=2;
        } else {
            displayH++;
        }
        if (displayH >= displayWidth) {
            displayH = 0;
            displayV++;
        }
        if (displayV >= displayHeight) {
            displayV = 0;
            vSync = true;

            // Save the frame
            if (leafFilename != null && !leafFilename.isEmpty()) {
                try {
                    File file = new File(leafFilename + String.format("%06d", frameNumberForSync) + ".bmp");
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
            frameNumberForSync++;
        }
    }

    public void writeDebugBMPsToLeafFilename(String leafFilename) {
        this.leafFilename = leafFilename;
    }

    public JFrame getWindow() {
        return window;
    }

    public void setForceBank(int forceBank) {
        this.forceBank = forceBank;
    }
}
