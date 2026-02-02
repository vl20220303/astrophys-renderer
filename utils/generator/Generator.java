package utils.generator;
import java.util.ArrayList;
import java.util.function.Function;

import utils.utils.Particle;
import utils.utils.Vector;

public abstract class Generator {
    protected Vector center;
    protected Vector centerVel;
    protected Vector rotationalAxis;
    protected Function<Double, Double> rotationalVelRadial;
    protected Function<Vector, Vector> rotationalVelPositional;

    public Generator(Vector center){
        this.center = center;
    }

    public abstract void setup();
    public abstract ArrayList<Particle> generate(int num, Particle template);
    public Generator withCenterVel(Vector centerVel){ this.centerVel = centerVel; return this; }
    public Generator withRotationalVel(Vector rotationalAxis, Function<Double, Double> rotationalVelRadial){
        this.rotationalAxis = rotationalAxis;
        this.rotationalVelRadial = rotationalVelRadial;
        return this;
    }
    public Generator withRotationalVel(Function<Vector, Vector> rotationalVelPositional){
        this.rotationalVelPositional = rotationalVelPositional;
        return this;
    }
}
