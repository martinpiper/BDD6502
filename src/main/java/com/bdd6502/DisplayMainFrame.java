package com.bdd6502;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

public class DisplayMainFrame extends JFrame implements KeyListener , MouseMotionListener {

    boolean pressedUp       = false;
    boolean pressedDown     = false;
    boolean pressedLeft     = false;
    boolean pressedRight    = false;
    boolean pressedFire     = false;

    DisplayBombJack displayBombJack;
    public DisplayMainFrame(DisplayBombJack displayBombJack) {
        this.displayBombJack = displayBombJack;
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        // While removing the opposing directional signal, like a joystick *should*
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            pressedUp = true;
            pressedDown = false;
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            pressedUp = false;
            pressedDown = true;
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            pressedLeft = true;
            pressedRight = false;
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            pressedLeft = false;
            pressedRight = true;
        } else if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
            pressedFire = true;
        } else if (e.getKeyCode() == KeyEvent.VK_P) {
            displayBombJack.setDebugDisplayPixels(!displayBombJack.isDebugDisplayPixels());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            pressedUp = false;
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            pressedDown = false;
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            pressedLeft = false;
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            pressedRight = false;
        } else if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
            pressedFire = false;
        }
    }

    public boolean isPressedUp() {
        return pressedUp;
    }

    public boolean isPressedDown() {
        return pressedDown;
    }

    public boolean isPressedLeft() {
        return pressedLeft;
    }

    public boolean isPressedRight() {
        return pressedRight;
    }

    public boolean isPressedFire() {
        return pressedFire;
    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        displayBombJack.HandlePixelPick();
    }
}
