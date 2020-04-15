package com.bdd6502;

import com.replicanet.cukesplus.Main;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.awt.image.BufferedImage;


public class TestRunner
{
	public static void main(String args[]) throws Exception
	{
		if (args.length >= 1 && args[0].compareToIgnoreCase("--exec") == 0) {
			DisplayBombJack displayBombJack = new DisplayBombJack();
			displayBombJack.InitWindow();

			// Setup a simple palette
			// White background
			displayBombJack.writeData(0x9c00,0x01,0xff);
			displayBombJack.writeData(0x9c01,0x01,0x0f);
			// Red pixel colour
			displayBombJack.writeData(0x9c01,0x01,0x0f);
			displayBombJack.writeData(0x9c02,0x01,0x00);

			while (displayBombJack.isVisible()) {

				for (int i = 0 ; i < 1000; i++) {
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
