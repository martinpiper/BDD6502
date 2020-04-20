package com.bdd6502;

import com.replicanet.cukesplus.Main;


public class TestRunner {
    public static void main(String args[]) throws Exception {
        if (args.length >= 1 && args[0].compareToIgnoreCase("--execVideoTest") == 0) {
            DisplayBombJack displayBombJack = new DisplayBombJack();
            displayBombJack.addLayer(new Mode7(0xa000, 0x08));
            displayBombJack.addLayer(new Tiles(0x9e00, 0x80, 0x40));
            displayBombJack.addLayer(new Chars(0x9000, 0x20));
            displayBombJack.addLayer(new Sprites(0x9800, 0x10));
            displayBombJack.InitWindow();

//            displayBombJack.writeData(0x9e00, 0x01, 0xf0);
            displayBombJack.writeData(0x9e00, 0x01, 0x30);

            // Background colour palette index
            displayBombJack.writeData(0x9e07, 0x01, 0x13);

            displayBombJack.writeDataFromFile(0x9c00, 0x01, "C:\\work\\BombJack\\PaletteData.bin");
            // White background
            displayBombJack.writeData(0x9c00, 0x01, 0x33);
            displayBombJack.writeData(0x9c01, 0x01, 0x08);

            // Setup a simple character screen
            for (int i = 0; i < 32 * 32; i += 7) {
                displayBombJack.writeData(0x9000 + i, 0x01, i + ((i / 32) - 2));
                int flips = (i >> 8) & 0x3;
                int exChars = (i >> 9) & 0x3;
                int colour = (i & 0x07);
                displayBombJack.writeData(0x9400 + i, 0x01, colour | (flips << 6) | (exChars << 4));
            }
//            displayBombJack.writeData(0x905e,0x01,0x41);
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
                    displayBombJack.writeData(0x9823 + i + j, 0x01, j * 9);
                }
            }

            int scrollX = 0, scrollY = 0;
            int scrollXTimeout = 50, scrollYTimeout = 150;
            double mode7Rot = 0.0f;
            int frame = 0;
            while (displayBombJack.isVisible()) {

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
//                displayBombJack.writeData(0x9e07, 0x01, scrollX + 0x30);
//                displayBombJack.writeData(0xa014, 0x01, scrollX + 0x30);
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
                        displayBombJack.calculatePixelsUntil(0x1a0, ypos);
                    }

                    displayBombJack.calculatePixelsUntil(0x190, 0xff);
                    mode7Rot += 0.01f;
                } else {
                    displayBombJack.calculatePixelsUntil(0x190, 0xff);
                }

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
                    }
                }

                displayBombJack.RepaintWindow();

                frame++;
                Thread.sleep(10);
            }
        }

        if (args.length == 0) {
            // Use a default command line if it's missing one
//			String temp = "--monochrome --format pretty --format json:cucumber.json --glue TestGlue features";
            String temp = "--monochrome --format pretty --format html:target/cucumber --format json:target/cucumber.json --glue TestGlue features";
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
