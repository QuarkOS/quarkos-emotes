package dev.quarkos.emotes.render;

/**
 * Third-person portrait camera. Distance is zoom in blocks. Drop lowers the
 * camera from eye height. Positive pitch looks down. Forward shifts the camera
 * along the player's look so a limb reaching toward the lens does not fill the frame.
 */
public record ShotFraming(float distance, float drop, float forward, float pitch, float orbit) {
    public static ShotFraming forId(String id, float orbitDegrees) {
        String name = id == null ? "" : id.substring(Math.max(0, id.indexOf(':') + 1));
        return switch (name) {
            // Prone bodies point away from a front camera. Sit above the eyes, look down, and orbit.
            case "pushups" -> new ShotFraming(5.4f, -0.55f, 0.15f, 32f, 68f);
            case "lay_down", "sleep", "worm" -> new ShotFraming(5.2f, -0.45f, 0.1f, 30f, 74f);
            // Sit above the standing portrait and look down so the lap is not hidden by the ground line.
            case "meditate", "sit", "sit_full", "kneel" -> new ShotFraming(4.4f, 0.2f, 0.2f, 18f, 26f);
            case "bow" -> new ShotFraming(3.55f, 0.28f, 0f, 2f, 0f);
            case "backflip" -> new ShotFraming(3.65f, 0.2f, 0f, 4f, 0f);
            case "spin" -> new ShotFraming(3.55f, 0.45f, 0f, 2f, 0f);
            case "jump_hype" -> new ShotFraming(3.7f, 0.35f, 0f, 2f, 0f);
            case "tableflip" -> orbitDegrees == 0f
                    ? new ShotFraming(3.5f, 0.85f, 0f, 6f, 0f)
                    : new ShotFraming(3.2f, 0.8f, 0.55f, 6f, 0f);
            default -> new ShotFraming(3.25f, 0.62f, 0f, 0f, 0f);
        };
    }
}
