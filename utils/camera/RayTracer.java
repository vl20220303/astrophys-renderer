package utils.camera;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

import utils.utils.BigColor;
import utils.utils.ColorLayer;
import utils.utils.ColorOps;
import utils.utils.Particle;
import utils.utils.Vector;

public class RayTracer extends Camera{
    private int pixelHeight = 1, pixelWidth = 1;
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
        focalLength = Math.min(Math.max(focalLength*(1-ticks), 1e-8), 1e8);
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
    public void setFocusColor(double ox, double oy){
        focusColorX = ox; focusColorY = oy;
    }

    @Override
    public void render(ArrayList<Particle> particles, Graphics2D g){
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        Vector focus = pos.add(normal.scale(-focalLength));
        for(int i = (int) (-environment.RESOLUTION * environment.ASPECT_RATIO/2); i<environment.RESOLUTION * environment.ASPECT_RATIO/2; i+=pixelWidth){
            for(int j = (int) (-environment.RESOLUTION/2); j<environment.RESOLUTION/2; j+=pixelHeight){
                Vector pixel = pos.add(right.scale(i+pixelWidth/2).addInPlace(up.scale(j+pixelHeight/2)).scaleInPlace(1/scale));
                BigColor origColor = getColor(focus, pixel, particles);

                Color dispColor;

                if(useFocusColor){
                    if(Math.abs(i-focusColorX)<pixelWidth && Math.abs(j-focusColorY)<pixelHeight){ 
                        focusColor = origColor; 
                    }
                    dispColor = origColor.normalizeTo(focusColor);
                } else{
                    dispColor = origColor.normalize();
                }

                g.setColor(dispColor);
                g.fillRect(i, j, pixelWidth, pixelHeight);
            }
        }
    }

    private BigColor getColor(Vector focus, Vector pixel, ArrayList<Particle> particles){
        Vector origin = pixel;
        Vector ray = pixel.subtract(focus);

        int reflections = 0, maxReflections = !useLighting ? 1 : 10;
        Deque<ColorLayer> layers = new ArrayDeque<ColorLayer>();
        while(reflections<maxReflections){
            Intersection intersection = getIntersection(origin, ray, particles);
            Particle particle = intersection.intersectParticle;
            Vector newOrigin = intersection.intersectPoint;
            double dist = intersection.intersectDist;

            if(particle==null){ break; }

            if(!useLighting){
                return new BigColor(particle.color);
            }

            Vector incoming = ray.scale(-1).normalize();
            Vector normal = particle.pos.subtract(newOrigin).normalize();
            double cosine = incoming.dot(normal);
            ray = incoming.add(normal.scale(2*cosine)).normalize();
            origin = newOrigin;

            double diffuseLoss = (Math.abs(cosine) * particle.diffusion) + (1 - particle.diffusion);
            double dropoffLoss = 1/(Math.pow(dist, 2)+1e-10);
            double absorptionLoss = particle.reflectivity;
            double intensityloss = diffuseLoss * dropoffLoss * absorptionLoss;

            if(particle.intensity == 0){
                layers.push(new ColorLayer(particle.color, intensityloss));
                layers.push(new ColorLayer(environment.BACKGROUND_COLOR, 1, environment.AMBIENT_INTENSITY));
            } else{
                layers.push(new ColorLayer(particle.color, intensityloss, particle.intensity));
                layers.push(new ColorLayer(particle.color, intensityloss));
                layers.push(new ColorLayer(environment.BACKGROUND_COLOR, 1, environment.AMBIENT_INTENSITY));
            }

            reflections++;
        }

        BigColor color = new BigColor(environment.BACKGROUND_COLOR);

        while(layers.size()>0){
            ColorLayer layer = layers.pop();
            if(layer.operation == ColorLayer.opType.ADD){
                color.add(layer.color, layer.intensity);
                color.scale(layer.dropoff);
            } else{
                color.multiply(layer.color, layer.dropoff);
            }
        }
        return color;
    }

    private class Intersection{
        Particle intersectParticle;
        Vector   intersectPoint;
        double   intersectDist;
        Intersection(Particle p, Vector v, double d){
            this.intersectParticle = p;
            this.intersectPoint = v;
            this.intersectDist = d;
        }
    }
    
    private Intersection getIntersection(Vector rayOrigin, Vector rayVec, ArrayList<Particle> particles){
        rayVec.normalizeInPlace();
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
            if(dist < 1e-9) dist = -dist - b/a;

            if(dist < 1e-9 || dist > intersectDist) continue;
            intersectParticle = p; intersectDist = dist;
        }
        Vector intersectVec = rayOrigin.add(rayVec.scale(intersectDist));
        return new Intersection(intersectParticle, intersectVec, intersectDist);
    }

    @Override
    protected Point[] projectLineToScreen(Vector p, Vector q) {
        
        throw new Error("Unimplemented method projectLineToScreen");
        // return null;
    }

}