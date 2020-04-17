package com.bdd6502;

import com.replicanet.cukesplus.Main;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;


public class TestRunner
{
	public static void main(String args[]) throws Exception
	{
		if (args.length >= 1 && args[0].compareToIgnoreCase("--exec") == 0) {
			DisplayBombJack displayBombJack = new DisplayBombJack();
			displayBombJack.addLayer(new Tiles());
			displayBombJack.addLayer(new Chars());
			displayBombJack.InitWindow();

//			displayBombJack.writeData(0x9e00, 0x01, 0xf0);
			displayBombJack.writeData(0x9e00, 0x01, 0x30);

			// Background colour palette index
			displayBombJack.writeData(0x9e07, 0x01, 0x13);

			displayBombJack.writeDataFromFile(0x9c00,0x01,"C:\\work\\BombJack\\PaletteData.bin");
			// White background
			displayBombJack.writeData(0x9c00, 0x01, 0x33);
			displayBombJack.writeData(0x9c01, 0x01, 0x08);

			// Setup a simple character screen
			for (int i = 0 ; i < 32*32 ; i++) {
				displayBombJack.writeData(0x9000 + i, 0x01, i + ((i/32)-2));
				int flips = (i>>8) & 0x3;
				int exChars = (i>>9) & 0x3;
				int colour = (i&0x07);
				displayBombJack.writeData(0x9400 + i, 0x01, colour | (flips << 6) | (exChars << 4));
			}
//			displayBombJack.writeData(0x905e,0x01,0x41);
			displayBombJack.writeData(0x905f,0x01,0x5f);

			// Add character data
			displayBombJack.writeDataFromFile(0x2000,0x20,"C:\\work\\BombJack\\05_k08t.bin");
			displayBombJack.writeDataFromFile(0x4000,0x20,"C:\\work\\BombJack\\04_h08t.bin");
			displayBombJack.writeDataFromFile(0x8000,0x20,"C:\\work\\BombJack\\03_e08t.bin");

			// Add tile data
			displayBombJack.writeDataFromFile(0x2000,0x40,"C:\\work\\BombJack\\08_r08t.bin");
			displayBombJack.writeDataFromFile(0x4000,0x40,"C:\\work\\BombJack\\07_n08t.bin");
			displayBombJack.writeDataFromFile(0x8000,0x40,"C:\\work\\BombJack\\06_l08t.bin");
			// Setup a simple tile screen
			for (int i = 0 ; i < 64*64 ; i++) {
				displayBombJack.writeData(0x2000 + i, 0x80, i + ((i/7)&1));
				int flips = (i>>8) & 0x3;
				int colour = (i&0x1f);
				displayBombJack.writeData(0x3000 + i, 0x80, colour | (flips << 6));
			}
			// Setup the sphinx test
			for (int i = 0 ; i < 16 ; i++) {
				displayBombJack.writeDataFromFile(0x2000 + (i*0x40), 0x80, "C:\\work\\BombJack\\02_p04t.bin", 0x400 + (i*0x10), 0x10);
				displayBombJack.writeDataFromFile(0x3000 + (i*0x40), 0x80, "C:\\work\\BombJack\\02_p04t.bin", 0x500 + (i*0x10), 0x10);
			}

			int scrollX = 0, scrollY = 0;
			int scrollXTimeout = 50,scrollYTimeout = 150;
			while (displayBombJack.isVisible()) {

				for (int i = 0 ; i < 384*64; i++) {
					displayBombJack.calculatePixel();
				}
				// Tests the bus contention timing works
				for (int i = 0 ; i < 101; i++) {
					for (int j = 0 ; j < 8; j++) {
						displayBombJack.calculatePixel();
					}
					displayBombJack.writeData(0x9c00, 0x01, 0x33);
				}
				for (int i = 0 ; i < 384*64; i++) {
					displayBombJack.calculatePixel();
				}
				for (int i = 0 ; i < 101; i++) {
					for (int j = 0 ; j < 8; j++) {
						displayBombJack.calculatePixel();
					}
//					displayBombJack.writeData(0x9000, 0x01, 0x00);
				}

				// Testing the tiles background colour write
				for (int i = 0 ; i < 101; i++) {
					for (int j = 0 ; j < 8; j++) {
						displayBombJack.calculatePixel();
					}
					displayBombJack.writeData(0x9e07, 0x01, i);
				}
				displayBombJack.writeData(0x9e07, 0x01, scrollX + 0x30);

				displayBombJack.RepaintWindow();

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

				Thread.sleep(10);
			}
		}

		if (args.length == 0)
		{
			// Use a default command line if it's missing one
//			String temp = "--monochrome --format pretty --format json:cucumber.json --glue TestGlue features";
			String temp = "--monochrome --format pretty --format html:target/cucumber --format json:target/cucumber.json --glue TestGlue features";
			System.out.println("Using default command line options: " + temp);
			args = temp.split(" ");
		}
		try
		{
			Main.main(args);
		}
		catch (Throwable throwable)
		{
			throwable.printStackTrace();
		}
	}
}
