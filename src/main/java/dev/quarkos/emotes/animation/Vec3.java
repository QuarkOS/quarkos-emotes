package dev.quarkos.emotes.animation;

public record Vec3(double x, double y, double z) {
    public static final Vec3 ZERO = new Vec3(0, 0, 0), ONE = new Vec3(1, 1, 1);
    public Vec3 lerp(Vec3 b, double t) { return new Vec3(x+(b.x-x)*t, y+(b.y-y)*t, z+(b.z-z)*t); }
    public double magnitude() { return Math.abs(x)+Math.abs(y)+Math.abs(z); }
    public static Vec3 catmull(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        return new Vec3(cat(p0.x,p1.x,p2.x,p3.x,t),cat(p0.y,p1.y,p2.y,p3.y,t),cat(p0.z,p1.z,p2.z,p3.z,t));
    }
    private static double cat(double a,double b,double c,double d,double t) {
        return .5*((2*b)+(-a+c)*t+(2*a-5*b+4*c-d)*t*t+(-a+3*b-3*c+d)*t*t*t);
    }
}
