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
			displayBombJack.addLayer(new Chars());
			displayBombJack.InitWindow();

			displayBombJack.writeData(0x9e00, 0x01, 0xf0);
//			displayBombJack.writeData(0x9e00, 0x01, 0x30);

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
				if (colour == 0) {
					colour = 1;
				}
				displayBombJack.writeData(0x9400 + i, 0x01, colour | (flips << 6) | (exChars << 4));
			}
//			displayBombJack.writeData(0x905e,0x01,0x41);
			displayBombJack.writeData(0x905f,0x01,0x5f);

			// Add simple character data
			displayBombJack.writeDataFromFile(0x2000,0x20,"C:\\work\\BombJack\\05_k08t.bin");
			displayBombJack.writeDataFromFile(0x4000,0x20,"C:\\work\\BombJack\\04_h08t.bin");
			displayBombJack.writeDataFromFile(0x8000,0x20,"C:\\work\\BombJack\\03_e08t.bin");


			while (displayBombJack.isVisible()) {

				for (int i = 0 ; i < 10000; i++) {
					displayBombJack.calculatePixel();
				}

				displayBombJack.RepaintWindow();

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
