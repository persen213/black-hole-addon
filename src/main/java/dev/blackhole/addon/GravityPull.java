package dev.blackhole.addon;

import io.github.pinchan4273.reacademycraft.api.ActivationOutcome;
import io.github.pinchan4273.reacademycraft.api.mc.AcademySafety;
import io.github.pinchan4273.reacademycraft.api.mc.ActivationContext;
import io.github.pinchan4273.reacademycraft.api.mc.SkillBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Level 1: Gravitational Pull (Instant).
 * Creates an instant gravity focal point that violently pulls all nearby entities toward it.
 */
public final class GravityPull implements SkillBehavior {

    @Override
    public ActivationOutcome start(ActivationContext c) {
        ServerPlayer player = c.naturalUser();
        if (player == null) return ActivationOutcome.REFUSED_INVALID;

        if (!c.pay(BlackHoleCosts.PULL_CP, BlackHoleCosts.PULL_OVERHEAT)) {
            return ActivationOutcome.REFUSED_RESOURCE;
        }

        ServerLevel level = player.serverLevel();
        double reach = 22.0 + 8.0 * c.proficiency();
        Vec3 focalPoint = Targeting.point(player, reach);
        double pullRadius = 8.0;

        int affected = 0;
        for (LivingEntity target : Targeting.within(level, focalPoint, pullRadius, player)) {
            if (!AcademySafety.mayAttack(c.actor(), target, BlackHoleMod.GRAVITY_PULL)) continue;

            Vec3 toCenter = focalPoint.subtract(Targeting.centre(target));
            double dist = toCenter.length();
            if (dist < 0.5) continue;

            double pullStrength = (0.8 + 0.6 * c.proficiency()) * Targeting.controlFactor(target);
            Vec3 motion = toCenter.normalize().scale(pullStrength).add(0, 0.25, 0);
            target.setDeltaMovement(motion);
            target.hurtMarked = true;

            float damage = 3.5f + 3.5f * c.proficiency();
            AcademySafety.attack(c.actor(), target, BlackHoleMod.GRAVITY_PULL, damage);
            affected++;
        }

        // High-visibility cosmic visuals:
        BlackHoleFx.sound(level, focalPoint, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.5f, 1.5f);
        BlackHoleFx.sound(level, focalPoint, SoundEvents.ENDERMAN_TELEPORT, 1.2f, 0.4f);

        // Dense black ink + purple dragon breath + portal suction
        BlackHoleFx.burst(level, ParticleTypes.SQUID_INK, focalPoint.add(0, 0.8, 0), 30, 0.6, 0.05);
        BlackHoleFx.burst(level, ParticleTypes.DRAGON_BREATH, focalPoint.add(0, 0.8, 0), 25, 0.8, 0.04);
        BlackHoleFx.burst(level, ParticleTypes.PORTAL, focalPoint.add(0, 0.8, 0), 60, 2.0, 0.5);
        BlackHoleFx.ring(level, ParticleTypes.SOUL_FIRE_FLAME, focalPoint.add(0, 0.2, 0), pullRadius * 0.7, 32);

        if (affected > 0) {
            c.addProficiency(0.02f * Math.min(affected, 4));
        }

        c.setCooldown(25);
        return ActivationOutcome.STARTED;
    }
}
