package utils.renderer.astrophys.runners;
import java.util.List;

import utils.renderer.astrophys.settings.Constants;
import utils.renderer.astrophys.settings.Environment;
import utils.renderer.astrophys.utils.Particle;

import java.util.ArrayList;
import java.util.Collections;

public class Simulator implements Runnable {
    private final ArrayList<Particle> particles;
    private final Constants constants;
    private final Environment environment;
    private final Object lock = new Object();
    private boolean running = true;

    private boolean GRAVITY_ENABLED = true;

    public Simulator(ArrayList<Particle> particles, Constants constants, Environment environment) {
        this.particles = particles;
        this.constants = constants;
        this.environment = environment;
    }

    public void stop() {
        running = false;
    }

    public ArrayList<Particle> getParticles() {
        synchronized (lock) {
            return new ArrayList<>(particles); // Return a copy to avoid concurrent modification
            // return particles;               // Unsafe, but slightly faster (200ms avg diff @ n=3000)
        }
    }

    public void enableGravity(){ GRAVITY_ENABLED = true; }
    public void disableGravity(){ GRAVITY_ENABLED = false; }

    @Override
    public void run() {
        long then = System.nanoTime();
        long now = System.nanoTime();
        while (running) {
            long cycleStart = System.nanoTime();

            then = System.nanoTime();
            synchronized (lock) {
                updateParticles();
            }
            now = System.nanoTime();
            int elapsed = (int) ((now-then) / 1e6);
            int step = Math.max(environment.TICK_SPEED - elapsed, 0);
            try {
                Thread.sleep(step); // Control simulation speed
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            int cycle = (int) ((System.nanoTime() - cycleStart) / 1e6);
            System.out.printf("%s SIMULATOR %s| update: %d, wait: %d, cycle: %d \n", "\u001B[31m", "\u001B[0m", elapsed, step, cycle);
        }
    }

    private void updateParticles() {
        if (GRAVITY_ENABLED) {
            for (int i = 0; i < particles.size(); i++) {
                for (int j = i + 1; j < particles.size(); j++) {
                    particles.get(i).gravitate(particles.get(j), constants);
                }
            }
        }
        for (int i = 0; i < particles.size(); i++) {
            for (int j = i + 1; j < particles.size(); j++) {
                particles.get(i).collide(particles.get(j), constants);
            }
        }
        for (Particle p : particles) {
            p.update();
            p.clearAccel();
        }
    }
}