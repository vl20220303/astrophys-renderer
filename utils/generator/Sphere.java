package utils.generator;
import java.util.ArrayList;
import java.util.function.Function;

import utils.utils.Particle;
import utils.utils.Vector;

public class Sphere extends Generator {
    
    public Sphere(Vector center, double ir, double or, Function<Double, Double> densityFunction){
        super(center, ir, or, densityFunction);
    }

    @Override
    public ArrayList<Particle> generate(int num, Particle template, Vector centerVel, Vector rotationAxis, Function<Double, Double> getRotationalVel){
        checkSetup();
        ArrayList<Particle> out = new ArrayList<Particle>();
        for(double r = ir; r<=or; r+=resolution){
            for(int i = 0; i<Math.round(densityFunction.apply(r)/total*num); i++){
                double phi = Math.random()*Math.PI;
                double theta = Math.random()*2*Math.PI;
                Vector diff = new Vector(Math.cos(phi), Math.sin(phi)*Math.cos(theta), Math.sin(phi)*Math.sin(theta)).scale(r);

                Vector vel = new Vector(0,0,0);
                if(centerVel!=null){ vel = vel.add(centerVel); }
                if(getRotationalVel!=null){ 
                    Double rotationalVel = getRotationalVel.apply(r);
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
}
