package utils.camera;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;

import utils.utils.Particle;
import utils.utils.Vector;

public class RayTracer extends Camera{
    private int pixelHeight = 3, pixelWidth = 2;
    public double focalLength;

    public RayTracer(Vector pos, Vector normal){
        super(pos, normal);
        focalLength = normal.abs()*2;
    }

    public RayTracer(Vector pos){
        this(pos, pos.scale(-1));
    }

    @Override
    public void zoom(double ox, double oy, double ticks) {
        Vector origin = pos.add(normal.scale(zoom));
        zoom = Math.min(Math.max(zoom*Math.pow(1.05, ticks), 1e-8), 1e8);
        pos = origin.subtract(normal.scale(zoom));
    }

    @Override
    public void scale(double ticks) {
        scale = Math.min(Math.max(scale+ticks, 1e-8), 1e8);
    }

    @Override
    public void focus(double ticks){
        focalLength = Math.min(Math.max(focalLength*(1+ticks), 1e-8), 1e8);
    }

    @Override
    public void shift(double dx, double dy) {
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        Vector shiftVec = right.scale(dx).addInPlace(up.scale(dy));

        pos = pos.add(shiftVec);
    }
    
    @Override
    public void jump(double dx, double dy) {
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        Vector shiftVec = right.scale(dx).addInPlace(up.scale(dy));

        pos.addInPlace(shiftVec);
    }

    @Override
    public void orbit(double dx, double dy) {
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        
        if(1-Math.pow(normal.dot(up),2) < Math.pow(dy, 2)) {
            dy*=Math.max(0, -Math.signum(normal.dot(up)*dy));
        }

        Vector origin = pos.add(normal.scale(zoom));
        normal = normal.rotateAroundAxis(up, dx).rotateAroundAxis(right, dy).normalize();
        pos = origin.subtract(normal.scale(zoom));
    }

    @Override
    public void render(ArrayList<Particle> particles, Graphics2D g){
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        

        Vector focus = normal.scale(-focalLength);
        for(int i = (int) (-environment.RESOLUTION * environment.ASPECT_RATIO/2); i<environment.RESOLUTION * environment.ASPECT_RATIO/2; i+=pixelWidth){
            for(int j = (int) (-environment.RESOLUTION/2); j<environment.RESOLUTION/2; j+=pixelHeight){
                Vector pixel = pos.add(right.scale(i+pixelWidth/2).addInPlace(up.scale(j+pixelHeight/2)).scaleInPlace(1/scale));
                g.setColor(getColor(focus, pixel, particles));
                g.fillRect(i, j, pixelWidth, pixelHeight);
            }
        }
    }

    private Color getColor(Vector focus, Vector pixel, ArrayList<Particle> particles){
        Color color = environment.BACKGROUND_COLOR;
        int reflections = 2;

        Vector origin = pixel;
        Vector ray = pixel.subtract(focus);
        for(int i = 0; i<reflections; i++){
            Object[] intersection = getIntersection(origin, ray, particles);
            Particle particle = (Particle) intersection[0];
            Vector newOrigin = (Vector) intersection[1];

            if(particle==null){ break; }

            color = particle.color;

            Vector incoming1 = ray.scale(-1).normalize();
            Vector incoming2 = newOrigin.subtract(origin).scale(-1).normalize();
            Vector normal = particle.pos.subtract(newOrigin).normalize();
            ray = incoming1.add(normal.scale(2*incoming1.dot(normal))).normalize();
            origin = newOrigin;
        }
        return color;
    }
    
    private Object[] getIntersection(Vector rayOrigin, Vector rayVec, ArrayList<Particle> particles){
        Particle intersectParticle = null;
        double intersectDist = Double.POSITIVE_INFINITY;
        for(Particle p : particles){
            Vector diff = rayOrigin.subtract(p.pos);

            double a = rayVec.dot(rayVec);
            double b = 2*diff.dot(rayVec);
            double c = diff.dot(diff) - Math.pow(p.rad, 2);

            double discriminant = Math.pow(b, 2) - 4*a*c;

            if(discriminant < 0) continue;
            double dist = (-b - Math.sqrt(discriminant))/(2*a);
            if(dist<0) dist = -dist - b/a;

            if(dist < 1e-9 || dist > intersectDist) continue;
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