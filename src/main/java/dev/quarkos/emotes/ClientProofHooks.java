package dev.quarkos.emotes;

import net.minecraft.client.Minecraft;
/** Runtime hooks; the opt-in implementation is present only on development run classpaths. */
public final class ClientProofHooks {
    public interface Support {
        void tick(Minecraft mc);
        void record(int entity,boolean changed);
        void recordHand(boolean changed);
        void rotation(int entity,float x,float y);
        String model();
        void capture();
        float distance();float drop();float orbit();float forward();
    }
    private static Support support;
    public static boolean visual() { return Boolean.getBoolean("quarkos_emotes.harness"); }
    public static boolean multiplayer() { return !System.getProperty("quarkos_emotes.joinServer","").isBlank(); }
    private static boolean enabled() { return visual()||multiplayer(); }
    private static Support support() {
        if(support==null)try {
            support=(Support)Class.forName("dev.quarkos.emotes.harness.ClientProofSupport").getConstructor().newInstance();
        } catch(ReflectiveOperationException e) {throw new IllegalStateException("Proof support requires a development run classpath",e);}
        return support;
    }
    public static void tick(Minecraft mc) {if(enabled())support().tick(mc);}
    public static void record(int entity,boolean changed) {if(enabled())support().record(entity,changed);}
    public static void recordHand(boolean changed) {if(enabled())support().recordHand(changed);}
    public static void rotation(int entity,float x,float y) {if(visual())support().rotation(entity,x,y);}
    public static String model() {return visual()?support().model():"";}
    public static void capture() {if(enabled())support().capture();}
    public static float distance() {return visual()?support().distance():4;}
    public static float drop() {return visual()?support().drop():0;}
    public static float orbit() {return visual()?support().orbit():0;}
    public static float forward() {return visual()?support().forward():0;}
}
