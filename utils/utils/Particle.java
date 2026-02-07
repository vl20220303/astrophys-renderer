package utils.utils;
import java.awt.Color;
// import java.nio.file.NotLinkException;

import utils.settings.Constants;

public class Particle{
    
    public Vector pos   = new Vector(Vector.ORIGIN);
    public Vector vel   = new Vector(Vector.ORIGIN);
    public Vector accel = new Vector(Vector.ORIGIN);

    public double rad  = 0;
    public double mass = 0;
    public Shape shape = Shape.SPHERE;
    public enum Shape{
        SPHERE,
        IMAGE;
    }

    public Color color = Color.BLACK;
    public double intensity = 0;
    public double reflectivity = 0;
    public double diffusion = 0;

    public boolean fixed = false;

    public enum Behavior{
        NONE(0),
        SEPARATE(1),
        COLLIDE(2);

        private final int val;
        private Behavior(int i){
            val = i;
        }
    }
    public Behavior collideType = Behavior.NONE;

    public Particle(){}

    public Particle(Vector pos, int rad, double mass, Shape shape, Vector vel, Color luminosity, boolean fixed, Behavior collideType){
        this.pos = new Vector(pos); this.rad = rad; this.mass = mass; this.shape = shape; this.vel = new Vector(vel); this.color = luminosity; this.fixed = fixed; this.collideType = collideType;

        this.accel = new Vector(0,0,0);
    }

    public Particle(Particle p){
        this.pos = p.pos; 
        this.vel = p.vel; 
        this.accel = p.accel;

        this.rad = p.rad; 
        this.mass = p.mass; 
        this.shape = p.shape; 

        this.color = p.color;
        this.intensity = p.intensity;

        this.fixed = p.fixed; 
        this.collideType = p.collideType;

        this.reflectivity = p.reflectivity;
        this.diffusion = p.diffusion;
    }

    //* Set Position */
    public Particle setPos(Vector pos){ this.pos = new Vector(pos); return this; }
    //* Set Velocity */
    public Particle setVel(Vector vel){ this.vel = new Vector(vel); return this; }
    //* Set Radius, Mass, & Shape */
    public Particle setAttributes(double rad, double mass, Shape shape){
        this.rad = rad;
        this.mass = mass;
        this.shape = shape;
        return this;
    }
    //* Set Color & Luminosity */
    public Particle setColor(Color color, double luminosity){
        this.color = color;
        this.intensity = luminosity;
        return this;
    }
    //* Fix particle in place */
    public Particle fixed(){ this.fixed = true; return this; }
    //* Set Collision Behavior */
    public Particle setBehavior(Behavior b){
        this.collideType = b;
        return this;
    }

    public void collide(Particle other, Constants c){
        if(this == other) return;
        if(this.fixed && other.fixed) return;

        int collideType = Math.min(this.collideType.val * other.collideType.val, 2);
        if(collideType == 0) return;

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

            overlap+=1e-4;

            Vector normalVector = displacement.normalize().scale(overlap);

            if(!this.fixed && !other.fixed){
                double thisScaleFactor = other.mass/(this.mass + other.mass);

                Vector thisSep = normalVector.scale(thisScaleFactor);
                Vector otherSep = normalVector.scale(1 - thisScaleFactor);

                this.pos.addInPlace(thisSep);
                other.pos.subtractInPlace(otherSep);
            } else if(!this.fixed){ 
                this.pos.addInPlace(normalVector); 
            } else if(!other.fixed){ 
                other.pos.subtractInPlace(normalVector); 
            }

        }

        if(collideType == 1){ //inelastic collision

            if(!this.fixed && other.fixed){ this.accel = this.accel.add(normal.scale(-v1)); }
            else if(this.fixed && !other.fixed){ other.accel = other.accel.add(normal.scale(-v2)); }
            else if(!this.fixed && !other.fixed){
                double a = (m1 * v1 + m2 * v2) / (m1 + m2);

                this.accel = this.accel.add(normal.scale(a-v1));
                other.accel = other.accel.add(normal.scale(a-v2));
            }

        } else if(collideType == 2){ //elastic collision

            if(!this.fixed && other.fixed){ this.accel = this.accel.add(normal.scale(-2*v1)); }
            else if(this.fixed && !other.fixed){ other.accel = other.accel.add(normal.scale(-2*v2)); }
            else if(!this.fixed && !other.fixed){
                double a = 2 * (v2 - v1) / (m1 + m2);
                
                this.accel = this.accel.add(normal.scale(a * m2));
                other.accel = other.accel.add(normal.scale(-a * m1));
            }

        }

    }

    public void gravitate(Particle other, Constants c){
        if(this == other) return;
        if(this.fixed && other.fixed) return;

        Vector displacement = this.pos.subtract(other.pos);
        double dist = displacement.abs();
        double overlap = Math.max((this.rad + other.rad) - dist, 0);

        double distSquared = Math.pow(dist + overlap/2, 2);

        Vector gravity = displacement.normalize().scale((c.GRAV_CONST * this.mass * other.mass) / distSquared);
        if(!this.fixed) this.accel.subtractInPlace(gravity.scale(1/this.mass));
        if(!other.fixed) other.accel.addInPlace(gravity.scale(1/other.mass));
    }

    public void update(){
        if(fixed) return;
        vel.addInPlace(accel);
        pos.addInPlace(vel);
    }

    public void clearAccel(){
        accel = new Vector(0, 0, 0);
    }
}