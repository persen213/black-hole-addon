package dev.blackhole.addon;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Spatial raycasting and entity search helpers.
 */
public final class Targeting {
    private Targeting() {}

    public static Vec3 point(ServerPlayer player, double maxDistance) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(maxDistance));

        BlockHitResult blockHit = player.level().clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double reach = blockHit.getType() != HitResult.Type.MISS ? eye.distanceTo(blockHit.getLocation()) : maxDistance;

        AABB box = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player.level(), player, eye, eye.add(look.scale(reach)), box, e -> !e.isSpectator() && e.isPickable());

        if (entityHit != null) return entityHit.getLocation();
        if (blockHit.getType() != HitResult.Type.MISS) return blockHit.getLocation();
        return end;
    }

    public static List<LivingEntity> within(Level level, Vec3 centre, double radius, @Nullable Entity exclude) {
        AABB box = new AABB(centre.x - radius, centre.y - radius, centre.z - radius,
                            centre.x + radius, centre.y + radius, centre.z + radius);
        List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != exclude);
        List<LivingEntity> result = new ArrayList<>();
        double r2 = radius * radius;
        for (LivingEntity e : found) {
            if (centre(e).distanceToSqr(centre) <= r2) result.add(e);
        }
        return result;
    }

    public static Vec3 centre(Entity e) {
        return new Vec3(e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ());
    }

    /** Control resistance factor: bosses resist movement, normal mobs move easily. */
    public static double controlFactor(LivingEntity e) {
        if (e instanceof EnderDragon || e instanceof WitherBoss) return 0.15;
        if (e.getType().getDescriptionId().contains("boss")) return 0.25;
        return 1.0;
    }
}
