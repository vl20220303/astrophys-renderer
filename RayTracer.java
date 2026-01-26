import java.awt.Graphics2D;
import java.util.ArrayList;

public class RayTracer extends Camera{
    public RayTracer(Vector pos, Vector normal, boolean usePerspective){
        super(pos, normal, usePerspective);
    }

    public RayTracer(Vector pos, boolean usePerspective){
        super(pos, usePerspective);
    }

    @Override
    public void render(ArrayList<Particle> particles, Graphics2D g){
        
    }
}