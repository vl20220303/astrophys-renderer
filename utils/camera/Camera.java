package utils.camera;
import java.util.ArrayList;
import java.util.Arrays;

import utils.settings.Settings;
import utils.utils.Particle;
import utils.utils.Vector;

import java.awt.GradientPaint;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

public abstract class Camera {

    protected Settings environment;
    public Vector normal, pos;
    public double zoom;
    public double scale = 1;

    public Camera(Vector pos, Vector normal){
        this.pos = pos; this.normal = normal.normalize();
        this.zoom = normal.abs();
    }

    public void setEnvironment(Settings environment){ this.environment = environment; }

    public void zoom(double ox, double oy, double ticks){
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        Vector shiftVec = right.scale(-ticks*ox).add(up.scale(-ticks*oy));

        Vector origin = pos.add(shiftVec.scale(zoom)).add(normal.scale(zoom));
        zoom = Math.max(zoom*Math.pow(1.05, ticks), 0.00000001);
        pos = origin.subtract((normal.scale(zoom)).add((shiftVec).scale(zoom)));
    }

    public void scale(double ticks){ scale = Math.max(scale+ticks, 0.00000001);}

    public void shift(double dx, double dy){
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        up = right.cross(normal).normalize();

        Vector shiftVec = right.scale(dx).add(up.scale(dy));

        pos = pos.add(shiftVec);
    }

    public void orbit(double ox, double oy, double dx, double dy){
        ox*=zoom; oy*=zoom;
        Vector up = new Vector(0, 1, 0);
        Vector right = normal.cross(up).normalize();
        
        if(1-Math.pow(normal.dot(up),2) < Math.pow(dy, 2)) {
            dy*=Math.max(0, -Math.signum(normal.dot(up)*dy));
        }

        Vector origin = pos.add(normal.scale(zoom));
        normal = normal.rotateAroundAxis(up, dx).rotateAroundAxis(right, dy).normalize();
        pos = origin.subtract(normal.scale(zoom));
    }
    
    public abstract void render(ArrayList<Particle> particles, Graphics2D g);

    public void drawGrid(Graphics2D g) {
        double gridMin = -100 ;
        double gridMax = 100;
        double step = 20;

        for (double x = gridMin + step; x <= gridMax - step; x += step) {
            Point[] line = projectLineToScreen(new Vector(x, gridMin, 0), new Vector(x, gridMax, 0));
            if(line == null) continue;
            g.setColor(Color.ORANGE);
            g.drawLine((int) (line[0].x*scale), (int) (line[0].y*scale), (int) (line[1].x*scale), (int) (line[1].y*scale));
        }
        for (double y = gridMin + step; y <= gridMax - step; y += step) {
            Point[] line = projectLineToScreen(new Vector(gridMin, y, 0), new Vector(gridMax, y, 0));
            if(line == null) continue;
            g.setColor(Color.ORANGE);
            g.drawLine((int) (line[0].x*scale), (int) (line[0].y*scale), (int) (line[1].x*scale), (int) (line[1].y*scale));
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
            Point[] line = projectLineToScreen(Vector.ORIGIN, l.end);
            if(line == null) continue;
            g.setColor(l.color);
            g.drawLine((int) (line[0].x*scale), (int) (line[0].y*scale), (int) (line[1].x*scale), (int) (line[1].y*scale));
        }
    }

    protected abstract Point[] projectLineToScreen(Vector p, Vector q);
}
