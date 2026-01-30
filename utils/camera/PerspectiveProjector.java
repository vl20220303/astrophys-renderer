package utils.camera;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;

import utils.utils.Particle;
import utils.utils.Vector;

public class PerspectiveProjector extends Camera{
    public PerspectiveProjector(Vector pos, Vector normal){
        super(pos, normal);
    }

    public PerspectiveProjector(Vector pos){
        this(pos, pos.scale(-1));
    }

    @Override
    public void zoom(double ox, double oy, double ticks) { super.zoom(ox/100, oy/100, ticks); }

    // @Override
    // public void shift(double dx, double dy) { super.shift(dx/100, dy/100); }

    public void render(ArrayList<Particle> particles, Graphics2D g){
        particles.sort((a, b) -> {
            double da = a.pos.subtract(pos).dot(normal);
            double db = b.pos.subtract(pos).dot(normal);
            return Double.compare(db-((b.rad*b.rad)/db), da-((a.rad*a.rad)/da));
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
                r = Math.min(Math.sqrt(r*r - (r*r*r*r)/(depth*depth))/((depth - (r*r)/depth)), 1e5);
            } else if(depth==0){
                r = 100000;
            } else{
                r = 0;
            }
            x *= 100; y *= 100; r *= 100;
            
            g.setColor(p.color);
            if(p.shape.equals("circle")){
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
                pRel.addInPlace(qRel.subtract(pRel).scaleInPlace(t));
                pDepth = 0;
            } else {
                qRel.addInPlace(pRel.subtract(qRel).scaleInPlace(1 - t));
                qDepth = 0;
            }
        }

        double px = pRel.dot(right); double py = pRel.dot(up);
        double qx = qRel.dot(right); double qy = qRel.dot(up);

        px /= pDepth / 100; py /= pDepth / 100;
        qx /= qDepth / 100; qy /= qDepth / 100;

        return new Point[]{
            new Point((int) px, (int) py),
            new Point((int) qx, (int) qy)
        };
    }
}
