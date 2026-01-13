import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Controls implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    private final Camera camera;
    private final JPanel panel;

    private Point lastMouse;
    private boolean leftDown, rightDown, controlDown = false;

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
            camera.shift(-dx, dy);
        } else if (rightDown) {
            camera.orbit(lastMouse.x, lastMouse.y, -dx, dy);
        }
        lastMouse = e.getPoint();
        panel.repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (controlDown) {
            camera.scale(e.getPreciseWheelRotation());
        } else {
            camera.zoom(e.getPoint().getX() - panel.getWidth() / 2, e.getPoint().getY() - panel.getHeight() / 2, -e.getPreciseWheelRotation());
        }
        panel.repaint();
    }

    // Unused mouse events
    @Override public void mouseMoved(MouseEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    // Keypress events for scaling (ctrl+wheel)
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_CONTROL:
                controlDown = true;
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_CONTROL:
                controlDown = false;
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}