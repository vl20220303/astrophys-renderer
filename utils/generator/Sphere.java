package utils.generator;
import java.util.ArrayList;
import java.util.function.Function;

import utils.utils.DensityFunction;
import utils.utils.Particle;
import utils.utils.Vector;

public class Sphere extends Generator {

    private DensityFunction radialDensityFunction;
    
    public Sphere(Vector center, double ir, double or, Function<Double, Double> radialDensityFunction){
        super(center);
        this.radialDensityFunction = new DensityFunction(radialDensityFunction, ir, or);
    }
    public Sphere(Vector center, DensityFunction radialDensityFunction){
        super(center);
        this.radialDensityFunction = radialDensityFunction;
    }

    public void setResolution(double radialRes){
        radialDensityFunction.setResolution(radialRes);
    }

    @Override
    public void setup(){
        radialDensityFunction.init();
    }

    @Override
    public ArrayList<Particle> generate(int num, Particle template){
        radialDensityFunction.checkSetup();
        ArrayList<Particle> out = new ArrayList<Particle>();
        for(int i = 0; i<num; i++){
            double r = radialDensityFunction.invCDF(Math.random());
            double phi = Math.random()*Math.PI;
            double theta = Math.random()*2*Math.PI;
            Vector diff = new Vector(Math.cos(phi), Math.sin(phi)*Math.cos(theta), Math.sin(phi)*Math.sin(theta)).scale(r);

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
