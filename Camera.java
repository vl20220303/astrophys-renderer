import java.util.ArrayList;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

public class Camera {
    public static final double ZOOM_PER_TICK = 0.5;
    public static final double SHIFT_PER_TICK = 1;
    public static final double ROTATE_PER_TICK = 0.001;
    
    public static final double PERSPECTIVE_MIN_DEPTH = 0.00001;

    public Vector normal, pos;
    public String view; //perspective, orthagonal
    public double zoom;
    public double rotation;

    public Camera(Vector pos, Vector normal, String view){
        this.pos = pos; this.normal = normal.normalize(); this.view = view;
        zoom = 1;
    }

    public Camera(Vector pos, String view){
        this.pos = pos; this.normal = pos.normalize().scale(-1); this.view = view;
        zoom = pos.subtract(normal).abs();
    }

    public void zoom(double ox, double oy, double ticks) { // zoom in/out by given mouse-ticks
        ticks*=ZOOM_PER_TICK;
        ox*=SHIFT_PER_TICK * 0.01; oy*=SHIFT_PER_TICK * 0.01;

        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        Vector shiftVec = right.scale(-ticks*ox).add(up.scale(-ticks*oy));

        Vector origin = pos.add(shiftVec.scale(zoom)).add(normal.scale(zoom));
        zoom = Math.max(zoom*Math.pow(1.05, ticks), 0.00000001);
        pos = origin.subtract((normal.scale(zoom)).add((shiftVec).scale(zoom)));
    }

    public void shift(double x, double y) { // pan by given x,y on-screen/relative to screen
        x*=SHIFT_PER_TICK; y*=SHIFT_PER_TICK;
        x*=zoom; y*=zoom;
        if(view.equals("perspective")){
            x/=100; y/=100;
        }

        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        Vector shiftVec = right.scale(x).add(up.scale(y));

        pos = pos.add(shiftVec);
    }

    public void orbit(double ox, double oy, double dx, double dy) { // rotate by given x,y on-screen/relative to screen
        dx*=ROTATE_PER_TICK; dy*=ROTATE_PER_TICK; ox*=zoom; oy*=zoom;
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();

        Vector origin = pos.add(normal.scale(zoom));
        normal = normal.rotateAroundAxis(up, dx).rotateAroundAxis(right, dy).normalize();
        pos = origin.subtract(normal.scale(zoom));
    }

    public void render(ArrayList<Particle> particles, Graphics2D g){   

        particles.sort((a, b) -> {
            double da = a.pos.subtract(pos).dot(normal);
            double db = b.pos.subtract(pos).dot(normal);
            return view.equals("orthogonal") ? Double.compare(db, da) : Double.compare(db-((b.rad*b.rad)/db), da-((a.rad*a.rad)/da));
        });

        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        for (Particle p : particles) {
            Vector rel = p.pos.subtract(pos);

            double depth = rel.dot(normal);
            
            double x = rel.dot(right), y = rel.dot(up), r = p.rad;

            if (view.equals("orthogonal")){
                if(r/zoom > Environment.RESOLUTION * Environment.ASPECT_RATIO) continue;
                x /= zoom;
                y /= zoom;
                r /= zoom;
            }

            if (view.equals("perspective")) {
                if (depth > 0) {
                    x /= (depth);
                    y /= (depth);
                    r = Math.min(Math.sqrt(r*r - (r*r*r*r)/(depth*depth))/(depth - (r*r)/depth), 100000);
                } else if(depth==0){
                    r = 100000;
                } else{
                    r = 0;
                }
                x *= 100; y *= 100; r *= 100;
            }
            
            g.setColor(p.luminosity);
            if(p.shape.equals("circle")){
                g.fillOval((int) (x-r), (int) (y-r), (int) (2*r), (int) (2*r));
            }
        }
    }

    public void drawGrid(Graphics2D g) {
        double gridMin = -100 ;
        double gridMax = 100;
        double step = 20;

        for (double x = gridMin + step; x <= gridMax - step; x += step) {
            Point[] line = projectLineToScreen(new Vector(x, gridMin, 0), new Vector(x, gridMax, 0), view);
            if(line == null) continue;
            g.setColor(Color.ORANGE);
            g.drawLine(line[0].x, line[0].y, line[1].x, line[1].y);
        }
        for (double y = gridMin + step; y <= gridMax - step; y += step) {
            Point[] line = projectLineToScreen(new Vector(gridMin, y, 0), new Vector(gridMax, y, 0), view);
            if(line == null) continue;
            g.setColor(Color.ORANGE);
            g.drawLine(line[0].x, line[0].y, line[1].x, line[1].y);
        }
    }

    public void drawOrigin(Graphics2D g) {
        Vector[] ends = { new Vector(0, 0, 100), new Vector(0, 100, 0), new Vector(100, 0, 0) };
        Color[] colors = { Color.RED, Color.BLUE, Color.GREEN };

        class Line {
            Vector end;
            Color color;
            double dist;
            Line(Vector e, Color c) {
                end = e; color = c;
                dist = Math.pow(pos.dot(normal), 2);
            }
        }
        java.util.List<Line> lines = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            lines.add(new Line(ends[i], colors[i]));
        }

        lines.sort((a, b) -> Double.compare(b.dist, a.dist));

        for (Line l : lines) {
            Point[] line = projectLineToScreen(Vector.ORIGIN, l.end, view);
            if(line == null) continue;
            g.setColor(l.color);
            g.drawLine(line[0].x, line[0].y, line[1].x, line[1].y);
        }
    }

    public void drawMiniOrigin(Graphics2D g){ 
        Point offset = new Point((int) (Environment.RESOLUTION * Environment.ASPECT_RATIO * 0.4), (int) (Environment.RESOLUTION * 0.4));

        Vector base = Vector.ORIGIN.add(pos).add(normal.scale(200));
        Vector[] ends = { new Vector(0, 0, 50).add(base), new Vector(0, 50, 0).add(base), new Vector(50, 0, 0).add(base) };
        Color[] colors = { Color.RED, Color.BLUE, Color.GREEN };

        class Line {
            Vector end;
            Color color;
            double dist;
            Line(Vector e, Color c) {
                end = e; color = c;
                dist = Math.pow(pos.dot(normal), 2);
            }
        }
        java.util.List<Line> lines = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            lines.add(new Line(ends[i], colors[i]));
        }

        lines.sort((a, b) -> Double.compare(b.dist, a.dist));

        for (Line l : lines) {
            Point[] line = projectLineToScreen(base, l.end, "");
            if(line == null) continue;
            g.setColor(l.color);
            g.drawLine(line[0].x + offset.x, line[0].y + offset.y, line[1].x + offset.x, line[1].y + offset.y);
        }
    }

    //proj 3d line to 2d surface, clipped
    private Point[] projectLineToScreen(Vector p, Vector q, String view) {
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        Vector pRel = p.subtract(pos);
        Vector qRel = q.subtract(pos);
        double pDepth = pRel.dot(normal);
        double qDepth = qRel.dot(normal);

        //line behind plane
        if (pDepth < 0 && qDepth < 0 && view.equals("perspective")) {
            return null;
        }

        //clip line
        if ((pDepth < 0 || qDepth < 0) && view.equals("perspective")) {
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

        if (view.equals("orthogonal")) {
            px /= zoom; py /= zoom;
            qx /= zoom; qy /= zoom;
        }

        if (view.equals("perspective")) {
            px /= pDepth / 100; py /= pDepth / 100;
            qx /= qDepth / 100; qy /= qDepth / 100;
        }

        return new Point[]{
            new Point((int) px, (int) py),
            new Point((int) qx, (int) qy)
        };
    }

}
