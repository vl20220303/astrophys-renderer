package utils.userinterface;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import utils.camera.Camera;

public class Controls implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    private double ZOOM_PER_TICK = 0.5;
    private double SHIFT_PER_TICK = 1;
    private double ROTATE_PER_TICK = 0.001;

    private final Camera camera;
    private final JPanel panel;

    private Point lastMouse;
    private boolean leftDown, rightDown;

    public Controls(Camera camera, JPanel panel) {
        this.camera = camera;
        this.panel = panel;

        // Attach listeners to the panel
        panel.addMouseListener(this);
        panel.addMouseMotionListener(this);
        panel.addMouseWheelListener(this);
        panel.addKeyListener(this);
        panel.setFocusable(true);
    }

    public void setZoomPerTick(double d){ ZOOM_PER_TICK = d; }
    public void setShiftPerTick(double d){ SHIFT_PER_TICK = d; }
    public void setRotatePerTick(double d){ ROTATE_PER_TICK = d; }

    // Mouse events for pan (left), rotate (right), zoom (wheel)
    @Override
    public void mousePressed(MouseEvent e) {
        lastMouse = e.getPoint();
        if (SwingUtilities.isLeftMouseButton(e)) leftDown = true;
        if (SwingUtilities.isRightMouseButton(e)) rightDown = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) leftDown = false;
        if (SwingUtilities.isRightMouseButton(e)) rightDown = false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        int dx = e.getX() - lastMouse.x;
        int dy = e.getY() - lastMouse.y;
        if (leftDown) {
            camera.shift(-dx*SHIFT_PER_TICK, dy*SHIFT_PER_TICK);
        } else if (rightDown) {
            camera.orbit(-dx*ROTATE_PER_TICK, dy*ROTATE_PER_TICK);
        }
        lastMouse = e.getPoint();
        panel.repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (controlDown) {
            camera.jump(e.getPoint().getX() - panel.getWidth()/2, -e.getPoint().getY() + panel.getHeight()/2);
        }
        if (cDown) {
            camera.setFocusColor(e.getPoint().getX() - panel.getWidth()/2, -e.getPoint().getY() + panel.getHeight()/2);
        }
        panel.repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (shiftDown) {
            camera.focus(-e.getPreciseWheelRotation()*ZOOM_PER_TICK);
        } else if (controlDown) {
            camera.scale(e.getPreciseWheelRotation()*ZOOM_PER_TICK);
        } else {
            camera.zoom(e.getPoint().getX() - panel.getWidth()/2, -e.getPoint().getY() + panel.getHeight()/2, -e.getPreciseWheelRotation()*ZOOM_PER_TICK);
        }
        panel.repaint();
    }

    // Unused mouse events
    @Override public void mouseMoved(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    private boolean shiftDown, controlDown, cDown;

    // Keypress events
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_CONTROL:
                controlDown = true;
                break;
            case KeyEvent.VK_SHIFT:
                shiftDown = true;
                break;
            case KeyEvent.VK_C:
                cDown = true;
                camera.enableFocusColor();
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_CONTROL:
                controlDown = false;
                break;
            case KeyEvent.VK_SHIFT:
                shiftDown = false;
                break;
            case KeyEvent.VK_C:
                cDown = false;
                camera.disableFocusColor();
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}