package dev.quarkos.emotes.network;

/** Late-join playback policy that does not depend on a client world. */
public final class RemoteApply {
    /** How long a remote pose may wait for its player entity before it is dropped. */
    public static final int ENTITY_GRACE_TICKS = 400;
    private RemoteApply() {}

    public static boolean keepWhileEntityMissing(int ticksWaiting) {
        return ticksWaiting <= ENTITY_GRACE_TICKS;
    }

    /**
     * A respawn, dimension change, or reconnect replaces the player entity.
     * The previous pose belongs to the old entity and must not stick to the new one.
     * The first time an entity is seen there is nothing to drop.
     */
    public static boolean dropOnEntityReplacement(boolean hadEntity, boolean sameObject, boolean alive) {
        return !alive || (hadEntity && !sameObject);
    }

    /**
     * Drop remote cues only when leaving a world that was already attached.
     * The first attach is that world, and a new local player object in the
     * same world (login/respawn bookkeeping) is not a new session.
     */
    public static boolean dropRemoteCues(boolean hadLevel, boolean levelChanged) {
        return hadLevel && levelChanged;
    }

    /**
     * A cue can arrive before the client has a connection object. Keep it across
     * the null to first-connection transition. A later connection change is a new session.
     */
    public static boolean retainCueOnConnectionChange(boolean hadConnection) {
        return !hadConnection;
    }
}
