package utils.settings;
import java.awt.Color;

public class Settings {
    public final int TICK_SPEED;

    // SCREEN
    public final int RESOLUTION;                //px height
    public final double ASPECT_RATIO;           //width/height
    public final boolean ANTIALIASING_ENABLED;  //edge softening
    public final Color BACKGROUND_COLOR;

    // COORDINATE PLANE
    public final boolean DRAW_ORIGIN;           //draws origin axes
    public final boolean DRAW_ORIGIN_GRID;      //draws xy plane
    public final boolean DRAW_MINI_ORIGIN;      //draws origin axes reference in top right
    public final boolean DRAW_ZOOM_INDICATOR;   //draws zoom indicator in top right

    public Settings(int tick_speed, int resolution, double aspect_ratio, boolean antialiasing_on, Color background_color, boolean draw_origin, boolean draw_origin_grid, boolean draw_mini_origin, boolean draw_zoom_indicator){
        this.TICK_SPEED = tick_speed;
        this.RESOLUTION = resolution;
        this.ASPECT_RATIO = aspect_ratio;
        this.ANTIALIASING_ENABLED = antialiasing_on;
        this.BACKGROUND_COLOR = background_color;
        this.DRAW_ORIGIN = draw_origin;
        this.DRAW_ORIGIN_GRID = draw_origin_grid;
        this.DRAW_MINI_ORIGIN = draw_mini_origin;
        this.DRAW_ZOOM_INDICATOR = draw_zoom_indicator;
    }

    public static class Builder{
        private int tick_speed = 16; //60FPS default
        
        private int resolution = 720;
        private double aspect_ratio = 1.778;
        private boolean antialiasing_on = false;
        private Color background_color = Color.WHITE;

        private boolean draw_origin = false;
        private boolean draw_origin_grid = false;
        private boolean draw_mini_origin = false;
        private boolean draw_zoom_indicator = false;

        public Settings build(){
            return new Settings(tick_speed, resolution, aspect_ratio, antialiasing_on, background_color, draw_origin, draw_origin_grid, draw_mini_origin, draw_zoom_indicator);
        }

        /** Set tick speed (milliseconds). */
        public Builder setTickSpeed(int i){ tick_speed = i; return this; }
        public Builder use60FPS(){ this.setTickSpeed(16); return this; }
        public Builder use30FPS(){ this.setTickSpeed(33); return this; }
        public Builder use10FPS(){ this.setTickSpeed(100); return this; }

        /** Set window height (pixels). 
         * @return */
        public Builder setResolution(int r){ resolution = r; return this; }
        /** Set aspect ratio (width/height). */
        public Builder setAspectRatio(double d){ aspect_ratio = d; return this; }
        public Builder use480p() { this.setResolution(480); this.setAspectRatio(1.778); return this;}
        public Builder use720p() { this.setResolution(720); this.setAspectRatio(1.778); return this;}
        public Builder use1080p() { this.setResolution(1080); this.setAspectRatio(1.778); return this;}

        public Builder enableAntiAliasing(){ antialiasing_on = true; return this; }

        public Builder setBackgroundColor(Color c){ background_color = c; return this; }

        public Builder enableOrigin(){ draw_origin = true; return this; }
        public Builder enableOriginGrid(){ draw_origin_grid = true; return this; }
        public Builder enableViewIndicator(){ draw_mini_origin = true; return this; }
        public Builder enableZoomIndicator(){ draw_zoom_indicator = true; return this; }

    }

}