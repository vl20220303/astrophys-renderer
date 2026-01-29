package utils.runners;
import java.util.ArrayList;

import utils.settings.Constants;
import utils.settings.Settings;
import utils.utils.Particle;

public class Simulator implements Runnable {
    private final ArrayList<Particle> particles;
    private final Constants constants;
    private final Object lock = new Object();
    private boolean running = true;
    private Settings environment;

    private boolean GRAVITY_ENABLED = true;

    public Simulator(ArrayList<Particle> particles, Constants constants) {
        this.particles = particles;
        this.constants = constants;
    }

    public void setEnvironment(Settings environment){ this.environment = environment; }

    public void stop() {
        running = false;
    }

    public ArrayList<Particle> getParticles() {
        synchronized (lock) {
            return new ArrayList<>(particles); // Return a copy to avoid concurrent modification
        }
    }

    public void enableGravity(){ GRAVITY_ENABLED = true; }
    public void disableGravity(){ GRAVITY_ENABLED = false; }

    @Override
    public void run() {
        while (running) {
            synchronized (lock) {
                updateParticles();
            }
            try {
                Thread.sleep(environment.TICK_SPEED); // Control simulation speed
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
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