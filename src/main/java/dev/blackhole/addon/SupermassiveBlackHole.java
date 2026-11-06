package dev.blackhole.addon;

import io.github.pinchan4273.reacademycraft.api.ActivationOutcome;
import io.github.pinchan4273.reacademycraft.api.StopReason;
import io.github.pinchan4273.reacademycraft.api.mc.AcademySafety;
import io.github.pinchan4273.reacademycraft.api.mc.ActivationContext;
import io.github.pinchan4273.reacademycraft.api.mc.SkillBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Level 5: Supermassive Black Hole (Ultimate).
 * Summons a true cosmic Black Hole complete with an Event Horizon, a swirling Accretion Disk,
 * extreme gravitational suction, and a cataclysmic singularity collapse.
 */
public final class SupermassiveBlackHole implements SkillBehavior {
    private static final int CHARGE_DURATION = 80; // 4 seconds
    private static final double SUCTION_RADIUS = 24.0;

    @Override
    public ActivationOutcome start(ActivationContext c) {
        ServerPlayer player = c.naturalUser();
        if (player == null) return ActivationOutcome.REFUSED_INVALID;

        Vec3 focalPoint = Targeting.point(player, 26.0);
        if (!c.pay(BlackHoleCosts.BLACKHOLE_CP, BlackHoleCosts.BLACKHOLE_OVERHEAT)) {
            return ActivationOutcome.REFUSED_RESOURCE;
        }

        BlackHoleState state = BlackHoleState.of(player);
        state.singularityCenter = focalPoint.add(0, 2.5, 0); // elevate slightly above terrain
        state.singularityTargets.clear();

        ServerLevel level = player.serverLevel();
        BlackHoleFx.sound(level, state.singularityCenter, SoundEvents.RESPAWN_ANCHOR_CHARGE, 2.0f, 0.4f);
        BlackHoleFx.sound(level, state.singularityCenter, SoundEvents.WARDEN_HEARTBEAT, 1.8f, 0.6f);
        BlackHoleFx.burst(level, ParticleTypes.FLASH, state.singularityCenter, 1, 0, 0);

        return ActivationOutcome.STARTED;
    }

    @Override
    public boolean tick(ActivationContext c) {
        ServerPlayer player = c.naturalUser();
        BlackHoleState state = player == null ? null : BlackHoleState.peek(player);
        if (player == null || state == null || state.singularityCenter == null) return false;

        ServerLevel level = player.serverLevel();
        Vec3 center = state.singularityCenter;
        int age = c.age();

        if (age >= CHARGE_DURATION) {
            supernovaCollapse(c, player, level, center);
            return false;
        }

        double progress = age / (double) CHARGE_DURATION;

        // Periodic target verification
        if (age % 5 == 0) {
            state.singularityTargets.clear();
            for (LivingEntity e : Targeting.within(level, center, SUCTION_RADIUS, player)) {
                if (AcademySafety.mayAttack(c.actor(), e, BlackHoleMod.SUPERMASSIVE_BLACK_HOLE)) {
                    state.singularityTargets.add(e.getUUID());
                }
            }
        }

        // Violent gravitational pull toward the core
        for (java.util.UUID id : state.singularityTargets) {
            Entity found = level.getEntity(id);
            if (!(found instanceof LivingEntity target) || !target.isAlive()) continue;

            Vec3 toCenter = center.subtract(Targeting.centre(target));
            double dist = toCenter.length();
            if (dist < 1.0) {
                // Intense tidal forces at the event horizon boundary
                if (age % 4 == 0) {
                    AcademySafety.attack(c.actor(), target, BlackHoleMod.SUPERMASSIVE_BLACK_HOLE, 5.0f);
                }
                continue;
            }

            double strength = Math.min((0.15 + 0.35 * progress) * Targeting.controlFactor(target), dist * 0.25);
            Vec3 pullMotion = target.getDeltaMovement().scale(0.55).add(toCenter.normalize().scale(strength));
            target.setDeltaMovement(pullMotion);
            target.hurtMarked = true;
        }

        // 1. Central Event Horizon Sphere
        double horizonRadius = 1.4 + 0.4 * progress;
        BlackHoleFx.eventHorizonSphere(level, center, horizonRadius, 36);

        // 2. Swirling Accretion Disk (spins faster as time progresses)
        double diskInner = horizonRadius + 0.4;
        double diskOuter = 6.0 + 2.0 * (1.0 - progress * 0.3);
        double rotationAngle = age * (0.35 + 0.45 * progress);
        BlackHoleFx.accretionDisk(level, center, diskInner, diskOuter, rotationAngle, 40);

        // 3. Inward Vacuum Trails
        BlackHoleFx.inwardVortex(level, center, SUCTION_RADIUS * (1.0 - progress * 0.4), 4, progress, age);

        // Ambient sound effects
        if (age % 10 == 0) {
            BlackHoleFx.sound(level, center, SoundEvents.BEACON_AMBIENT, 1.5f, (float)(0.6 + 0.8 * progress));
            BlackHoleFx.sound(level, center, SoundEvents.WARDEN_HEARTBEAT, 1.2f, (float)(0.7 + 0.6 * progress));
        }

        return true;
    }

    private static void supernovaCollapse(ActivationContext c, ServerPlayer player, ServerLevel level, Vec3 center) {
        double radius = SUCTION_RADIUS * 0.85;
        float baseDamage = 45.0f + 25.0f * c.proficiency();

        int hitCount = 0;
        for (LivingEntity target : Targeting.within(level, center, radius, player)) {
            double dist = Targeting.centre(target).distanceTo(center);
            float scaledDamage = (float) (baseDamage * (1.0 - 0.4 * Math.min(1.0, dist / radius)));

            if (!AcademySafety.attack(c.actor(), target, BlackHoleMod.SUPERMASSIVE_BLACK_HOLE, scaledDamage)) continue;

            Vec3 diff = Targeting.centre(target).subtract(center);
            Vec3 blastDir = diff.lengthSqr() < 0.01 ? new Vec3(level.random.nextDouble() - 0.5, 0.6, level.random.nextDouble() - 0.5).normalize() : diff.normalize();

            double force = 2.2 * Targeting.controlFactor(target);
            target.setDeltaMovement(blastDir.x * force, Math.max(0.6, blastDir.y * force + 0.3), blastDir.z * force);
            target.hurtMarked = true;
            hitCount++;
        }

        // Cataclysmic visual burst
        BlackHoleFx.burst(level, ParticleTypes.FLASH, center, 2, 0, 0);
        BlackHoleFx.burst(level, ParticleTypes.EXPLOSION_EMITTER, center, 5, 2.0, 0);
        BlackHoleFx.burst(level, ParticleTypes.SONIC_BOOM, center, 1, 0, 0);

        BlackHoleFx.burst(level, BlackHoleFx.BLACK, center, 150, radius * 0.5, 0.4);
        BlackHoleFx.burst(level, BlackHoleFx.PURPLE, center, 120, radius * 0.5, 0.35);
        BlackHoleFx.burst(level, BlackHoleFx.ORANGE, center, 100, radius * 0.4, 0.3);
        BlackHoleFx.burst(level, BlackHoleFx.WHITE, center, 80, radius * 0.3, 0.4);

        // 5 Expanding Shockwave Rings
        for (int i = 1; i <= 5; i++) {
            final double r = radius * i / 5.0;
            final int points = 24 + i * 16;
            BlackHoleFx.ring(level, (i % 2 == 0) ? BlackHoleFx.CYAN : BlackHoleFx.ORANGE, center, r, points);
        }

        BlackHoleFx.sound(level, center, SoundEvents.GENERIC_EXPLODE, 2.5f, 0.5f);
        BlackHoleFx.sound(level, center, SoundEvents.WARDEN_SONIC_BOOM, 2.0f, 0.7f);

        if (hitCount > 0) {
            c.addProficiency(0.03f * Math.min(hitCount, 5));
        }
    }

    @Override
    public void stop(ActivationContext c, StopReason reason) {
        ServerPlayer player = c.naturalUser();
        BlackHoleState state = player == null ? null : BlackHoleState.peek(player);
        if (state != null) {
            state.singularityCenter = null;
            state.singularityTargets.clear();
        }
        c.setCooldown(120 * 20); // 2 minutes cooldown
    }
}
