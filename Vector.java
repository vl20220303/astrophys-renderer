public class Vector {
    public static final Vector ORIGIN = new Vector(0,0,0);
    public static final Vector X_AXIS = new Vector(1, 0, 0);
    public static final Vector Y_AXIS = new Vector(0, 1, 0);
    public static final Vector Z_AXIS = new Vector(0, 0, 1);

    public double x, y, z;

    public Vector(double x, double y, double z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector(Vector other){
        this.x = other.x; this.y = other.y; this.z = other.z;
    }

    public Vector add(Vector other) {
        return new Vector(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    public Vector subtract(Vector other) {
        return new Vector(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public Vector scale(double scalar) {
        return new Vector(x * scalar, y * scalar, z * scalar);
    }

    public Vector normalize() {
        double length = this.abs();
        if (length == 0) return new Vector(0, 0, 0);
        return new Vector(x / length, y / length, z / length);
    }

    public double abs(){
        return Math.sqrt(x*x + y*y + z*z);
    }

    public double dot(Vector other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    public Vector cross(Vector other) {
        double cx = this.y * other.z - this.z * other.y;
        double cy = this.z * other.x - this.x * other.z;
        double cz = this.x * other.y - this.y * other.x;
        return new Vector(cx, cy, cz);
    }

    public Vector rotateAroundAxis(Vector axis, double angle) {
        Vector k = axis.normalize();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        Vector term1 = this.scale(cos);
        Vector term2 = k.cross(this).scale(sin);
        Vector term3 = k.scale(k.dot(this) * (1 - cos));

        return term1.add(term2).add(term3);
    }
}