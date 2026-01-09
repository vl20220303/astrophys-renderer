import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class Renderer extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {
    public Camera camera;
    public ArrayList<Particle> particles;
    public ArrayList<Particle> lightSources;

    private Point lastMouse;
    private boolean leftDown = false, rightDown = false;

    public Renderer() {
        //camera = new Camera(new Vector(0, -30, 40), new Vector(0, 0, -1), "perspective");
        this.camera = new Camera(new Vector(0, 0, 1000), "perspective");
        //camera.zoom = 0.25;
        particles = new ArrayList<>();
        lightSources = new ArrayList<>();


        // Example: add some particles
        particles.add(new Particle(new Vector(0,0, 0), 300, 1.98e5, "circle", new Vector(0,0,0), Color.BLACK, true, "collide"));
        particles.add(new Particle(new Vector(0, 400, 0), 100, 3.29e-2, "circle", new Vector(0,0,0), Color.RED, true, "none"));
        // particles.add(new Particle(new Vector(0, 120, 450), 80, 3.29e-2, "circle", new Vector(9,1,0), Color.BLUE, false, "none"));
        // particles.add(new Particle(new Vector(300, 0, 500), 120, 3.29e-2, "circle", new Vector(10,0,0), Color.GREEN, false, "none"));
        // particles.add(new Particle(new Vector(380, 0, 500), 40, 3.29e-2, "circle", new Vector(9,1,0), Color.GRAY, false, "none"));

        setPreferredSize(new Dimension((int) (Environment.RESOLUTION* Environment.ASPECT_RATIO), Environment.RESOLUTION));
        setBackground(Environment.BACKGROUND_COLOR);

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);

        Timer timer = new Timer(Environment.TICK_SPEED, e -> {
            update();
            repaint();
        });
        timer.start();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Astrophys Renderer");
        Renderer renderer = new Renderer();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(renderer);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void update() {
        if(Environment.GRAVITY_ENABLED){
            for (int i = 0; i < particles.size(); i++) {
                for (int j = i + 1; j < particles.size(); j++) {
                    particles.get(i).gravitate(particles.get(j));
                }
            }
        }
        for (int i = 0; i < particles.size(); i++) {
            for (int j = i + 1; j < particles.size(); j++) {
                particles.get(i).collide(particles.get(j));
            }
        }
        for (Particle p : particles) {
            p.update();
            p.clearAccel();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.translate(getWidth() / 2, getHeight() / 2);
        g2d.scale(1, -1);

        if(Environment.ANTIALIASING_ENABLED) g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if(Environment.DRAW_ORIGIN_GRID) camera.drawGrid(g2d);

        if(Environment.DRAW_ORIGIN) camera.drawOrigin(g2d);

        if(Environment.DRAW_MINI_ORIGIN) camera.drawMiniOrigin(g2d);
        
        camera.render(particles, g2d);

        g2d.dispose();
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
        repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        camera.zoom(e.getPoint().getX() - getWidth()/2, e.getPoint().getY() - getHeight()/2, -e.getPreciseWheelRotation());
        repaint();
    }

    // Unused mouse events
    @Override public void mouseMoved(MouseEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}