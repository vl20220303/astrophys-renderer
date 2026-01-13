import java.awt.Color;
import java.util.ArrayList;

import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) {
        Constants constants = new Constants();
        constants.init();

        Camera camera = new Camera(new Vector(0, 0, 1000), true);
        // camera = new Camera(new Vector(0, 0, 500), new Vector(0, 0, -1), false);

        ArrayList<Particle> particles = new ArrayList<>();
        ArrayList<Particle> lightSources = new ArrayList<>();

        // Example: add some particles
        particles.add(new Particle(new Vector(0,0, 0), 300, 1.98e10, "circle", new Vector(0,0,0), Color.BLACK, true, "collide"));
        particles.add(new Particle(new Vector(400, 0, 0), 100, 3.29e5, "circle", new Vector(0,0,0), Color.RED, true, "collide"));
        particles.add(new Particle(new Vector(0, 900, 0), 100, 3.29e1, "circle", new Vector(0,0,0), Color.BLUE, true, "collide"));
        // particles.add(new Particle(new Vector(0, 120, 450), 80, 3.29e10, "circle", new Vector(0,0,0), Color.BLUE, false, "none"));
        // particles.add(new Particle(new Vector(300, 0, 500), 120, 3.29e-2, "circle", new Vector(10,0,0), Color.GREEN, false, "none"));
        // particles.add(new Particle(new Vector(380, 0, 500), 40, 3.29e-2, "circle", new Vector(9,1,0), Color.GRAY, false, "none"));

        Simulator simulator = new Simulator(particles, constants);

        Renderer renderer = new Renderer(camera, simulator);

        JFrame frame = new JFrame("Astrophys Renderer");
        renderer.init(frame);
        renderer.run();
    }
}