package utils.userinterface;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Arrays;

import utils.camera.Camera;
import utils.camera.RayTracer;
import utils.settings.Environment;
import utils.utils.Vector;

public class Display{
    
    Camera camera;
    Environment environment;

    public Display(Camera c){
        this.camera = c;
    }

    public void setEnvironment(Environment environment){ this.environment = environment; }

    public void drawMiniOrigin(Graphics2D g){
        if(!environment.DRAW_MINI_ORIGIN) return;

        final Point offset = new Point((int) (environment.RESOLUTION * environment.ASPECT_RATIO * 0.4), (int) (environment.RESOLUTION * 0.4));

        class ColoredPoint{
            Vector point;
            Color color;
            ColoredPoint(Vector v, Color c){
                this.point = v; this.color = c;
            }
        }

        ColoredPoint[] ends = { new ColoredPoint(new Vector(0,0,50), Color.RED), new ColoredPoint(new Vector(0,50,0), Color.BLUE), new ColoredPoint(new Vector(50,0,0), Color.GREEN)};

        Arrays.sort(ends, (a,b) -> Double.compare( b.point.dot(camera.normal), a.point.dot(camera.normal)));

        Vector up = new Vector(0, 1, 0);
        Vector right = camera.normal.cross(up).normalize();
        up = right.cross(camera.normal).normalize();

        for (ColoredPoint end : ends) {
            double px = end.point.dot(right);
            double py = end.point.dot(up);
            g.setColor(end.color);
            g.drawLine(offset.x, offset.y, (int) px + offset.x, (int) py + offset.y);
        }
    }

    public void drawZoomIndicator(Graphics2D g){
        if(!environment.DRAW_ZOOM_INDICATOR) return;

        final Point offset = new Point((int) (environment.RESOLUTION * environment.ASPECT_RATIO * 0.4), (int) (environment.RESOLUTION * 0.35));

        final int scaleRadius = (int) (5*(camera.scale>1 ? Math.log(Math.E - 1 + camera.scale) : camera.scale));

        g.setColor(Color.RED);
        g.fillOval(offset.x - scaleRadius, offset.y - scaleRadius, 2*scaleRadius, 2*scaleRadius);
        g.setColor(Color.BLUE);
        g.drawOval(offset.x - 5, offset.y - 5,10,10);
        g.setColor(Color.BLACK);
    }

    public void drawFocusIndicator(Graphics2D g){
        if(!environment.DRAW_FOCUS_INDICATOR) return;
        if(!(camera instanceof RayTracer)) return;

        final Point offset = new Point((int) (environment.RESOLUTION * environment.ASPECT_RATIO * 0.4), (int) (environment.RESOLUTION * 0.30));

        final double focusToZoom = ((RayTracer) camera).focalLength/camera.zoom;
        final int scaleRadius = (int) (5*(focusToZoom>1 ? Math.log(Math.E - 1 + focusToZoom) : focusToZoom));

        g.setColor(Color.GREEN);
        g.fillOval(offset.x - scaleRadius, offset.y - scaleRadius, 2*scaleRadius, 2*scaleRadius);
        g.setColor(Color.BLUE);
        g.drawOval(offset.x - 5, offset.y - 5,10,10);
        g.setColor(Color.BLACK);
    }
}