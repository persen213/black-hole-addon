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
 * Creates a focused gravity focal point that violently pulls all nearby entities toward it.
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
        double reach = 20.0 + 8.0 * c.proficiency();
        Vec3 focalPoint = Targeting.point(player, reach);
        double pullRadius = 7.0;

        int affected = 0;
        for (LivingEntity target : Targeting.within(level, focalPoint, pullRadius, player)) {
            if (!AcademySafety.mayAttack(c.actor(), target, BlackHoleMod.GRAVITY_PULL)) continue;

            Vec3 toCenter = focalPoint.subtract(Targeting.centre(target));
            double dist = toCenter.length();
            if (dist < 0.5) continue;

            double pullStrength = (0.7 + 0.5 * c.proficiency()) * Targeting.controlFactor(target);
            Vec3 motion = toCenter.normalize().scale(pullStrength).add(0, 0.2, 0);
            target.setDeltaMovement(motion);
            target.hurtMarked = true;

            // Small gravitational crush damage
            float damage = 3.0f + 3.0f * c.proficiency();
            AcademySafety.attack(c.actor(), target, BlackHoleMod.GRAVITY_PULL, damage);
            affected++;
        }

        // Visual and sound effects
        BlackHoleFx.sound(level, focalPoint, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.2f, 1.6f);
        BlackHoleFx.sound(level, focalPoint, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 0.5f);
        BlackHoleFx.burst(level, ParticleTypes.REVERSE_PORTAL, focalPoint.add(0, 0.5, 0), 25, 1.2, 0.1);
        BlackHoleFx.burst(level, BlackHoleFx.BLACK, focalPoint.add(0, 0.5, 0), 30, 0.8, 0.05);
        BlackHoleFx.ring(level, BlackHoleFx.PURPLE, focalPoint.add(0, 0.1, 0), pullRadius * 0.7, 32);

        if (affected > 0) {
            c.addProficiency(0.02f * Math.min(affected, 4));
        }

        c.setCooldown(30); // 1.5 seconds cooldown
        return ActivationOutcome.STARTED;
    }
}
