package utils.generator;
import java.util.ArrayList;
import java.util.function.Function;

import utils.utils.Particle;
import utils.utils.Vector;

public class Ray extends Generator{
    private Vector line;
    public Ray(Vector center, Vector normal, double ir, double or, Function<Double, Double> densityFunction, Boolean twoTailed){
        super(center, ir, or, densityFunction);
        this.line = normal;
    }

    @Override
    public ArrayList<Particle> generate(int num, Particle template, Vector centerVel, Vector rotationAxis, Function<Double, Double> getRotationalVel){
        checkSetup();
        ArrayList<Particle> out = new ArrayList<Particle>();
        for(double r = ir; r<=or; r+=resolution){
            for(int i = 0; i<Math.round(densityFunction.apply(r)/total*num); i++){
                double nr = (r+resolution*Math.random()) * (Math.signum(Math.random()-0.5));
                Vector diff = line.normalize().scale(nr);

                Vector vel = new Vector(0,0,0);
                if(centerVel!=null){ vel = vel.add(centerVel); }
                if(getRotationalVel!=null){ 
                    Double rotationalVel = getRotationalVel.apply(nr);
                    Vector rotationalVec = rotationAxis.cross(diff).normalize().scale(rotationalVel);
                    vel = vel.add(rotationalVec);
                }

                Particle particle = new Particle(template);
                particle.pos = center.add(diff);
                particle.vel = vel;
                out.add(particle);
            }
        }
        return out;
    }
    public ArrayList<Particle> generate(int num, Particle template, Vector centerVel, Function<Double, Double> getRotationalVel){
        return generate(num, template, centerVel, this.line, getRotationalVel);
    }
}