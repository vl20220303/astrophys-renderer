import java.awt.Color;

public class Environment {
    public static final int TICK_SPEED = 2; //ms

    // SCREEN
    public static final int RESOLUTION = 1000;                   //px height
    public static final double ASPECT_RATIO = 1.5;              //width/height
    public static final boolean ANTIALIASING_ENABLED = true;    //edge softening
    public static final Color BACKGROUND_COLOR = Color.WHITE;

    // COORDINATE PLANE
    public static final boolean DRAW_ORIGIN = true;         //draws origin axes
    public static final boolean DRAW_ORIGIN_GRID = true;    //draws xy plane
    public static final boolean DRAW_MINI_ORIGIN = true;    //draws origin axes reference in top right
    public static final boolean DRAW_ZOOM_INDICATOR = true; //draws zoom indicator in top right

    // RENDERING
    public static final boolean LIGHT_ENABLED = false;
    public static final boolean LIGHT_DIFFUSE = false;

    // PHYSICS
    public static final boolean GRAVITY_ENABLED = true;
}