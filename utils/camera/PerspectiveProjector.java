package utils.camera;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import utils.utils.ColorOps;
import utils.utils.Particle;
import utils.utils.Vector;
import utils.utils.Particle.SourceType;

public class PerspectiveProjector extends Camera{
    private static final int CONSTANT_SCALING = 100;

    public PerspectiveProjector(Vector pos, Vector normal){
        super(pos, normal);
    }

    public PerspectiveProjector(Vector pos){
        this(pos, pos.scale(-1));
    }

    public void zoom(double ox, double oy, double ticks) {
        ox/=CONSTANT_SCALING; oy/=CONSTANT_SCALING;

        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        Vector shiftVec = right.scale(ox).add(up.scale(oy)).scaleInPlace(Math.abs(ticks));

        Vector origin = pos.add(shiftVec.scale(zoom)).add(normal.scale(zoom));
        zoom = Math.min(Math.max(zoom*Math.pow(1.05, ticks), 1e-8), 1e8);
        pos = origin.subtract((normal.scale(zoom)).add((shiftVec).scale(zoom)));
    }

    public void scale(double ticks) { // scale the image by given mouse-ticks
        scale = Math.min(Math.max(scale+ticks, 1e-8), 1e8);
    }

    public void focus(double ticks){}

    public void shift(double dx, double dy) { // pan by given x,y on-screen/relative to screen
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        Vector shiftVec = right.scale(dx).addInPlace(up.scale(dy)).scale(zoom/scale/CONSTANT_SCALING);

        pos.addInPlace(shiftVec);
    }

    
    @Override
    public void jump(double dx, double dy) {
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        Vector shiftVec = right.scale(dx).addInPlace(up.scale(dy)).scale(zoom/scale/CONSTANT_SCALING);

        pos.addInPlace(shiftVec);
    }

    public void orbit(double dx, double dy) { // rotate by given x,y on-screen/relative to screen
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
    public void setFocusColor(double dx, double dy) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setFocusColor'");
    }

    public void render(ArrayList<Particle> particles, Graphics2D g){
        particles.sort((a, b) -> {
            double da = a.pos.subtract(pos).dot(normal);
            double db = b.pos.subtract(pos).dot(normal);
            return Double.compare(db * (1-Math.pow(b.rad/db, 2)), da * (1-Math.pow(a.rad/da, 2)));
        });

        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        for (Particle p : particles) {
            Vector rel = p.pos.subtract(pos);
            
            double x = rel.dot(right), y = rel.dot(up), r = p.rad;

            double depth = rel.dot(normal);

            if (depth > 0) {
                x /= (depth);
                y /= (depth);
                r = Math.min(r/Math.sqrt(depth*depth - r*r), 1e8);
            } else if(depth<1e-8){
                r = CONSTANT_SCALING*CONSTANT_SCALING;
            } else{
                r = 0;
            }

            x *= scale * CONSTANT_SCALING;
            y *= scale * CONSTANT_SCALING;
            r *= scale * CONSTANT_SCALING;

            if(r<0.5) continue;

            double d = 2*r;
            
            if(p.shape==SourceType.SPHERE){
                Color realColor = p.color;
                if(useLighting && p.intensity > 0){

                }
                
                g.setColor(realColor);
                g.fillOval((int) (x-r), (int) (y-r), (int) d, (int) d);
            }
        }
    }

    @Override
    protected Point[] projectLineToScreen(Vector p, Vector q) {
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        Vector pRel = p.subtract(pos);
        Vector qRel = q.subtract(pos);
        double pDepth = pRel.dot(normal);
        double qDepth = qRel.dot(normal);

        //line behind plane
        if (pDepth < 0 && qDepth < 0) {
            return null;
        }

        //clip line
        if ((pDepth < 0 || qDepth < 0)) {
            double t = pDepth / (pDepth - qDepth);
            if (pDepth < 0) {
                pRel = pRel.add(qRel.subtract(pRel).scale(t));
                pDepth = 0;
            } else {
                qRel = qRel.add(pRel.subtract(qRel).scale(1 - t));
                qDepth = 0;
            }
        }

        double px = pRel.dot(right); double py = pRel.dot(up);
        double qx = qRel.dot(right); double qy = qRel.dot(up);

        px *= scale * CONSTANT_SCALING / pDepth; py *= scale * CONSTANT_SCALING / pDepth;
        qx *= scale * CONSTANT_SCALING / qDepth; qy *= scale * CONSTANT_SCALING / qDepth;

        return new Point[]{
            new Point((int) px, (int) py),
            new Point((int) qx, (int) qy)
        };
    }
}
