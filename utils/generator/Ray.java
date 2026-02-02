package utils.generator;
import java.util.ArrayList;
import java.util.function.Function;

import utils.utils.DensityFunction;
import utils.utils.Particle;
import utils.utils.Vector;

public class Ray extends Generator{
    public final boolean twoTailed;
    public final Vector ray;
    private final DensityFunction rayDensityFunction;

    public Ray(Vector center, Vector ray, double ir, double or, Function<Double, Double> rayDensityFunction, Boolean twoTailed){
        super(center);
        this.ray = ray.normalize();
        this.rayDensityFunction = new DensityFunction(rayDensityFunction, ir, or);
        this.twoTailed = twoTailed;
    }

    public Ray(Vector center, Vector ray, DensityFunction densityFunction, Boolean twoTailed){
        super(center);
        this.ray = ray;
        this.rayDensityFunction = densityFunction;
        this.twoTailed = twoTailed;
    }

    public void setResolution(double rayRes){
        rayDensityFunction.setResolution(rayRes);
    }

    @Override
    public void setup(){
        rayDensityFunction.init();
    }

    @Override
    public ArrayList<Particle> generate(int num, Particle template){
        rayDensityFunction.checkSetup();
        ArrayList<Particle> out = new ArrayList<Particle>();
        for(int i = 0; i<num; i++){
            double r = rayDensityFunction.invCDF(Math.random());
            if(twoTailed) r*=Math.pow(-1, (int) (Math.random()+0.5));

            Vector diff = ray.scale(r);

            Vector vel = new Vector(0,0,0);
            if(centerVel!=null){ 
                vel = vel.add(centerVel); 
            }
            if(rotationalVelRadial!=null){ 
                Double rotationalVel = rotationalVelRadial.apply(r);
                Vector rotationalVec = rotationalAxis.cross(diff).normalize().scale(rotationalVel);
                vel = vel.add(rotationalVec);
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