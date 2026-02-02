package utils.generator;
import java.util.ArrayList;
import java.util.function.Function;

import utils.utils.DensityFunction;
import utils.utils.Particle;
import utils.utils.Vector;

public class Disk extends Generator {
    public final Vector normal;
    private final DensityFunction radialDensityFunction;
    private final DensityFunction heightDensityFunction;
    public Disk(Vector center, Vector normal, double ir, double or, double height, Function<Double, Double> radialDensityFunction, Function<Double, Double> heightDensityFunction){
        super(center);
        this.normal = normal;
        this.radialDensityFunction = new DensityFunction(radialDensityFunction, ir, or);
        this.heightDensityFunction = new DensityFunction(heightDensityFunction, -height/2, height/2);
    }

    public Disk(Vector center, Vector normal, DensityFunction radialDensityFunction, DensityFunction heightDensityFunction){
        super(center);
        this.normal = normal;
        this.radialDensityFunction = radialDensityFunction;
        this.heightDensityFunction = heightDensityFunction;
    }

    public void setResolution(double radialRes, double heightRes){
        radialDensityFunction.setResolution(radialRes);
        heightDensityFunction.setResolution(heightRes);
    }

    @Override
    public void setup(){
        radialDensityFunction.init();
        heightDensityFunction.init();
    }

    @Override
    public ArrayList<Particle> generate(int num, Particle template){
        radialDensityFunction.checkSetup();
        heightDensityFunction.checkSetup();
        ArrayList<Particle> out = new ArrayList<Particle>();
        for(int i = 0; i<num; i++){
            double r = radialDensityFunction.invCDF(Math.random());
            double h = heightDensityFunction.invCDF(Math.random());
            double theta = Math.random()*2*Math.PI;
            Vector diff = (Vector.X_AXIS.equals(normal) ? Vector.Y_AXIS : Vector.X_AXIS).cross(normal)
                            .rotateAroundAxis(normal, theta).scaleInPlace(r)
                            .addInPlace(normal.scale(h));

            Vector vel = new Vector(0,0,0);
            if(centerVel!=null){
                vel = vel.add(centerVel);
            }
            if(rotationalVelRadial!=null){ 
                Double rotationalVel = rotationalVelRadial.apply(r);
                Vector rotationalVec = rotationalAxis.cross(diff).normalize().scale(rotationalVel);
                vel.addInPlace(rotationalVec);
            }
            if(rotationalVelPositional!=null){
                Vector rotationalVec = rotationalVelPositional.apply(diff);
                vel.addInPlace(rotationalVec);
            }

            Particle particle = new Particle(template);
            particle.pos = center.add(diff);
            particle.vel = vel;
            out.add(particle);
        }
        return out;
    }
}
