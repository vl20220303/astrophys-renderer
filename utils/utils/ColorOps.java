package utils.utils;

import java.awt.Color;

public class ColorOps {
    public static Color add(Color base, Color color, float alpha){
        int r = (int) (base.getRed() + alpha*color.getRed());
        int g = (int) (base.getGreen() + alpha*color.getGreen());
        int b = (int) (base.getBlue() + alpha*color.getBlue());
        return new Color(r, g, b);
    }

    public static Color multiply(Color base, Color color, float alpha){
        int r = (int) (base.getRed() * (1 + alpha*color.getRed()/255)/2);
        int g = (int) (base.getGreen() * (1 + alpha*color.getGreen()/255)/2);
        int b = (int) (base.getBlue() * (1 + alpha*color.getBlue()/255)/2);
        return new Color(r,g,b);
    }

    public static Color average(Color... colors){
        int r = 0;
        int g = 0;
        int b = 0;
        for(Color c : colors){
            r+=c.getRed();
            g+=c.getGreen();
            b+=c.getBlue();
        }
        r/=colors.length; g/=colors.length; b/=colors.length; 
        return new Color(r,g,b);
    }

    public static Color scale(Color color, double dropoff){
        int r = (int) (color.getRed() * dropoff);
        int g = (int) (color.getGreen() * dropoff);
        int b = (int) (color.getBlue() * dropoff);
        return new Color(r,g,b);
    }
}
