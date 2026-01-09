import java.awt.Color;

public class Environment {
    public static final int TICK_SPEED = 2; //ms

    public static final double SCALE = 1e10; //physical scale

    public static final int RESOLUTION = 800; //px height

    public static final double ASPECT_RATIO = 2; //width/height

    public static final boolean LIGHT_FILTERING = false;
    public static final boolean ANTIALIASING_ENABLED = true;

    public static final boolean DRAW_ORIGIN = true;      //draws origin axes
    public static final boolean DRAW_ORIGIN_GRID = true; //draws xy plane
    public static final boolean DRAW_MINI_ORIGIN = true; //draws origin axes reference in top right

    public static final Color BACKGROUND_COLOR = Color.WHITE;

    public static final boolean GRAVITY_ENABLED = true;


    
    public static final double GRAV_CONST = 6.67430e-11 * SCALE;
}