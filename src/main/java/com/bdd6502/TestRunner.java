package com.bdd6502;

import com.replicanet.cukesplus.Main;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.awt.image.BufferedImage;

class QuickDrawPanel extends JPanel {
	BufferedImage image;
	Dimension size = new Dimension();

	public QuickDrawPanel(BufferedImage image) {
		this.image = image;
		size.width  = image.getWidth();
		size.height = image.getHeight();
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
//		g.drawImage(image, 0, 0, this);
		g.drawImage(image, 0, 0,  size.width , size.height, this);
	}

	public Dimension getPreferredSize() {
		return size;
	}
}

public class TestRunner
{
	public static void main(String args[]) throws Exception
	{
		if (false) {
			// Testing window drawing in a loop for eventual graphics updates
			JFrame window = new JFrame();
			window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//			window.setBounds(30, 30, 300, 300);
//			window.setSize(600, 400);
			window.getContentPane().setPreferredSize(new Dimension(600,400));
			window.pack();

			BufferedImage img = new BufferedImage(384, 276, BufferedImage.TYPE_INT_RGB);
			QuickDrawPanel panel = new QuickDrawPanel(img);
			window.add(panel);

//		window.getGraphics().drawImage(img,0,0, window);
//		window.repaint();

			int i = 0;
			window.setVisible(true);
			while (window.isVisible()) {
				panel.size.setSize(window.getContentPane().getWidth(),window.getContentPane().getHeight());

				Graphics2D graphics = img.createGraphics();
				graphics.setColor(Color.black);
				graphics.clearRect(0, 0, img.getWidth(), img.getHeight());
				graphics.setColor(Color.red);
				graphics.fill(new Rectangle(i, i, 100, 150));
				i++;
				graphics.dispose();
				img.setRGB(101,101, Color.green.getRGB());
				window.repaint();

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
