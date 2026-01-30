package utils.camera;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;

import utils.utils.Particle;
import utils.utils.Vector;

public class RayTracer extends Camera{
    private int pixelHeight = 2, pixelWidth = 2;

    public RayTracer(Vector pos, Vector normal){
        super(pos, normal);
    }

    public RayTracer(Vector pos){
        super(pos, pos.normalize().scale(-1));
    }

    @Override
    public void zoom(double ox, double oy, double ticks) { super.zoom(ox, oy, ticks); }

    @Override
    public void render(ArrayList<Particle> particles, Graphics2D g){
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        Vector focus = normal.scale(-1).scale(1/zoom);
        for(int i = (int) (-environment.RESOLUTION * environment.ASPECT_RATIO/2); i<environment.RESOLUTION * environment.ASPECT_RATIO/2; i+=pixelWidth){
            for(int j = (int) (-environment.RESOLUTION/2); j<environment.RESOLUTION/2; j+=pixelHeight){
                Vector pixel = pos.add(right.scale(i+pixelWidth/2).addInPlace(up.scale(j+pixelHeight/2)).scaleInPlace(1/scale));
                g.setColor(getColor(focus, pixel, particles));
                g.fillRect(i, j, pixelWidth, pixelHeight);
            }
        }
    }

    private Color getColor(Vector focus, Vector pixel, ArrayList<Particle> particles){
        Object[] intersection = getIntersection(pixel, pixel.subtract(focus), particles);
        Particle particle = (Particle) intersection[0];
        if(particle == null){
            return environment.BACKGROUND_COLOR;
        } else {
            return particle.color;
        }
    }
    
    private Object[] getIntersection(Vector rayOrigin, Vector rayVec, ArrayList<Particle> particles){
        Particle intersectParticle = null;
        double intersectDist = Double.POSITIVE_INFINITY;
        for(Particle p : particles){
            Vector diff = rayOrigin.subtract(p.pos);

            if(diff.dot(rayVec) < 0) continue;
            double a = rayVec.dot(rayVec);
            double b = 2*diff.dot(rayVec);
            double c = diff.dot(diff) - Math.pow(p.rad, 2);

            if(Math.pow(b, 2) - 4*a*c < 0) continue;
            double dist = (-b - Math.sqrt(Math.pow(b, 2) - 4*a*c))/(2*a);
            if(dist<0) dist+=Math.sqrt(Math.pow(b, 2) - 4*a*c)/a;

            if(dist < 0 || intersectDist < dist) continue;
            intersectParticle = p; intersectDist = dist;
        }
        Vector intersectVec = rayOrigin.add(rayVec.scale(intersectDist));
        return new Object[]{intersectParticle, intersectVec};
    }

    @Override
    protected Point[] projectLineToScreen(Vector p, Vector q) {
        return null;
    }
}