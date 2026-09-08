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
import utils.settings.Environment;
import utils.utils.Particle;
import utils.utils.Vector;
import utils.utils.Particle.Behavior;
import utils.utils.Particle.SourceType;

public class Main {
    public static void main(String[] args) {
        Constants constants = new Constants();
        constants.setScale(1e10).setWeight(1e24).setTime(1e3).init();

        Environment environment = new Environment.Builder()
                                        .use720p()
                                        .setTickSpeed(10)
                                        .use30FPS()
                                        .enableViewIndicator()
                                        .enableZoomIndicator()
                                        .enableFocusIndicator()
                                        // .setBackgroundColor(new Color(250,100,250))
                                        .setBackgroundColor(Color.BLACK)
                                        .build();

        
        ArrayList<Particle> particles = new ArrayList<>();

        Particle sun = new Particle()
                            .setAttributes(300, 1.98e16, SourceType.SPHERE)
                            .setColor(Color.ORANGE, 1e100)
                            .fixed();
        Particle planet = new Particle()
                                .setPos(new Vector(990, 0, 0))
                                .setAttributes(100, 3.29e2, SourceType.SPHERE)
                                .setColor(Color.RED, 0)
                                .fixed();

        // Example: add some particles
        // particles.add(new Particle(new Vector(0,0, 0), 300, 1.98e16, "circle", new Vector(0,0,0), Color.BLACK, true,"none"));
        // particles.add(new Particle(new Vector(500, 0, 0), 100, 3.29e2, "circle", new Vector(5,0,0), Color.RED, true, "none"));
        // particles.add(new Particle(new Vector(0, 900, 0), 100, 3.29e1, "circle", new Vector(0,0,0), Color.BLUE, true, "none"));
        // particles.add(new Particle(new Vector(0, 120, 450), 80, 3.29e3, "circle", new Vector(0,0,0), Color.BLUE, true, "none"));
        // particles.add(new Particle(new Vector(300, 0, 500), 120, 3.29e-5, "circle", new Vector(10,0,0), Color.GREEN, true, "none"));
        // particles.add(new Particle(new Vector(380, 0, 500), 40, 3.29e-2, "circle", new Vector(9,1,0), Color.GRAY, true, "none"));
        
        // particles.add(new Particle(new Vector(0,0, 0), 300, 1.98e16, "circle", new Vector(0,0,0), Color.BLACK, true,"none"));
        // particles.add(new Particle(new Vector(-300,0, 0), 300, 5.98e16, "circle", new Vector(0,-10,0), Color.BLACK, false,"none"));

        Sphere eq = new Sphere(new Vector(0, 0, 0), 500, 800, (Double r) -> Math.pow(r-499, -1)+1e-4);
        eq.setResolution(1);
        eq.setup();
        ArrayList<Particle> cloud = 
            eq.withRotationalVel(Vector.Z_AXIS.add(Vector.Y_AXIS), (Double r) -> 8*Math.pow(r, -0.5))
            .generate(3,
                    new Particle()
                        .setAttributes(15, 2e12, SourceType.SPHERE)
                        .setColor(Color.GREEN, 0)
                        .fixed()
                        .setBehavior(Behavior.NONE));

        Disk eq2 = new Disk(new Vector(0,0,0), Vector.Y_AXIS, 500, 800, 10, (Double r) -> Math.pow(r-499, -1)+1e-3, (Double h) -> 1d);
        eq2.setResolution(0.1, 1);
        eq2.setup();
        ArrayList<Particle> cloud2 = 
            eq2.withRotationalVel(Vector.Z_AXIS.add(Vector.Y_AXIS), (Double r) -> 8*Math.pow(r, -0.5))
            .generate(2500,
                new Particle()
                    .setAttributes(10, 2e12, SourceType.SPHERE)
                    .setColor(Color.ORANGE, 0)
                    .fixed()
                    .setBehavior(Behavior.NONE));

        Ray eq3 = new Ray(new Vector(0,0,0), Vector.Y_AXIS.add(Vector.X_AXIS), 300, 1500, (Double r) -> Math.pow(r-299, -1)+1e-5, true);
        eq3.setResolution(0.1);
        eq3.setup();
        ArrayList<Particle> cloud3 = 
            eq3.withRotationalVel((Vector diff) -> diff.scale(0.01))
            .generate(2500,
                new Particle()
                    .setAttributes(10, 2e14, SourceType.SPHERE)
                    .setColor(Color.BLUE, 0)
                    .fixed()
                    .setBehavior(Behavior.NONE));
        
        particles.add(sun);
        particles.add(planet);
        // particles.addAll(cloud);
        // particles.addAll(cloud2);
        // particles.addAll(cloud3);

        Simulator simulator = new Simulator(particles, constants, environment);

        Thread simulatorThread = new Thread(simulator);
        simulatorThread.start();

        Camera camera = new PerspectiveProjector(new Vector(0, 0, 1000));
        Renderer renderer = new Renderer(camera, simulator, environment);
        JFrame frame = new JFrame("Astrophys Renderer");
        renderer.init(frame);
        renderer.run();

        // Camera camera2 = new OrthographicProjector(new Vector(0, 0, 1000));
        // Renderer renderer2 = new Renderer(camera2, simulator, environment);
        // JFrame frame2 = new JFrame("Astrophys Renderer - Alternate Angle");
        // renderer2.init(frame2);
        // renderer2.run();

        Camera camera3 = new RayTracer(new Vector(0, 0, 1000));
        Renderer renderer3 = new Renderer(camera3, simulator, environment);
        JFrame frame3 = new JFrame("Astrophys Renderer - Raytraced");
        renderer3.init(frame3);
        renderer3.run();

        Camera camera4 = new RayTracer(new Vector(0, 0, 1000));
        camera4.useLighting();
        Renderer renderer4 = new Renderer(camera4, simulator, environment);
        JFrame frame4 = new JFrame("Astrophys Renderer - Raytraced with Lighting");
        renderer4.init(frame4);
        renderer4.run();
    }
}