import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Arrays;

public class Display{
    
    Camera camera;

    private boolean DRAW_MINI_ORIGIN = true;    //draws origin axes reference in top right
    private boolean DRAW_ZOOM_INDICATOR = true; //draws zoom indicator in top right

    public Display(Camera c){
        this.camera = c;
    }

    public void enableMiniOrigin(){ DRAW_MINI_ORIGIN = true; }
    public void disableMiniOrigin(){ DRAW_MINI_ORIGIN = false; }

    public void enableZoomIndicator(){ DRAW_ZOOM_INDICATOR = true; }
    public void disableZoomIndicator(){ DRAW_ZOOM_INDICATOR = false; }

    public void drawMiniOrigin(Graphics2D g){
        if(!DRAW_MINI_ORIGIN) return;

        final Point offset = new Point((int) (Environment.RESOLUTION * Environment.ASPECT_RATIO * 0.4), (int) (Environment.RESOLUTION * 0.4));

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
        if(!DRAW_ZOOM_INDICATOR) return;

        final Point offset = new Point((int) (Environment.RESOLUTION * Environment.ASPECT_RATIO * 0.4), (int) (Environment.RESOLUTION * 0.35));

        final int scaleRadius = (int) (5*(camera.scale>1 ? Math.log(Math.E - 1 + camera.scale) : camera.scale));

        g.setColor(Color.RED);
        g.fillOval(offset.x - scaleRadius, offset.y - scaleRadius, 2*scaleRadius, 2*scaleRadius);
        g.setColor(Color.BLUE);
        g.drawOval(offset.x - 5, offset.y - 5,10,10);
        g.setColor(Color.BLACK);
    }
}