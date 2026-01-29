package utils.runners;
import javax.swing.*;

import utils.camera.Camera;
import utils.settings.Settings;
import utils.utils.Particle;
import utils.userinterface.Display;
import utils.userinterface.Controls;

import java.awt.*;
import java.util.ArrayList;

public class Renderer extends JPanel {
    private final Camera camera;
    private final Display display;
    private final Simulator simulator;
    private final Settings environment;

    public Renderer(Camera camera, Simulator simulator, Settings environment) {
        this(camera, simulator, new Display(camera), environment);
    }

    public Renderer(Camera camera, Simulator simulator, Display display, Settings environment) {
        this.camera = camera;
        this.display = display;
        this.simulator = simulator;
        this.environment = environment;
    }

    public void init(JFrame frame){
        setPreferredSize(new Dimension((int) (environment.RESOLUTION * environment.ASPECT_RATIO), environment.RESOLUTION));
        setBackground(environment.BACKGROUND_COLOR);

        simulator.setEnvironment(environment);
        camera.setEnvironment(environment);
        display.setEnvironment(environment);

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

        Timer timer = new Timer(environment.TICK_SPEED, e -> repaint());
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.translate(getWidth() / 2, getHeight() / 2);
        g2d.scale(1, -1);

        // g2d.scale(camera.scale, camera.scale);

        if (environment.ANTIALIASING_ENABLED) g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (environment.DRAW_ORIGIN_GRID) camera.drawGrid(g2d);

        if (environment.DRAW_ORIGIN) camera.drawOrigin(g2d);

        ArrayList<Particle> particles = simulator.getParticles();
        camera.render(particles, g2d);

        // g2d.scale(1/camera.scale, 1/camera.scale);
        
        display.drawZoomIndicator(g2d);
        display.drawMiniOrigin(g2d);

        g2d.dispose();
    }
}