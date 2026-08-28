package utils.renderer.astrophys.camera;
import java.util.ArrayList;

import utils.renderer.astrophys.settings.Environment;
import utils.renderer.astrophys.utils.Particle;
import utils.renderer.astrophys.utils.Vector;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

public abstract class Camera {

    protected Environment environment;
    public Vector normal, pos;
    public double zoom = 1, scale = 1;

    public boolean useLighting, useGlow;

    public Camera(Vector pos, Vector normal){
        this.pos = pos; this.normal = normal.normalize();
        this.zoom = normal.abs();
    }

    public void useLighting(){ this.useLighting = true; }
    public void useGlow(){ this.useGlow = true; }

    public void setEnvironment(Environment environment){ this.environment = environment; }

    public abstract void zoom(double ox, double oy, double ticks);
    public abstract void scale(double ticks);
    public abstract void focus(double ticks);

    public abstract void shift(double dx, double dy);
    public abstract void jump(double ox, double oy);

    public abstract void orbit(double dx, double dy);
    
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
