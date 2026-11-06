package dev.blackhole.addon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Event subscriber for tick management and passive gravitational field effects.
 */
@Mod.EventBusSubscriber(modid = BlackHoleMod.MOD_ID)
public final class BlackHoleEvents {
    private BlackHoleEvents() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) Scheduler.run(server);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        BlackHoleState state = BlackHoleState.peek(player);
        if (state == null || !state.eventHorizon) return;

        // Gravitational projectile deflection: drag incoming projectiles into the floor
        ServerLevel level = player.serverLevel();
        Vec3 pos = player.position();
        double radius = 7.0;
        AABB box = new AABB(pos.x - radius, pos.y - radius, pos.z - radius,
                            pos.x + radius, pos.y + radius, pos.z + radius);

        for (Projectile proj : level.getEntitiesOfClass(Projectile.class, box, p -> p.isAlive() && p.getOwner() != player)) {
            Vec3 m = proj.getDeltaMovement();
            // Force downwards towards ground
            proj.setDeltaMovement(m.x * 0.4, Math.min(m.y - 0.35, -0.75), m.z * 0.4);
            proj.hasImpulse = true;
            if (level.random.nextInt(3) == 0) {
                BlackHoleFx.burst(level, BlackHoleFx.BLACK, proj.position(), 2, 0.1, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        BlackHoleState.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        BlackHoleState.clearAll();
        Scheduler.clear();
    }
}
