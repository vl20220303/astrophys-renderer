import java.awt.Color;
import java.util.ArrayList;

import javax.swing.JFrame;

import utils.camera.Camera;
import utils.camera.OrthographicProjector;
import utils.camera.PerspectiveProjector;
import utils.camera.RayTracer;
import utils.generator.Disk;
import utils.generator.Generator;
import utils.generator.Ray;
import utils.generator.Sphere;
import utils.runners.Renderer;
import utils.runners.Simulator;
import utils.settings.Constants;
import utils.settings.Settings;
import utils.utils.Particle;
import utils.utils.Vector;

public class Main {
    public static void main(String[] args) {
        Constants constants = new Constants();
        constants.setScale(1e10).setWeight(1e24).setTime(1e3).init();

        Settings environment = new Settings.Builder()
                                        .use720p()
                                        .setTickSpeed(10)
                                        .setFrameTime(1000)
                                        .enableViewIndicator()
                                        .enableZoomIndicator()
                                        .build();

        

        ArrayList<Particle> particles = new ArrayList<>();
        ArrayList<Particle> lightSources = new ArrayList<>();

        // Example: add some particles
        // particles.add(new Particle(new Vector(0,0, 0), 300, 1.98e16, "circle", new Vector(0,0,0), Color.BLACK, true,"none"));
        particles.add(new Particle(new Vector(500, 0, 0), 100, 3.29e2, "circle", new Vector(5,0,0), Color.RED, true, "none"));
        particles.add(new Particle(new Vector(0, 900, 0), 100, 3.29e1, "circle", new Vector(0,0,0), Color.BLUE, true, "none"));
        // particles.add(new Particle(new Vector(0, 120, 450), 80, 3.29e3, "circle", new Vector(0,0,0), Color.BLUE, true, "none"));
        // particles.add(new Particle(new Vector(300, 0, 500), 120, 3.29e-5, "circle", new Vector(10,0,0), Color.GREEN, true, "none"));
        // particles.add(new Particle(new Vector(380, 0, 500), 40, 3.29e-2, "circle", new Vector(9,1,0), Color.GRAY, true, "none"));
        
        particles.add(new Particle(new Vector(0,0, 0), 300, 1.98e16, "circle", new Vector(0,0,0), Color.BLACK, true,"none"));
        // particles.add(new Particle(new Vector(-300,0, 0), 300, 5.98e16, "circle", new Vector(0,-10,0), Color.BLACK, false,"none"));

        Generator eq = new Sphere(new Vector(0, 0, 0), 500, 2000, (Double r) -> Math.pow((r-499)/1500, 0));
        eq.setResolution(1);
        eq.setup();
        ArrayList<Particle> cloud = eq.generate(1500, new Particle(Vector.ORIGIN, 20, 2e12, "circle", new Vector(0,0,0), Color.GREEN, true, "none"),null,Vector.Z_AXIS.add(Vector.Y_AXIS), (Double d) -> 8*Math.pow(d, -0.5));

        Generator eq2 = new Disk(new Vector(0,0,0), Vector.Y_AXIS, 500, 2000, (Double r) -> Math.pow((r-499)/1500, 0));
        eq2.setResolution(1);
        eq2.setup();
        ArrayList<Particle> cloud2 = eq2.generate(1500, new Particle(Vector.ORIGIN, 20, 2e12, "circle", new Vector(0,0,0), Color.ORANGE, false, "none"),null,Vector.Z_AXIS.add(Vector.Y_AXIS), (Double d) -> 8*Math.pow(d, -0.5));

        Generator eq3 = new Ray(new Vector(0,0,0), Vector.X_AXIS, 500, 2000, (Double r) -> Math.pow((r-499)/1500, 0), true);
        eq3.setResolution(1);
        eq3.setup();
        ArrayList<Particle> cloud3 = eq3.generate(1500, new Particle(Vector.ORIGIN, 20, 2e12, "circle", new Vector(0,0,0), Color.BLUE, false, "none"),null,Vector.Z_AXIS.add(Vector.Y_AXIS), (Double d) -> 8*Math.pow(d, -0.5));
        
        // particles.addAll(cloud);
        // particles.addAll(cloud2);
        // particles.addAll(cloud3);

        // System.out.println(cloud.size());
        // System.out.println(cloud2.size());
        // System.out.println(cloud3.size());

        Simulator simulator = new Simulator(particles, constants, environment);
        simulator.disableGravity();

        Thread simulatorThread = new Thread(simulator);
        simulatorThread.start();

        Camera camera = new PerspectiveProjector(new Vector(0, 0, 1000));
        Renderer renderer = new Renderer(camera, simulator, environment);
        JFrame frame = new JFrame("Astrophys Renderer");
        renderer.init(frame);
        renderer.run();

        // Camera camera2 = new OrthographicProjector(new Vector(0, 0, 500));
        // Renderer renderer2 = new Renderer(camera2, simulator, environment);
        // JFrame frame2 = new JFrame("Astrophys Renderer - Alternate Angle");
        // renderer2.init(frame2);
        // renderer2.run();

        Camera camera3 = new RayTracer(new Vector(0, 0, -100));
        Renderer renderer3 = new Renderer(camera3, simulator, environment);
        JFrame frame3 = new JFrame("Astrophys Renderer - Raytraced");
        renderer3.init(frame3);
        renderer3.run();
    }
}