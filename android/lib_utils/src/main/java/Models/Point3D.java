package Models;

public class Point3D {
    private float x;
    private float y;
    private float z;

    public Point3D(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float distanceTo(Point3D other) {
        float dx = this.x - other.getX();
        float dy = this.y - other.getY();
        float dz = this.z - other.getZ();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    public void setZ(float log10) {
        this.z = log10;
    }
}
