package utils.utils;

import java.awt.Color;

public class BigColor{
    public double r, g, b;
    public BigColor(Color c){
        r = c.getRed();
        g = c.getGreen();
        b = c.getBlue();
    }
    public BigColor(double r, double g, double b){
        this.r = r; this.g = g; this.b = b;
    }
    public void add(Color c, double intensity){
        r += c.getRed() * intensity;
        g += c.getGreen() * intensity;
        b += c.getBlue() * intensity;
    }
    public void scale(double dropoff){
        r *= dropoff;
        g *= dropoff;
        b *= dropoff;
    }
    public void multiply(Color c, double dropoff){
        r *= c.getRed()/255 * dropoff;
        g *= c.getGreen()/255 * dropoff;
        b *= c.getBlue()/255 * dropoff;
    }
    public Color normalize(){
        double max = Math.max(r, Math.max(g, b));
        double scale = max > 255 ? 255.0 / max : 1.0;
        return new Color((int)(r*scale), (int)(g*scale), (int)(b*scale));
    }
    public Color normalizeTo(BigColor color){
        double maxIntensity = color.intensity();
        if(maxIntensity==0){ return this.normalize(); }
        double scale = this.intensity()/maxIntensity;
        BigColor newColor = new BigColor(r*scale, g*scale, b*scale);
        return newColor.normalize();
    }
    public double intensity(){
        return this.r + this.g + this.b;
    }
    public String toString(){
        return "[r=" + r + ",g=" + g + ",b=" + b + "]";
    }
}