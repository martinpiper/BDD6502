package com.bdd6502;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

class QuickDrawPanel extends JPanel {
    BufferedImage image;
    Dimension size = new Dimension();
    int[] rawPixels = null;
    boolean isCached = false;
    int width , height;

    public QuickDrawPanel(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        AttachImage(img);
    }

    public BufferedImage getImage() {
        updateRawPixelsToImage();
        return image;
    }

    public int fastGetWidth() {
        return width;
    }
    public int fastGetHeight() {
        return height;
    }

    public void fastSetRGB(int x, int y, int rgb) {
        isCached = false;
        rawPixels[x + (y*width)] = rgb;
    }

    protected void AttachImage(BufferedImage image) {
        this.image = image;
        size.width = image.getWidth();
        size.height = image.getHeight();
        this.width = size.width;
        this.height = size.height;
        rawPixels = image.getRGB(0, 0, width,height,null,0,width);
        isCached = false;
    }

    protected void paintComponent(Graphics g) {
        updateRawPixelsToImage();
        super.paintComponent(g);
        g.drawImage(image, 0, 0, size.width, size.height, this);
    }

    private void updateRawPixelsToImage() {
        if (isCached) {
            return;
        }
        image.setRGB(0,0,width,height,rawPixels,0,width);
        isCached = true;
    }

    public Dimension getPreferredSize() {
        return size;
    }
}
