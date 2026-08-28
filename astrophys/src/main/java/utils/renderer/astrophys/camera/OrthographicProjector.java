package utils.renderer.astrophys.camera;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;

import utils.renderer.astrophys.utils.Particle;
import utils.renderer.astrophys.utils.Vector;
import utils.renderer.astrophys.utils.Particle.Shape;

public class OrthographicProjector extends Camera{
    public OrthographicProjector(Vector pos, Vector normal){
        super(pos, normal);
    }

    public OrthographicProjector(Vector pos){
        this(pos, pos.normalize().scale(-1));
    }

    public void zoom(double ox, double oy, double ticks) {
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        Vector shiftVec = right.scale(ox).add(up.scale(oy)).scaleInPlace(Math.abs(ticks));

        Vector origin = pos.add(shiftVec.add(normal).scale(zoom));
        zoom = Math.min(Math.max(zoom*Math.pow(1.05, ticks), 1e-8), 1e8);
        pos = origin.subtract((normal.add(shiftVec).scale(zoom)));
    }

    public void scale(double ticks) { // scale the image by given mouse-ticks
        scale = Math.min(Math.max(scale+ticks, 1e-8), 1e8);
    }

    public void focus(double ticks){}

    public void shift(double dx, double dy) { // pan by given x,y on-screen/relative to screen
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        Vector shiftVec = right.scale(dx).add(up.scale(dy)).scale(zoom/scale);

        pos = pos.add(shiftVec);
    }

    @Override
    public void jump(double ox, double oy) {
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalizeInPlace();
        up = right.cross(normal).normalize();

        Vector shiftVec = right.scale(ox).addInPlace(up.scale(oy)).scale(zoom/scale);

        pos.addInPlace(shiftVec);
    }

    public void orbit(double dx, double dy) { // rotate by given x,y on-screen/relative to screen
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalizeInPlace();
        
        if(1-Math.pow(normal.dot(up),2) < Math.pow(dy, 2)) {
            dy*=Math.max(0, -Math.signum(normal.dot(up)*dy));
        }

        Vector origin = pos.add(normal.scale(zoom));
        normal = normal.rotateAroundAxis(up, dx).rotateAroundAxis(right, dy).normalize();
        pos = origin.subtract(normal.scale(zoom));
    }

    public void render(ArrayList<Particle> particles, Graphics2D g){
        particles.sort((a, b) -> {
            double da = a.pos.subtract(pos).dot(normal);
            double db = b.pos.subtract(pos).dot(normal);
            return Double.compare(db, da);
        });

        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalizeInPlace();
        up = right.cross(normal).normalizeInPlace();

        for (Particle p : particles) {
            Vector rel = p.pos.subtract(pos);

            double x = rel.dot(right), y = rel.dot(up), r = p.rad;

            x /= zoom;
            y /= zoom;
            r /= zoom;

            if(r<0.5) continue;
            
            g.setColor(p.color);
            if(p.shape==Shape.SPHERE){
                g.fillOval((int) ((x-r)*scale), (int) ((y-r)*scale), (int) ((2*r)*scale), (int) ((2*r)*scale));
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

        double px = pRel.dot(right);
        double py = pRel.dot(up);

        double qx = qRel.dot(right);
        double qy = qRel.dot(up);

        px /= zoom; py /= zoom;
        qx /= zoom; qy /= zoom;

        return new Point[]{
            new Point((int) px, (int) py),
            new Point((int) qx, (int) qy)
        };
    }
}
