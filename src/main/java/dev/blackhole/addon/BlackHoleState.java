package dev.blackhole.addon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Per-player runtime state for toggle/charging skills.
 */
public final class BlackHoleState {
    private static final Map<UUID, BlackHoleState> STATES = new HashMap<>();

    // Event Horizon field active
    public boolean eventHorizon = false;

    // Micro Singularity active state
    public Vec3 microPos = null;
    public Vec3 microMotion = null;

    // Supermassive Black Hole active state
    public Vec3 singularityCenter = null;
    public final Set<UUID> singularityTargets = new HashSet<>();

    public static synchronized BlackHoleState of(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new BlackHoleState());
    }

    @Nullable
    public static synchronized BlackHoleState peek(ServerPlayer player) {
        return STATES.get(player.getUUID());
    }

    public static synchronized void forget(UUID id) {
        STATES.remove(id);
    }

    public static synchronized void clearAll() {
        STATES.clear();
    }
}
