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
 * Level 3: Gravitational Repulsion (Instant).
 * Inverts local gravity instantly into a massive outward shockwave, blasting away all attackers.
 */
public final class GravityRepulsion implements SkillBehavior {

    @Override
    public ActivationOutcome start(ActivationContext c) {
        ServerPlayer player = c.naturalUser();
        if (player == null) return ActivationOutcome.REFUSED_INVALID;

        if (!c.pay(BlackHoleCosts.REPULSION_CP, BlackHoleCosts.REPULSION_OVERHEAT)) {
            return ActivationOutcome.REFUSED_RESOURCE;
        }

        ServerLevel level = player.serverLevel();
        Vec3 origin = player.position().add(0, 0.5, 0);
        double blastRadius = 9.0 + 3.0 * c.proficiency();

        int hitCount = 0;
        for (LivingEntity target : Targeting.within(level, origin, blastRadius, player)) {
            if (!AcademySafety.mayAttack(c.actor(), target, BlackHoleMod.GRAVITY_REPULSION)) continue;

            Vec3 diff = Targeting.centre(target).subtract(origin);
            Vec3 dir = diff.lengthSqr() < 0.01 ? new Vec3(level.random.nextDouble() - 0.5, 0.5, level.random.nextDouble() - 0.5).normalize() : diff.normalize();

            double force = (1.4 + 0.8 * c.proficiency()) * Targeting.controlFactor(target);
            Vec3 impulse = new Vec3(dir.x * force, Math.max(0.65, dir.y * force + 0.45), dir.z * force);
            target.setDeltaMovement(impulse);
            target.hurtMarked = true;

            float damage = 12.0f + 8.0f * c.proficiency();
            AcademySafety.attack(c.actor(), target, BlackHoleMod.GRAVITY_REPULSION, damage);
            hitCount++;
        }

        // Stunning visual effects: expanding rings & sonic boom
        BlackHoleFx.sound(level, origin, SoundEvents.WARDEN_SONIC_BOOM, 1.8f, 0.9f);
        BlackHoleFx.sound(level, origin, SoundEvents.GENERIC_EXPLODE, 1.4f, 1.2f);
        BlackHoleFx.burst(level, ParticleTypes.SONIC_BOOM, origin, 1, 0, 0);
        BlackHoleFx.burst(level, ParticleTypes.FLASH, origin, 1, 0, 0);

        for (int i = 1; i <= 4; i++) {
            final double r = blastRadius * i / 4.0;
            final int points = 24 + i * 12;
            BlackHoleFx.ring(level, i % 2 == 0 ? BlackHoleFx.CYAN : BlackHoleFx.WHITE, origin, r, points);
        }
        BlackHoleFx.burst(level, BlackHoleFx.PURPLE, origin, 40, blastRadius * 0.4, 0.25);

        if (hitCount > 0) {
            c.addProficiency(0.025f * Math.min(hitCount, 4));
        }

        c.setCooldown(60); // 3 seconds cooldown
        return ActivationOutcome.STARTED;
    }
}
