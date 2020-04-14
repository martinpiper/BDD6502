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

			int i = 0;
			while (displayBombJack.isVisible()) {

				BufferedImage img = displayBombJack.getImage();
				Graphics2D graphics = img.createGraphics();
				graphics.setColor(Color.black);
				graphics.clearRect(0, 0, img.getWidth(), img.getHeight());
				graphics.setColor(Color.red);
				graphics.fill(new Rectangle(i, i, 100, 150));
				i++;
				graphics.dispose();
				img.setRGB(101,101, Color.green.getRGB());


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
