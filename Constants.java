public class Constants {
    private final double ACTUAL_GRAV_CONST = 6.67430e-11;;
    private final double ACTUAL_LIGHT_SPEED = 2.99792e8;

    public double GRAV_CONST;
    public double LIGHT_SPEED;

    private double UNITS_SCALE, UNITS_WEIGHT, UNITS_TIME = 1;

    /**
     * To scale all constants according to the specified units, for use by the simulator.
     * 
     * <p> UNITS_SCALE is wrt. meters, UNITS_WEIGHT is wrt. kilograms, UNITS_TIME is wrt. seconds.
     */
    public void init(){
        GRAV_CONST  = ACTUAL_GRAV_CONST  * Math.pow(UNITS_SCALE, -3) * Math.pow(UNITS_TIME, 2) * Math.pow(UNITS_WEIGHT, 1) ;
        LIGHT_SPEED = ACTUAL_LIGHT_SPEED * Math.pow(UNITS_SCALE, -1) * Math.pow(UNITS_TIME, 1);
    }

    public void setScale(double s){ UNITS_SCALE = s; }
    public void setWeight(double w){ UNITS_WEIGHT = w; }
    public void setTime(double t){ UNITS_TIME = t; }
}