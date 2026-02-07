package utils.utils;

import java.awt.Color;

public class ColorLayer{
    public Color  color;
    public double dropoff;
    public double intensity;

    public enum opType{ ADD, MULTIPLY; };
    public opType operation;
    
    public ColorLayer(Color c, double d, double i){
        this.color = c;
        this.intensity = i;
        this.dropoff = Math.pow(1+d, -2);
        this.operation = opType.ADD;
    }
    public ColorLayer(Color c, double d){
        this.color = c;
        this.intensity = 0;
        this.dropoff = Math.pow(1+d, -2);
        this.operation = opType.MULTIPLY;
    }
    public void compress(Color c, double d){
        this.color = ColorOps.multiply(this.color, color, 1);
        this.dropoff *= Math.pow(1+d, -2);
    }
    @Override
    public String toString(){
        return "<" + operation + ",Color=" + color + ", Dropoff=" + dropoff + ",Intensity=" + intensity + ">";
    }
}