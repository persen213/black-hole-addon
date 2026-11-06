package dev.blackhole.addon;

import io.github.pinchan4273.reacademycraft.api.ActivationOutcome;
import io.github.pinchan4273.reacademycraft.api.StopReason;
import io.github.pinchan4273.reacademycraft.api.mc.AcademySafety;
import io.github.pinchan4273.reacademycraft.api.mc.ActivationContext;
import io.github.pinchan4273.reacademycraft.api.mc.SkillBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Level 4: Micro Singularity (Toggle/Channel).
 * Launches a travelling miniature black hole forward that pulls in mobs as it glides,
 * then detonates upon block collision or after reaching maximum lifespan.
 */
public final class MicroSingularity implements SkillBehavior {
    private static final int MAX_AGE = 70;
    private static final double SPEED = 0.45;

    @Override
    public ActivationOutcome start(ActivationContext c) {
        ServerPlayer player = c.naturalUser();
        if (player == null) return ActivationOutcome.REFUSED_INVALID;

        if (!c.pay(BlackHoleCosts.MICRO_INIT_CP, BlackHoleCosts.MICRO_INIT_OVERHEAT)) {
            return ActivationOutcome.REFUSED_RESOURCE;
        }

        BlackHoleState state = BlackHoleState.of(player);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);
        state.microPos = eye.add(look.scale(1.5));
        state.microMotion = look.scale(SPEED);

        ServerLevel level = player.serverLevel();
        BlackHoleFx.sound(level, state.microPos, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, 1.8f, 0.4f);
        BlackHoleFx.sound(level, state.microPos, SoundEvents.ENDER_DRAGON_FLAP, 1.5f, 0.4f);

        return ActivationOutcome.STARTED;
    }

    @Override
    public boolean tick(ActivationContext c) {
        ServerPlayer player = c.naturalUser();
        BlackHoleState state = player == null ? null : BlackHoleState.peek(player);
        if (player == null || state == null || state.microPos == null) return false;

        ServerLevel level = player.serverLevel();
        int age = c.age();

        if (age >= MAX_AGE) {
            detonate(c, player, level, state.microPos);
            return false;
        }

        Vec3 currentPos = state.microPos;
        Vec3 nextPos = currentPos.add(state.microMotion);

        BlockPos blockPos = BlockPos.containing(nextPos.x, nextPos.y, nextPos.z);
        if (level.getBlockState(blockPos).isSolid()) {
            detonate(c, player, level, currentPos);
            return false;
        }
        state.microPos = nextPos;

        // Visuals of the traveling black hole
        BlackHoleFx.burst(level, ParticleTypes.SQUID_INK, nextPos, 16, 0.35, 0.02);
        BlackHoleFx.burst(level, ParticleTypes.REVERSE_PORTAL, nextPos, 8, 0.4, 0.05);
        BlackHoleFx.burst(level, ParticleTypes.DRAGON_BREATH, nextPos, 4, 0.25, 0.01);
        BlackHoleFx.ring(level, ParticleTypes.FLAME, nextPos, 1.1, 16);
        BlackHoleFx.burst(level, ParticleTypes.PORTAL, nextPos, 10, 1.0, 0.2);

        if (age % 5 == 0) {
            BlackHoleFx.sound(level, nextPos, SoundEvents.BEACON_AMBIENT, 1.2f, 1.6f);
        }

        // Pull nearby entities
        double suctionRadius = 7.0;
        for (LivingEntity target : Targeting.within(level, nextPos, suctionRadius, player)) {
            if (!AcademySafety.mayAttack(c.actor(), target, BlackHoleMod.MICRO_SINGULARITY)) continue;

            Vec3 toCenter = nextPos.subtract(Targeting.centre(target));
            double dist = toCenter.length();
            if (dist < 0.6) continue;

            double pull = Math.min(0.3 * Targeting.controlFactor(target), dist * 0.18);
            target.setDeltaMovement(target.getDeltaMovement().scale(0.65).add(toCenter.normalize().scale(pull)));
            target.hurtMarked = true;

            if (age % 6 == 0) {
                AcademySafety.attack(c.actor(), target, BlackHoleMod.MICRO_SINGULARITY, 3.0f);
            }
        }

        return true;
    }

    private static void detonate(ActivationContext c, ServerPlayer player, ServerLevel level, Vec3 center) {
        double radius = 8.0 + 2.0 * c.proficiency();
        float baseDamage = 25.0f + 12.0f * c.proficiency();

        int hits = 0;
        for (LivingEntity target : Targeting.within(level, center, radius, player)) {
            if (!AcademySafety.attack(c.actor(), target, BlackHoleMod.MICRO_SINGULARITY, baseDamage)) continue;

            Vec3 away = Targeting.centre(target).subtract(center);
            Vec3 dir = away.lengthSqr() < 0.01 ? new Vec3(0, 0.6, 0) : away.normalize();
            target.setDeltaMovement(dir.scale(1.4));
            target.hurtMarked = true;
            hits++;
        }

        // Massive collapse FX
        BlackHoleFx.sound(level, center, SoundEvents.GENERIC_EXPLODE, 2.0f, 0.7f);
        BlackHoleFx.sound(level, center, SoundEvents.WARDEN_SONIC_BOOM, 1.8f, 1.1f);
        BlackHoleFx.burst(level, ParticleTypes.FLASH, center, 2, 0, 0);
        BlackHoleFx.burst(level, ParticleTypes.EXPLOSION_EMITTER, center, 3, 1.2, 0);
        BlackHoleFx.burst(level, ParticleTypes.SONIC_BOOM, center, 1, 0, 0);
        BlackHoleFx.burst(level, ParticleTypes.DRAGON_BREATH, center, 80, radius * 0.5, 0.25);
        BlackHoleFx.burst(level, ParticleTypes.SQUID_INK, center, 60, radius * 0.4, 0.15);
        BlackHoleFx.ring(level, ParticleTypes.SOUL_FIRE_FLAME, center, radius, 36);

        if (hits > 0) {
            c.addProficiency(0.02f * Math.min(hits, 4));
        }
    }

    @Override
    public void stop(ActivationContext c, StopReason reason) {
        ServerPlayer player = c.naturalUser();
        BlackHoleState state = player == null ? null : BlackHoleState.peek(player);
        if (state != null) {
            state.microPos = null;
            state.microMotion = null;
        }
        c.setCooldown(70);
    }
}
