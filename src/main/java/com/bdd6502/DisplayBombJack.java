package com.bdd6502;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class DisplayBombJack {

    JFrame window;
    QuickDrawPanel panel;

    int displayWidth = 384;
    int displayHeight = 276;

    public DisplayBombJack() {
    }

    public void InitWindow() {
        InitWindow(800,600);
    }

    public void InitWindow(int width, int height) {
        // Testing window drawing in a loop for eventual graphics updates
        window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.getContentPane().setPreferredSize(new Dimension(width,height));
        window.pack();

        panel = new QuickDrawPanel(displayWidth,displayHeight);
        window.add(panel);

        window.setVisible(true);
    }

    public void RepaintWindow() {
        // Calculate a scale that fits the display without compromising the aspect ratio
        double scaleX = (double)window.getContentPane().getWidth() / (double)displayWidth;
        double scaleY = (double)window.getContentPane().getHeight() / (double)displayHeight;
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
}
