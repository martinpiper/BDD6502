package com.bdd6502;

import com.replicanet.cukesplus.Main;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.PointerByReference;
import de.quippy.javamod.multimedia.MultimediaContainer;
import de.quippy.javamod.multimedia.MultimediaContainerManager;
import de.quippy.javamod.multimedia.mod.ModMixer;
import de.quippy.javamod.system.Helpers;
import mmarquee.automation.ControlType;
import mmarquee.automation.Element;
import mmarquee.automation.UIAutomation;
import mmarquee.automation.controls.*;
import mmarquee.automation.controls.Window;
import mmarquee.automation.pattern.Text;
import mmarquee.uiautomation.TreeScope;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


public class TestRunner {

    static byte[] compressedData = null;
    static int compressedPos = 0;
    static byte escapeByte = 0;
    static int escapeByteRun = 0;
    static int stackedPos = 0;
    static int stackedThisRun = 0;

    static int getNextByte() {
        if(escapeByteRun > 0) {
            escapeByteRun--;
            return Byte.toUnsignedInt(escapeByte);
        }

        if (stackedThisRun > 0) {
            byte data = compressedData[compressedPos++];

            stackedThisRun--;
            if (stackedThisRun == 0) {
                // And restore
                compressedPos = stackedPos;
            }

            return Byte.toUnsignedInt(data);
        }

        byte data = compressedData[compressedPos++];

        if (data == escapeByte) {
            data = compressedData[compressedPos++];
            if (data == 0) {
                // Run of escape bytes
                data = compressedData[compressedPos++];
                escapeByteRun = Byte.toUnsignedInt(data);
                escapeByteRun--;
                return Byte.toUnsignedInt(escapeByte);
            } else {
                stackedPos = compressedPos + 2; // Calculate the next bytes after the escapeByte data
                stackedThisRun = Byte.toUnsignedInt(data);
                // Set the offset to read from
                int newPos = Byte.toUnsignedInt(compressedData[compressedPos++]) | (Byte.toUnsignedInt(compressedData[compressedPos++]) << 8);
                compressedPos = newPos + 4; // +4 for the file header
                stackedThisRun--;
                return Byte.toUnsignedInt(compressedData[compressedPos++]);
            }
        }

        return Byte.toUnsignedInt(data);
    }

    public static void main(String args[]) throws Exception {
        if (args.length >= 1 && args[0].compareToIgnoreCase("--scan") == 0) {
            UIAutomation automation = UIAutomation.getInstance();
            List<Window> windows = automation.getDesktopWindows();
            for (Window window : windows) {
                System.out.println(">>>> Window");
                System.out.println(window.toString());
                System.out.println(window.getName());
                System.out.println(window.getClassName());

                // https://github.com/mmarquee/ui-automation
                // https://mmarquee.github.io/ui-automation/docs/developer.html

                try {
                    Document document = window.getDocument(0);
                    System.out.println("Document " + document.getText());
                } catch (Exception e) {}

                // https://learn.microsoft.com/en-us/windows/win32/winauto/inspect-objects
                // C:\Program Files (x86)\Windows Kits\10\bin\10.0.22621.0\x86\inspect.exe


                // https://learn.microsoft.com/en-us/dotnet/api/system.windows.automation.treescope?view=windowsdesktop-7.0
                TreeScope scope = new TreeScope(TreeScope.SUBTREE);

//                Class c = Class.forName("mmarquee.automation.controls.Window");
                Class c = Class.forName("mmarquee.automation.controls.AutomationBase");
                Method method = c.getDeclaredMethod("findAll", TreeScope.class , PointerByReference.class);
                method.setAccessible(true);
                Object retObj = method.invoke(window, scope, automation.createTrueCondition());
                List<Element> elements = (List<Element>) retObj;

                System.out.println("Num elements " + elements.size());
                for (Element element : elements) {
                    System.out.println(">>>> Element");
                    System.out.println(element.toString());
                    System.out.println(element.getName());
                    System.out.println(element.getClassName());
                    System.out.println(element.getFullDescription());

                    try {
                        ElementBuilder eb = new ElementBuilder(element);
                        TextBox textBox = new TextBox(eb);
                        System.out.println("TextBox " + textBox.getValueFromIAccessible());
                    } catch (Exception e) {}

                    try {
                        ElementBuilder eb = new ElementBuilder(element);
                        EditBox editBox = new EditBox(eb);
                        System.out.println("EditBox " + editBox.getText());
                    } catch (Exception e) {}

                    try {
                        Text t = new Text(element);
                        System.out.println("Text " + t.getText());
                    } catch (Exception e) {}


//                    WinDef.HWND hwnd = window.getNativeWindowHandle();
//                    User32.INSTANCE.PostMessage(hwnd, 0,0,0);
                    // com.sun.jna.platform.win32
                    // WinUser.WM_KEYDOWN

                    // Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    // System.out.println(clipboard.getData(DataFlavor.stringFlavor));

                    /*
                    List<Element> elements2 = element.findAll(scope, automation.createTrueCondition());
                    System.out.println("Num elements2 " + elements.size());
                    for (Element element2 : elements2) {
                        System.out.println(">>>> Element2");
                        System.out.println(element2.toString());
                        System.out.println(element2.getName());
                        System.out.println(element2.getClassName());
                        System.out.println(element2.getFullDescription());
                    }
                    */

                }

            }
            return;
        }
        if (args.length >= 1 && args[0].compareToIgnoreCase("--execVideoTest") == 0) {
            DisplayBombJack displayBombJack = new DisplayBombJack();
//            displayBombJack.writeDebugBMPsToLeafFilename("target/frames/execVideoTest-");
            displayBombJack.enableDebugData();
            displayBombJack.addLayer(new Mode7(0xa000, 0x08));
            displayBombJack.addLayer(new Tiles(0x9e00, 0x80, 0x40));
            displayBombJack.addLayer(new Chars(0x9000, 0x20));
            displayBombJack.addLayer(new Sprites(0x9800, 0x10));
            displayBombJack.InitWindow();

//            displayBombJack.writeData(0x9e00, 0x01, 0xf0);
            // Default layer order
            displayBombJack.writeData(0x9e08, 0x01, 0xe4);
            // Just display, no tiles
            displayBombJack.writeData(0x9e00, 0x01, 0x20);

            // Background colour palette index
            displayBombJack.writeData(0x9e07, 0x01, 0x13);

            displayBombJack.writeDataFromFile(0x9c00, 0x01, "C:\\work\\BombJack\\PaletteData.bin");
            // White background
            displayBombJack.writeData(0x9c00, 0x01, 0x33);
            displayBombJack.writeData(0x9c01, 0x01, 0x08);

            // Setup a simple character screen
            for (int i = 0; i < 32 * 32; i += 7) {
//            for (int i = 0; i < 32 * 32; i++) {
                displayBombJack.writeData(0x9000 + i, 0x01, i + ((i / 32) - 2));
//                displayBombJack.writeData(0x9000 + i, 0x01, i/8);
//                displayBombJack.writeData(0x9000 + i, 0x01, i);
                int flips = (i >> 8) & 0x3;
                int exChars = (i >> 9) & 0x3;
                int colour = (i & 0x07);
//                flips = 0;
//                exChars = 0;
//                colour = 1;
                displayBombJack.writeData(0x9400 + i, 0x01, colour | (flips << 6) | (exChars << 4));
            }

            // Char screen palette lo/hi
            //displayBombJack.writeData(0x9000, 0x01, 1);

            // Debug top right char screen, A then graphics vertical line on right (not visible due to border), horizontal line on bottom
            displayBombJack.writeData(0x905e,0x01,0x41);
            displayBombJack.writeData(0x905f, 0x01, 0x5f);

            // Add character data
            displayBombJack.writeDataFromFile(0x2000, 0x20, "C:\\work\\BombJack\\05_k08t.bin");
            displayBombJack.writeDataFromFile(0x4000, 0x20, "C:\\work\\BombJack\\04_h08t.bin");
            displayBombJack.writeDataFromFile(0x8000, 0x20, "C:\\work\\BombJack\\03_e08t.bin");

            // Add tile data
            displayBombJack.writeDataFromFile(0x2000, 0x40, "C:\\work\\BombJack\\08_r08t.bin");
            displayBombJack.writeDataFromFile(0x4000, 0x40, "C:\\work\\BombJack\\07_n08t.bin");
            displayBombJack.writeDataFromFile(0x8000, 0x40, "C:\\work\\BombJack\\06_l08t.bin");
            // Setup a simple tile screen
            for (int i = 0; i < 64 * 64; i += 7) {
                displayBombJack.writeData(0x2000 + i, 0x80, i + ((i / 7) & 1));
                int flips = (i >> 8) & 0x3;
                int colour = (i & 0x1f);
                displayBombJack.writeData(0x3000 + i, 0x80, colour | (flips << 6));
            }
            // Setup the sphinx test
            for (int i = 0; i < 16; i++) {
                displayBombJack.writeDataFromFile(0x2000 + (i * 0x40), 0x80, "C:\\work\\BombJack\\02_p04t.bin", 0x400 + (i * 0x10), 0x10);
                displayBombJack.writeDataFromFile(0x3000 + (i * 0x40), 0x80, "C:\\work\\BombJack\\02_p04t.bin", 0x500 + (i * 0x10), 0x10);
            }

            // Mode7 tiles
            displayBombJack.writeDataFromFile(0x2000, 0x08, "C:\\work\\BombJack\\map.bin");
            displayBombJack.writeDataFromFile(0x4000, 0x08, "C:\\work\\BombJack\\Mode7.bin");
            displayBombJack.writeDataFromFile(0x8000, 0x08, "C:\\work\\BombJack\\Mode7B.bin");
            // Mode7 registers
            displayBombJack.writeData(0xa000, 0x01, 0x00);
            displayBombJack.writeData(0xa001, 0x01, 0x01);
            displayBombJack.writeData(0xa002, 0x01, 0x00);

            displayBombJack.writeData(0xa003, 0x01, 0x00);
            displayBombJack.writeData(0xa004, 0x01, 0x00);
            displayBombJack.writeData(0xa005, 0x01, 0x00);

            displayBombJack.writeData(0xa006, 0x01, 0x00);
            displayBombJack.writeData(0xa007, 0x01, 0x01);
            displayBombJack.writeData(0xa008, 0x01, 0x00);

            displayBombJack.writeData(0xa009, 0x01, 0x00);
            displayBombJack.writeData(0xa00a, 0x01, 0x00);
            displayBombJack.writeData(0xa00b, 0x01, 0x00);

            // Background colour
            displayBombJack.writeData(0xa014, 0x01, 0x14);
            // Enable flags
            displayBombJack.writeData(0xa015, 0x01, 0x1f);

            // Sprite data
            displayBombJack.writeDataFromFile(0x2000, 0x10, "C:\\work\\BombJack\\14_j07b.bin");
            displayBombJack.writeDataFromFile(0x4000, 0x10, "C:\\work\\BombJack\\15_l07b.bin");
            displayBombJack.writeDataFromFile(0x8000, 0x10, "C:\\work\\BombJack\\16_m07b.bin");
            // Sprite registers
            // Start/end 32x32 sprites
            displayBombJack.writeData(0x9a00, 0x01, 0x14);    // Plus sprite enable
            displayBombJack.writeData(0x9a01, 0x01, 0x08);
            // Sprite 0
            for (int i = 0; i < 24 * 4; i += 6 * 4) {
                for (int j = 0; j < 6 * 4; j += 4) {
                    displayBombJack.writeData(0x9820 + i + j, 0x01, 0x04);
                    displayBombJack.writeData(0x9821 + i + j, 0x01, 0x01);
                    displayBombJack.writeData(0x9822 + i + j, 0x01, 16 + (int) (i * 2.5f));
                    displayBombJack.writeData(0x9823 + i + j, 0x01, (int)(j * 12.0f));
                }
            }

            // Setup debug tiles scroll
//            displayBombJack.writeData(0x9e01, 0x01, 0xe7);
//            displayBombJack.writeData(0x9e02, 0x01, 0xf);
//            displayBombJack.writeData(0x9e03, 0x01, 0x00);
//            displayBombJack.writeData(0x9e04, 0x01, 0x0);

            int scrollX = 0, scrollY = 0;
            int scrollXTimeout = 50, scrollYTimeout = 150;
            double mode7Rot = 0.0f;
            int frame = 0;

            while (displayBombJack.isVisible() && frame < 100) {
                displayBombJack.calculatePixelsUntilVSync();

                displayBombJack.writeData(0xa000, 0x01, 0);
                displayBombJack.writeData(0xa001, 0x01, 1);
                displayBombJack.writeData(0xa002, 0x01, 0);

                displayBombJack.writeData(0xa003, 0x01, 0);
                displayBombJack.writeData(0xa004, 0x01, 0);
                displayBombJack.writeData(0xa005, 0x01, 0);

                displayBombJack.writeData(0xa006, 0x01, 0);
                displayBombJack.writeData(0xa007, 0x01, 1);
                displayBombJack.writeData(0xa008, 0x01, 0);

                displayBombJack.writeData(0xa009, 0x01, 0);
                displayBombJack.writeData(0xa00a, 0x01, 0);
                displayBombJack.writeData(0xa00b, 0x01, 0);

                // Test mode7 internal register reset logic

                // Test complex perspective mode7 register updates
                for (int ypos = 100; ypos < 240; ypos++) {
                    double scaleValue = (256.0f * 400.0f) / ypos;
                    displayBombJack.calculatePixelsUntil(0x1b0, ypos);
                    displayBombJack.writeData(0xa014, 0x01, ypos);
                    // Reset internal counters
                    displayBombJack.writeData(0xa015, 0x01, 0);


                    double dx = Math.sin((mode7Rot + (Math.PI / 2.0f))) * scaleValue;
                    int intValue = (int) dx;
                    displayBombJack.writeData(0xa000, 0x01, intValue);
                    displayBombJack.writeData(0xa001, 0x01, intValue >> 8);
                    displayBombJack.writeData(0xa002, 0x01, intValue >> 16);

                    double dxy = Math.sin(mode7Rot) * scaleValue;
                    intValue = (int) dxy;
                    displayBombJack.writeData(0xa003, 0x01, intValue);
                    displayBombJack.writeData(0xa004, 0x01, intValue >> 8);
                    displayBombJack.writeData(0xa005, 0x01, intValue >> 16);

                    double dy = -Math.sin(mode7Rot + (Math.PI / 2.0f) + (Math.PI / 2.0f) + (Math.PI / 2.0f)) * scaleValue;
                    intValue = (int) dy;
                    displayBombJack.writeData(0xa006, 0x01, intValue);
                    displayBombJack.writeData(0xa007, 0x01, intValue >> 8);
                    displayBombJack.writeData(0xa008, 0x01, intValue >> 16);
                    double dyx = Math.sin(mode7Rot + (Math.PI / 2.0f) + (Math.PI / 2.0f)) * scaleValue;
                    intValue = (int) dyx;
                    displayBombJack.writeData(0xa009, 0x01, intValue);
                    displayBombJack.writeData(0xa00a, 0x01, intValue >> 8);
                    displayBombJack.writeData(0xa00b, 0x01, intValue >> 16);

                    // xpos/ypos org calculation, note how the coordinates project back along the deltas calculated above
                    // xorg neg dx + yorg neg dxy
                    intValue = (int) (((double) frame * 256.0f) + (192.5f * -dx) + (64.5f * -dxy));
                    displayBombJack.writeData(0xa00c, 0x01, intValue);
                    displayBombJack.writeData(0xa00d, 0x01, intValue >> 8);
                    displayBombJack.writeData(0xa00e, 0x01, intValue >> 16);
                    // xorg neg dyx + yorg neg dy
                    intValue = (int) ((192.5f * -dyx) + (64.5f * -dy));
                    displayBombJack.writeData(0xa00f, 0x01, intValue);
                    displayBombJack.writeData(0xa010, 0x01, intValue >> 8);
                    displayBombJack.writeData(0xa011, 0x01, intValue >> 16);

                    // Simulate updating the registers later on
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa015, 0x01, 0x1f);




                    // Vertical splits
                    scaleValue = (256.0f * 200.0f) / ypos;
                    displayBombJack.calculatePixelsUntil(0x40, ypos);
                    // Reset internal counters
                    displayBombJack.writeData(0xa015, 0x01, 0);


                    dx = Math.sin((-mode7Rot + (Math.PI / 2.0f))) * scaleValue;
                    intValue = (int) dx;
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa000, 0x01, intValue);
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa001, 0x01, intValue >> 8);
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa002, 0x01, intValue >> 16);

                    dxy = Math.sin(-mode7Rot) * scaleValue;
                    intValue = (int) dxy;
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa003, 0x01, intValue);
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa004, 0x01, intValue >> 8);
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa005, 0x01, intValue >> 16);

                    dy = -Math.sin(-mode7Rot + (Math.PI / 2.0f) + (Math.PI / 2.0f) + (Math.PI / 2.0f)) * scaleValue;
                    intValue = (int) dy;
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa006, 0x01, intValue);
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa007, 0x01, intValue >> 8);
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa008, 0x01, intValue >> 16);
                    dyx = Math.sin(-mode7Rot + (Math.PI / 2.0f) + (Math.PI / 2.0f)) * scaleValue;
                    intValue = (int) dyx;
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa009, 0x01, intValue);
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa00a, 0x01, intValue >> 8);
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa00b, 0x01, intValue >> 16);

                    // xpos/ypos org calculation, note how the coordinates project back along the deltas calculated above
                    // xorg neg dx + yorg neg dxy
                    intValue = (int) (((double) frame * 256.0f) + (292.5f * -dx) + (164.5f * -dxy));
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa00c, 0x01, intValue);
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa00d, 0x01, intValue >> 8);
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa00e, 0x01, intValue >> 16);
                    // xorg neg dyx + yorg neg dy
                    intValue = (int) ((292.5f * -dyx) + (164.5f * -dy));
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa00f, 0x01, intValue);
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa010, 0x01, intValue >> 8);
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa011, 0x01, intValue >> 16);

                    // Simulate updating the registers later on
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa015, 0x01, 0x1f);



                    displayBombJack.calculatePixelsUntil(0x80, ypos);
                    // Reset some internal counters for yx and x
                    displayBombJack.writeData(0xa015, 0x01, 0x05);
                    dx = -dx;
                    intValue = (int) dx;
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa000, 0x01, intValue);
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa001, 0x01, intValue >> 8);
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa002, 0x01, intValue >> 16);

                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa015, 0x01, 0x1f);

                    displayBombJack.calculatePixelsUntil(0xc0, ypos);
                    // Reset some internal counters for x and xy
                    displayBombJack.writeData(0xa015, 0x01, 0x03);

                    dxy = -dxy;
                    intValue = (int) dxy;
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa003, 0x01, intValue);
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa004, 0x01, intValue >> 8);
                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa005, 0x01, intValue >> 16);

                    displayBombJack.calculatePixel();
                    displayBombJack.writeData(0xa015, 0x01, 0x1f);
                }

                mode7Rot += 0.01f;

                displayBombJack.RepaintWindow();

                frame++;
                Thread.sleep(10);
            }

            // Now enable tiles
            displayBombJack.writeData(0x9e00, 0x01, 0x30);

            while (displayBombJack.isVisible()) {

                displayBombJack.calculatePixelsUntilVSync();

                // Test sprite animation
                if (true) {
                    for (int i = 0; i < 24; i++) {
                        int xpos = (int) (120.0f + Math.sin((((double) frame) / 50.0f) + (((double) i) / 2.0f)) * 120.0f);
                        int ypos = (int) (120.0f + Math.cos((((double) frame) / 75.0f) + (((double) i) / 5.0f)) * 120.0f);
                        displayBombJack.writeData(0x9820 + (i * 4), 0x01, i + (frame / 10));
                        int fullHeight = 0;
                        if (i <= 3) {
                            fullHeight = 0x20;
                        }
                        displayBombJack.writeData(0x9821 + (i * 4), 0x01, i | (((frame / 50) & 0x03) << 6) | fullHeight);
                        displayBombJack.writeData(0x9822 + (i * 4), 0x01, ypos);
                        displayBombJack.writeData(0x9823 + (i * 4), 0x01, xpos);

                        // Exercise sprite contention timings
                        for (int j = 0; j < 8; j++) {
//                            displayBombJack.calculatePixel();
                        }
                    }
                }

                // Contention timing test
                if (false) {
                    for (int i = 0; i < 384 * 64; i++) {
                        displayBombJack.calculatePixel();
                    }
                    // Tests the bus contention timing works
                    for (int i = 0; i < 101; i++) {
                        for (int j = 0; j < 8; j++) {
                            displayBombJack.calculatePixel();
                        }
                        displayBombJack.writeData(0x9c00, 0x01, 0x33);
                    }
                    for (int i = 0; i < 384 * 64; i++) {
                        displayBombJack.calculatePixel();
                    }
                    for (int i = 0; i < 101; i++) {
                        for (int j = 0; j < 8; j++) {
                            displayBombJack.calculatePixel();
                        }
//					displayBombJack.writeData(0x9000, 0x01, 0x00);
                    }

                    // Testing the tiles background colour write
                    for (int i = 0; i < 101; i++) {
                        for (int j = 0; j < 8; j++) {
                            displayBombJack.calculatePixel();
                        }
                        displayBombJack.writeData(0x9e07, 0x01, i);
                        displayBombJack.writeData(0xa014, 0x01, i);
                    }

                    // Flash background colour
//                    displayBombJack.writeData(0x9e07, 0x01, scrollX + 0x30);  // Tiles
//                    displayBombJack.writeData(0xa014, 0x01, scrollX + 0x30);  // Mode7
                }

                if (true) {
                    if (scrollXTimeout-- < 0) {
                        scrollX++;
                        displayBombJack.writeData(0x9e01, 0x01, scrollX);
                        displayBombJack.writeData(0x9e02, 0x01, scrollX >> 8);
                        displayBombJack.writeData(0x9e00, 0x01, 0xf0);
                    }
                    if (scrollYTimeout-- < 0) {
                        scrollY++;
                        displayBombJack.writeData(0x9e03, 0x01, scrollY);
                        displayBombJack.writeData(0x9e04, 0x01, scrollY >> 8);
                        displayBombJack.writeData(0x9e00, 0x01, 0xf0);
                    }
                }

                if (true) {
                    // Test complex perspective mode7 register updates
                    double scaleValue = 256 + 32 + (Math.sin(mode7Rot * 5.0f) * 256);
                    for (int ypos = 100; ypos < 240; ypos++) {
                        displayBombJack.writeData(0xa014, 0x01, ypos);

                        double dx = Math.sin((mode7Rot + (Math.PI / 2.0f))) * scaleValue;
                        int intValue = (int) dx;
                        displayBombJack.writeData(0xa000, 0x01, intValue);
                        displayBombJack.writeData(0xa001, 0x01, intValue >> 8);
                        displayBombJack.writeData(0xa002, 0x01, intValue >> 16);

                        double dxy = Math.sin(mode7Rot) * scaleValue;
                        intValue = (int) dxy;
                        displayBombJack.writeData(0xa003, 0x01, intValue);
                        displayBombJack.writeData(0xa004, 0x01, intValue >> 8);
                        displayBombJack.writeData(0xa005, 0x01, intValue >> 16);

                        double dy = -Math.sin(mode7Rot + (Math.PI / 2.0f) + (Math.PI / 2.0f) + (Math.PI / 2.0f)) * scaleValue;
                        intValue = (int) dy;
                        displayBombJack.writeData(0xa006, 0x01, intValue);
                        displayBombJack.writeData(0xa007, 0x01, intValue >> 8);
                        displayBombJack.writeData(0xa008, 0x01, intValue >> 16);
                        double dyx = Math.sin(mode7Rot + (Math.PI / 2.0f) + (Math.PI / 2.0f)) * scaleValue;
                        intValue = (int) dyx;
                        displayBombJack.writeData(0xa009, 0x01, intValue);
                        displayBombJack.writeData(0xa00a, 0x01, intValue >> 8);
                        displayBombJack.writeData(0xa00b, 0x01, intValue >> 16);

                        // xpos/ypos org calculation, note how the coordinates project back along the deltas calculated above
                        // xorg neg dx + yorg neg dxy
                        intValue = (int) (((double) frame * 256.0f) + (192.5f * -dx) + (64.5f * -dxy));
                        displayBombJack.writeData(0xa00c, 0x01, intValue);
                        displayBombJack.writeData(0xa00d, 0x01, intValue >> 8);
                        displayBombJack.writeData(0xa00e, 0x01, intValue >> 16);
                        // xorg neg dyx + yorg neg dy
                        intValue = (int) ((192.5f * -dyx) + (64.5f * -dy));
                        displayBombJack.writeData(0xa00f, 0x01, intValue);
                        displayBombJack.writeData(0xa010, 0x01, intValue >> 8);
                        displayBombJack.writeData(0xa011, 0x01, intValue >> 16);

                        scaleValue = (256.0f * 400.0f) / ypos;
                        // This is just off screen
                        displayBombJack.calculatePixelsUntil(0x188, ypos);
                    }

                    mode7Rot += 0.01f;
                }

                displayBombJack.RepaintWindow();

                frame++;
                Thread.sleep(10);
            }

            System.exit(0);
        }

        if (args.length >= 1 && args[0].compareToIgnoreCase("--exportmod") == 0) {
            Helpers.registerAllClasses();

            MultimediaContainer loaded = MultimediaContainerManager.getMultimediaContainer(args[1]);
//            loaded.createNewMixer().startPlayback();
            String filename = args[2];
            loaded.createNewMixer().fastExport(filename, Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            int bestLen = 6;
            int bestSize = -1;

            // Try various thresholds
            for (int i = 3 ; i < 30 ; i++) {
                System.out.println("Testing length threshold: " + i);
                int theSize = CompressData.compressMusicData(filename , i);
                if (bestSize == -1 || theSize < bestSize) {
                    System.out.println("** New best");
                    bestSize = theSize;
                    bestLen = i;
                }
                // Early out if the best size is increasing too far beyond the current best
                if (theSize > (bestSize + (bestSize/10))) {
                    break;
                }
            }

            System.out.println("Best length threshold: " + bestLen);
            CompressData.compressMusicData(filename , bestLen);

            System.exit(0);
        }

        if (args.length >= 1 && args[0].compareToIgnoreCase("--playmod") == 0) {
            // Test output data
            AudioExpansion audioExpansion = new AudioExpansion();
            audioExpansion.writeDataFromFile(0, 0x04, args[1] + ModMixer.SAMPLES_BIN);

            audioExpansion.writeData(0x8041, 0x01, 0);

            audioExpansion.writeData(0x8000, 0x01, 0xff);
            audioExpansion.writeData(0x8003, 0x01, 0xff);
            audioExpansion.writeData(0x8004, 0x01, 0xff);
            audioExpansion.writeData(0x8005, 0x01, 67);
            audioExpansion.writeData(0x8006, 0x01, 0);

            audioExpansion.writeData(0x8041, 0x01, 0x01);

            // Now try decompression
            compressedData = Files.readAllBytes(Paths.get(args[1] + ModMixer.EVENTS_CMP));
            int originalLength = Byte.toUnsignedInt(compressedData[0]) | (Byte.toUnsignedInt(compressedData[1]) << 8) | (Byte.toUnsignedInt(compressedData[2]) << 16);
            escapeByte = compressedData[3];

            compressedPos = 4;

            int pos = 0;

            int sampleStarts[] = new int[256];
            int sampleLengths[] = new int[256];
            int sampleLoopStarts[] = new int[256];
            int sampleLoopLengths[] = new int[256];

            audioExpansion.start();
            long startTime = System.currentTimeMillis();
            int waitUntil = 0;
            int channelPlayingMask = 0;
            boolean doPlay = true;
            while (compressedPos < compressedData.length && doPlay) {
                while (System.currentTimeMillis() < (startTime+waitUntil)) {
                    for (int i = 0; i < 50; i++) {
                        audioExpansion.calculateSamples();
                    }
                    Thread.sleep(1);
                }
                int command = getNextByte();
                switch (command & Helpers.kMusicCommandMask) {
                    case Helpers.kMusicCommandWaitFrames: {
                        waitUntil += (getNextByte() * (1000 / 60));
                        continue;
                    }
                    case Helpers.kMusicCommandSetSampleData: {
                        int sampleIndex = getNextByte();
                        sampleStarts[sampleIndex] = getNextByte() | (getNextByte() << 8);
                        sampleLengths[sampleIndex] = getNextByte() | (getNextByte() << 8);
                        sampleLoopStarts[sampleIndex] = getNextByte() | (getNextByte() << 8);
                        sampleLoopLengths[sampleIndex] = getNextByte() | (getNextByte() << 8);
                        continue;
                    }
                    case Helpers.kMusicCommandPlayNote: {
                        int channel = command & Helpers.kMusicCommandChannelMask;
                        int volume = getNextByte();
                        int sampleIndex = getNextByte();
                        int sampleStart = sampleStarts[sampleIndex];
                        int sampleLength = sampleLengths[sampleIndex];
                        int sampleLoopStart = sampleLoopStarts[sampleIndex];
                        int sampleLoopLength = sampleLoopLengths[sampleIndex];

                        int sampleFrequency = getNextByte() | (getNextByte() << 8);

                        channelPlayingMask = channelPlayingMask & ~(1<<channel);
                        audioExpansion.writeData(0x8000 + (AudioExpansion.numVoices * AudioExpansion.voiceSize) + 1, 0x01, channelPlayingMask);

                        int voiceAddress = 0x8000 + (channel * AudioExpansion.voiceSize);
                        audioExpansion.writeData(voiceAddress, 0x01, volume);
                        audioExpansion.writeData(voiceAddress+1, 0x01, sampleStart);
                        audioExpansion.writeData(voiceAddress+2, 0x01, sampleStart>>8);
                        audioExpansion.writeData(voiceAddress+3, 0x01, sampleLength);
                        audioExpansion.writeData(voiceAddress+4, 0x01, sampleLength>>8);
                        audioExpansion.writeData(voiceAddress+5, 0x01, sampleFrequency);
                        audioExpansion.writeData(voiceAddress+6, 0x01, sampleFrequency>>8);

                        audioExpansion.writeData(voiceAddress+7, 0x01, sampleLoopStart);
                        audioExpansion.writeData(voiceAddress+8, 0x01, sampleLoopStart>>8);
                        audioExpansion.writeData(voiceAddress+9, 0x01, sampleLoopLength);
                        audioExpansion.writeData(voiceAddress+10, 0x01, sampleLoopLength>>8);

                        channelPlayingMask = channelPlayingMask | (1<<channel);
                        audioExpansion.writeData(0x8000 + (AudioExpansion.numVoices * AudioExpansion.voiceSize) + 1, 0x01, channelPlayingMask);
                        break;
                    }
                    case Helpers.kMusicCommandAdjustNote: {
                        int channel = command & Helpers.kMusicCommandChannelMask;
                        int volume = getNextByte();

                        int sampleFrequency = getNextByte() | (getNextByte() << 8);

                        int voiceAddress = 0x8000 + (channel * AudioExpansion.voiceSize);
                        audioExpansion.writeData(voiceAddress, 0x01, volume);
                        audioExpansion.writeData(voiceAddress+5, 0x01, sampleFrequency);
                        audioExpansion.writeData(voiceAddress+6, 0x01, sampleFrequency>>8);
                        break;
                    }
                    case Helpers.kMusicCommandAdjustVolume: {
                        int channel = command & Helpers.kMusicCommandChannelMask;
                        int volume = getNextByte();

                        int voiceAddress = 0x8000 + (channel * AudioExpansion.voiceSize);
                        audioExpansion.writeData(voiceAddress, 0x01, volume);
                        break;
                    }
                    case Helpers.kMusicCommandStop: {
                        System.out.println("kMusicCommandStop");
                        doPlay = false;
                        break;
                    }
                    default: {
                        throw new Exception("eek");
                    }
                }
            }
            audioExpansion.line.close();

            System.exit(0);
        }
        if (args.length >= 1 && args[0].compareToIgnoreCase("--execAudioTest") == 0) {
            AudioExpansion audioExpansion = new AudioExpansion();
            // Converted using: C:\Downloads\ImageMagick-7.0.7-4-portable-Q16-x64\ffmpeg.exe -i <input> -y -acodec pcm_u8 -ar 22050 -ac 1 t.wav
            // Then the wav header was removed and file set to 0x10000 bytes
            audioExpansion.writeDataFromFile(0, 0x04, "testdata/sample.pcmu8");

            // Set no voices active
            audioExpansion.writeData(0x8000 + (AudioExpansion.numVoices * AudioExpansion.voiceSize) + 1, 0x01, 0);

            // Voice 0
            audioExpansion.writeData(0x8000, 0x01, 0xff);
            // Start
//            audioExpansion.writeData(0x8001, 0x01, 0x90);
//            audioExpansion.writeData(0x8002, 0x01, 0x71);
            // Length
            audioExpansion.writeData(0x8003, 0x01, 0xff);
            audioExpansion.writeData(0x8004, 0x01, 0xff);
            int rate = AudioExpansion.calculateRateFromFrequency(22050);
            audioExpansion.writeData(0x8005, 0x01, rate);
            audioExpansion.writeData(0x8006, 0x01, rate >> 8);
            // Loop start
            audioExpansion.writeData(0x8007, 0x01, 0x07);
            audioExpansion.writeData(0x8008, 0x01, 0x10);
            // Loop size
            audioExpansion.writeData(0x8009, 0x01, 0x80);
            audioExpansion.writeData(0x800a, 0x01, 0x18);
            // Set voice 0 loop
            audioExpansion.writeData(0x8000 + (AudioExpansion.numVoices * AudioExpansion.voiceSize), 0x01, 0x01);

            // Set voices active
            audioExpansion.writeData(0x8000 + (AudioExpansion.numVoices * AudioExpansion.voiceSize) + 1, 0x01, 0x01);

            audioExpansion.start();
            for (int i = 0 ; i < 100 ; i++) {
                Thread.sleep(16);
                for (int j = 0 ; j < 100 ; i++) {
                    audioExpansion.calculateSamples();
                }
            }
            audioExpansion.line.close();

            System.exit(0);
        }

        if (args.length == 0) {
            // Use a default command line if it's missing one
//			String temp = "--monochrome --format pretty --format json:cucumber.json --glue TestGlue features";
            String temp = "--tags ~@ignore --monochrome --format pretty --format html:target/cucumber --format json:target/cucumber.json --glue TestGlue features";
            System.out.println("Using default command line options: " + temp);
            args = temp.split(" ");
        }
        try {
            Main.main(args);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
