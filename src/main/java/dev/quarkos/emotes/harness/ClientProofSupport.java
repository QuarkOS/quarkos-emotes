package dev.quarkos.emotes.harness;

import dev.quarkos.emotes.ClientProofHooks;
import net.minecraft.client.Minecraft;
public final class ClientProofSupport implements ClientProofHooks.Support {
    public void tick(Minecraft mc) {ClientHarness.tick(mc);MultiplayerHarness.tick(mc);}
    public void record(int entity,boolean changed) {ClientHarness.record(entity,changed);}
    public void recordHand(boolean changed) {ClientHarness.recordHand(changed);}
    public void rotation(int entity,float x,float y) {ClientHarness.rotation(entity,x,y);}
    public void prop(int entity,String bone) {ClientHarness.prop(entity,bone);}
    public void propPass(int entity) {ClientHarness.propPass(entity);}
    public String model() {return ClientHarness.model;}
    public void capture() {ClientHarness.capture();}
    public float distance() {return ClientHarness.portraitDistance;}
    public float drop() {return ClientHarness.portraitDrop;}
    public float orbit() {return ClientHarness.portraitOrbit;}
    public float forward() {return ClientHarness.portraitForward;}
    public float pitch() {return ClientHarness.portraitPitch;}
}
