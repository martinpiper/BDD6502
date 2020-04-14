package com.bdd6502;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

class QuickDrawPanel extends JPanel {
    public BufferedImage getImage() {
        return image;
    }

    BufferedImage image;
    Dimension size = new Dimension();

    public QuickDrawPanel(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        AttachImage(img);
    }

    protected void AttachImage(BufferedImage image) {
        this.image = image;
        size.width  = image.getWidth();
        size.height = image.getHeight();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0,  size.width , size.height, this);
    }

    public Dimension getPreferredSize() {
        return size;
    }
}
