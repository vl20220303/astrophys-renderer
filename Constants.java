public class Constants {
    public static double GRAV_CONST = 6.67430e-11;

    public static void init(){
        GRAV_CONST*=Math.pow(Environment.SCALE, 3)*Math.pow(Environment.WEIGHT, 2);
    }
}