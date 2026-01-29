package utils.camera;
import java.awt.Graphics2D;
import java.util.ArrayList;

import utils.utils.Particle;
import utils.utils.Vector;

public class Projector extends Camera{
    public Projector(Vector pos, Vector normal, boolean usePerspective){
        super(pos, normal, usePerspective);
    }

    public Projector(Vector pos, boolean usePerspective){
        super(pos, usePerspective);
    }

    public void render(ArrayList<Particle> particles, Graphics2D g){
        particles.sort((a, b) -> {
            double da = a.pos.subtract(pos).dot(normal);
            double db = b.pos.subtract(pos).dot(normal);
            return usePerspective ? Double.compare(db-((b.rad*b.rad)/db), da-((a.rad*a.rad)/da)) : Double.compare(db, da);
        });

        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        for (Particle p : particles) {
            Vector rel = p.pos.subtract(pos);
            
            double x = rel.dot(right), y = rel.dot(up), r = p.rad;

            double depth = rel.dot(normal);

            if (usePerspective) {
                if (depth > 0) {
                    x /= (depth);
                    y /= (depth);
                    r = Math.min(Math.sqrt(r*r - (r*r*r*r)/(depth*depth))/((depth - (r*r)/depth)), 100000);
                } else if(depth==0){
                    r = 100000;
                } else{
                    r = 0;
                }
                x *= 100; y *= 100; r *= 100;
            } else {
                if(r/zoom > environment.RESOLUTION * environment.ASPECT_RATIO) continue;
                x /= zoom;
                y /= zoom;
                r /= zoom;
            }
            
            g.setColor(p.luminosity);
            if(p.shape.equals("circle")){
                g.fillOval((int) ((x-r)*scale), (int) ((y-r)*scale), (int) ((2*r)*scale), (int) ((2*r)*scale));
            }
        }
    }
}
