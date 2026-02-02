package utils.utils;

import java.util.function.Function;

public class DensityFunction {
    private final double lowerBound;
    private final double upperBound;
    private final Function<Double, Double> densityFunction;
    private double resolution;

    private double[] cdfarr;
    private boolean initHappened;

    public DensityFunction(Function<Double, Double> densityFunction, double lowerBound, double upperBound){
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.densityFunction = densityFunction;
    }

    public DensityFunction setResolution(double r){
        this.resolution = r;
        initHappened = false;
        return this;
    }

    public void init(){
        if(initHappened) return;

        int nums = (int) ((upperBound - lowerBound)/resolution);
        if(nums>=Integer.MAX_VALUE) throw new Error("Cannot construct CDF! Out of memory. Consider increasing resolution with setResolution().");

        cdfarr = new double[nums+1];

        int i = 0; double cumsum = 0;
        for(double r = lowerBound; r<=upperBound; r+=resolution){
            double result = densityFunction.apply(r);
            if(result==Double.NaN || result==Double.NEGATIVE_INFINITY || result==Double.POSITIVE_INFINITY)
                throw new Error("Density function returned " + result + "! Check your function and bounds.");
            cumsum += result; cdfarr[i] = cumsum; i++;
        }

        if(cumsum == 0) throw new Error("Sum of function between " + lowerBound + " and " + upperBound + " is 0!");

        for(i = 0; i<=nums; i++){ cdfarr[i]/=cumsum; }
        initHappened = true;
    }

    public void checkSetup(){
        if(resolution==0){ throw new Error("Resolution not specified! Use setResolution() to set the resolution."); }
        if(!initHappened){ throw new Error("Setup not complete! Run init() to complete density calculations."); }
    }

    public double CDF(double d){
        return cdfarr[(int) ((d-lowerBound)/resolution)];
    }

    public double invCDF(double d){
        if(d<0 || d>1) throw new Error("Input " + d + " out of bounds [0,1]!");

        int low = 0, high = cdfarr.length, mid = (high+low)/2;
        while(low<high){
            if(Math.abs(d - cdfarr[mid]) < 0.001){
                break;
            } else if(d - cdfarr[mid] > 0){
                low = mid+1;
            } else{
                high = mid;
            }
            mid = (high+low)/2;
        }
        if(d - cdfarr[mid] > 0.001) mid+=(cdfarr[mid+1]-cdfarr[mid])*(cdfarr[mid] - d);
        return lowerBound + mid*resolution;
    }
}
