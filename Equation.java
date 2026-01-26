import java.util.ArrayList;
import java.util.function.Function;

public abstract class Equation {
    protected Vector center;
    protected double ir, or;
    protected Function<Double, Double> densityFunction;
    protected double total;
    protected double resolution;

    protected double[] etc = new double[]{0, 2*Math.PI}, andthen = new double[]{0, 0};

    public Equation(Vector center, double ir, double or, Function<Double, Double> densityFunction){
        this.center = center; this.ir = ir; this.or = or;
        this.densityFunction = densityFunction;
    }

    public Equation(int dims, Vector center, double ir, double or){
        this(center, ir, or, (Double r) -> 1d);
    }

    public void setResolution(double res){ this.resolution = res; }

    public void setup(){
        double result = 0;
        for(double r = ir; r<=or; r+=resolution){
            result+=densityFunction.apply(r);
        }
        total = result;
    }

    public void checkSetup(){
        if(resolution==0){ throw new Error("Resolution not specified! Use setResolution() to set the resolution."); }
        if(total==0){ throw new Error("Equation not setup! Run setup() to complete density calculations."); }
        if(Double.isInfinite(total)){ throw new Error("Calculated mass is infinite. Check densityFunction()."); }
    }

    public abstract ArrayList<Particle> generate(int num, Particle template, Vector centerVel, Vector rotationAxis, Function<Double, Double> getRotationalVel);
}
