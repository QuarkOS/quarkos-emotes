package dev.quarkos.emotes.animation;

/** A pivoted Z-Y-X transform in Java model pixels (feet at Y=0). */
public record PropTransform(Vec3 pivot,Vec3 rotation,Vec3 position,Vec3 scale) {
    public static final java.util.Set<String> PLAYER_BONES=java.util.Set.of("head","body","right_arm","left_arm","right_leg","left_leg");
    public static PropTransform geometry(Vec3 pivot,Vec3 rotation,Vec3 position,Vec3 scale) {
        return new PropTransform(BedrockAxes.geometry(pivot),BedrockAxes.rotation(rotation),BedrockAxes.position(position),scale);
    }
    /** ModelPart positions use a top-of-head origin; prop geometry uses the feet. */
    public static PropTransform parent(Vec3 initialModelPivot,Emote.Pose posed) {
        Vec3 pivot=new Vec3(initialModelPivot.x(),initialModelPivot.y()-24,initialModelPivot.z());
        Vec3 p=posed.position();
        return new PropTransform(pivot,posed.rotation(),new Vec3(p.x()-initialModelPivot.x(),p.y()-initialModelPivot.y(),p.z()-initialModelPivot.z()),posed.scale());
    }
    /** Parent.apply(child.apply(point)): same order as the render pose stack. */
    public Vec3 apply(Vec3 point) {
        double x=(point.x()-pivot.x())*scale.x(),y=(point.y()-pivot.y())*scale.y(),z=(point.z()-pivot.z())*scale.z();
        double cx=Math.cos(rotation.x()),sx=Math.sin(rotation.x()),cy=Math.cos(rotation.y()),sy=Math.sin(rotation.y()),cz=Math.cos(rotation.z()),sz=Math.sin(rotation.z());
        double ry=y*cx-z*sx,rz=y*sx+z*cx,rx=x*cy+rz*sy;
        rz=-x*sy+rz*cy;
        return new Vec3(pivot.x()+position.x()+rx*cz-ry*sz,pivot.y()+position.y()+rx*sz+ry*cz,pivot.z()+position.z()+rz);
    }
}
