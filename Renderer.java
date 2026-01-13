import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class Renderer extends JPanel {
    private final Camera camera;
    private final Display display;
    private final Simulator simulator;

    public Renderer(Camera camera, Simulator simulator) {
        this.camera = camera;
        this.display = new Display(camera);
        this.simulator = simulator;
    }

    public Renderer(Camera camera, Simulator simulator, Display display) {
        this.camera = camera;
        this.display = display;
        this.simulator = simulator;
    }

    public void init(JFrame frame){
        setPreferredSize(new Dimension((int) (Environment.RESOLUTION * Environment.ASPECT_RATIO), Environment.RESOLUTION));
        setBackground(Environment.BACKGROUND_COLOR);

        new Controls(camera, this);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void run(){
        Thread simulatorThread = new Thread(simulator);
        simulatorThread.start();

        Timer timer = new Timer(Environment.TICK_SPEED, e -> repaint());
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.translate(getWidth() / 2, getHeight() / 2);
        g2d.scale(1, -1);

        g2d.scale(camera.scale, camera.scale);

        if (Environment.ANTIALIASING_ENABLED) g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (Environment.DRAW_ORIGIN_GRID) camera.drawGrid(g2d);

        if (Environment.DRAW_ORIGIN) camera.drawOrigin(g2d);

        ArrayList<Particle> particles = simulator.getParticles();
        camera.render(particles, g2d);

        g2d.scale(1/camera.scale, 1/camera.scale);
        
        display.drawZoomIndicator(g2d);
        display.drawMiniOrigin(g2d);

        g2d.dispose();
    }
}