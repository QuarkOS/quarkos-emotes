package dev.quarkos.emotes.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BedrockAxesTest {
    @Test void negativeNinetyPointsRightArmForward() {
        var r=BedrockAxes.rotation(new Vec3(-90,0,0));
        // A resting Java arm extends down +Y. Rotate its unit tip around local X.
        double tipY=Math.cos(r.x()),tipZ=Math.sin(r.x());
        assertEquals(0,tipY,1e-9);assertEquals(-1,tipZ,1e-9); // forward = -Z
    }
    @Test void positiveNinetyMatchesAuthoredBackwardArmAndBlockbenchAxes() {
        var r=BedrockAxes.rotation(new Vec3(90,45,30));
        assertEquals(1,Math.sin(r.x()),1e-9); // +Z: opposite authored direction
        assertEquals(Math.PI/4,r.y(),1e-9);assertEquals(Math.PI/6,r.z(),1e-9);
        assertEquals(new Vec3(16,-8,4),BedrockAxes.position(new Vec3(16,8,4)));
    }
    @Test void blockatoWaveMovesRightShoulderAndHandOutward() {
        var offset=BedrockAxes.position(new Vec3(-1.5,.5,0));
        assertEquals(-6.5,-5+offset.x(),1e-9); // right shoulder moves away from the body
        assertEquals(1.5,2+offset.y(),1e-9); // Steve shoulder rises half a pixel
        var rotation=BedrockAxes.rotation(new Vec3(0,0,148));
        assertTrue(-Math.sin(rotation.z())<0); // resting +Y arm rotates toward Java -X (right)
    }
}
