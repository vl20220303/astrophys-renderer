import java.awt.Color;
import java.util.ArrayList;

public class Particle{
    public Vector pos;
    public int rad;
    public double mass;
    public String shape;

    public Vector vel;
    public Vector accel;

    public Color luminosity; 
    public boolean fixed;
    public String collideType; //none, separate, collide

    public Particle(Vector pos, int rad, double mass, String shape, Vector vel, Color luminosity, boolean fixed, String collideType){
        this.pos = new Vector(pos); this.rad = rad; this.mass = mass; this.shape = shape; this.vel = new Vector(vel).scale(Environment.TICK_SPEED); this.luminosity = luminosity; this.fixed = fixed; this.collideType = collideType;

        this.accel = new Vector(0,0,0);
    }

    // public ArrayList<Particle> createParticles(Equation[] eqautions, int particleSize, double totalMass, String shape, Equation[] velEquations, double[] luminosity, String collideType){
        
    // }

    public void collide(Particle other){
        if(this == other) return;

        if(this.fixed && other.fixed) return;

        int aggrType;
        if(this.collideType.equals("none") && other.collideType.equals("none")){
            aggrType = 0; //none
        } else if(this.collideType.equals("separate") || other.collideType.equals("separate")){
            aggrType = 1; //separate
        } else{
            aggrType = 2; //collide, default
        }

        if(aggrType == 0) return;

        Vector displacement = this.pos.subtract(other.pos);
        double dist = displacement.abs();
        double overlap = (this.rad + other.rad) - dist;

        if(overlap < 0) return; //no overlap or contact
        
        Vector normal = displacement.normalize();

        double v1 = this.vel.dot(normal);
        double v2 = other.vel.dot(normal);
     
        double m1 = this.mass;
        double m2 = other.mass;

        if(overlap > 0){ //resolve overlap

            overlap+=0.1;

            Vector normalVector = displacement.normalize().scale(overlap);

            if(!this.fixed && !other.fixed){
                double thisScaleFactor = other.mass/(this.mass + other.mass);

                Vector thisSep = normalVector.scale(thisScaleFactor);
                Vector otherSep = normalVector.scale(1 - thisScaleFactor);

                this.pos = this.pos.add(thisSep);
                other.pos = other.pos.subtract(otherSep);
            }

            else if(!this.fixed){ this.pos = this.pos.add(normalVector); }
            else if(!other.fixed){ other.pos = other.pos.subtract(normalVector); }

        }

        if(aggrType == 1){ //inelastic collision

            if(!this.fixed && !other.fixed){
                double a1 = (m1 * v1 + m2 * v2) / (m1 + m2) - v1;
                double a2 = (m1 * v1 + m2 * v2) / (m1 + m2) - v2;

                this.accel = this.accel.add(normal.scale(a1));
                other.accel = other.accel.add(normal.scale(a2));
            }

            else if(!this.fixed){ this.accel = this.accel.add(normal.scale(-v1)); }
            else if(!other.fixed){ other.accel = other.accel.add(normal.scale(-v2)); }

        }

        if(aggrType == 2){ //elastic collision

            if(!this.fixed && !other.fixed){
                double a1 = 2 * m2 * (v2 - v1) / (m1 + m2);
                double a2 = 2 * m1 * (v1 - v2) / (m1 + m2);
                
                this.accel = this.accel.add(normal.scale(a1));
                other.accel = other.accel.add(normal.scale(a2));
            }

            else if(!this.fixed){ this.accel = this.accel.add(normal.scale(-2*v1)); }
            else if(!other.fixed){ other.accel = other.accel.add(normal.scale(-2*v2)); }

        }

    }

    public void gravitate(Particle other){
        if(this == other) return;

        Vector displacement = this.pos.subtract(other.pos);
        double dist = displacement.abs();
        double overlap = Math.max((this.rad + other.rad) - dist, 0);

        double distSquared = (dist + overlap/2) * (dist + overlap/2);

        Vector gravity = displacement.normalize().scale((Environment.GRAV_CONST * this.mass * other.mass) / distSquared);
        if(!this.fixed) this.accel = this.accel.subtract(gravity.scale(1/this.mass));
        if(!other.fixed) other.accel = other.accel.add(gravity.scale(1/other.mass));
    }

    public void update(){
        if(fixed) return;
        vel = vel.add(accel);
        pos = pos.add(vel);
    }

    public void clearAccel(){
        accel = new Vector(0, 0, 0);
    }
}