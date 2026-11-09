package dev.quarkos.emotes.animation;

/** Canonical authored player bones: X=-90 points an arm forward, in pixel/model space. */
public final class BedrockAxes {
    private BedrockAxes() {}
    public static Vec3 rotation(Vec3 degrees) {
        // ModelPart's downwards Y axis already supplies the X handedness change.
        // Negating X again would reverse every authored forward/backward arm pose.
        return new Vec3(Math.toRadians(degrees.x()), -Math.toRadians(degrees.y()), -Math.toRadians(degrees.z()));
    }
    public static Vec3 position(Vec3 pixels) { return new Vec3(-pixels.x(),-pixels.y(),pixels.z()); }
    public static String bone(String name) {
        String n=name.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return switch(n) {
            case "head" -> "head";
            case "body", "torso", "root", "waist" -> "body";
            case "rightarm", "armright" -> "right_arm";
            case "leftarm", "armleft" -> "left_arm";
            case "rightleg", "legright" -> "right_leg";
            case "leftleg", "legleft" -> "left_leg";
            default -> name.toLowerCase(java.util.Locale.ROOT);
        };
    }
}
