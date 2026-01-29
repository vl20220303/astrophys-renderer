package utils.camera;
import java.awt.Graphics2D;
import java.util.ArrayList;

import utils.utils.Particle;
import utils.utils.Vector;

public class RayTracer extends Camera{
    private Vector focus;

    public RayTracer(Vector pos, Vector normal, boolean usePerspective){
        super(pos, normal, usePerspective);
    }

    public RayTracer(Vector pos, boolean usePerspective){
        super(pos, usePerspective);
    }

    @Override
    public void render(ArrayList<Particle> particles, Graphics2D g){
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();
        for(int i = 0; i<environment.RESOLUTION * environment.ASPECT_RATIO; i++){
            for(int j = 0; j<environment.RESOLUTION; j++){

            }
        }
    }

    private Color getColor(Vector pixel, ArrayList<Particle> particles){

    }
    
    private Object[] getIntersection(Vector rayOrigin, Vector rayVec, ArrayList<Particle> particles){
        Particle intersectParticle = null;
        double intersectDist = Double.POSITIVE_INFINITY;
        for(Particle p : particles){
            Vector diff = p.pos.subtract(rayOrigin);

            if(diff.dot(rayVec) < 0) continue;
            double a = rayVec.dot(rayVec);
            double b = 2*diff.dot(rayVec);
            double c = diff.dot(diff) - Math.pow(p.rad, 2);

            if(Math.pow(b, 2) - 4*a*c < 0) continue;
            double dist = (-b - Math.sqrt(Math.pow(b, 2) - 4*a*c))/(2*a);
            if(dist<0) dist+=Math.sqrt(Math.pow(b, 2) - 4*a*c)/a;

            if(intersectDist < dist) continue;
            intersectParticle = p; intersectDist = dist;
        }
        Vector intersectVec = rayOrigin.add(rayVec.scale(intersectDist));
        return new Object[]{intersectParticle, intersectVec};
    }
}