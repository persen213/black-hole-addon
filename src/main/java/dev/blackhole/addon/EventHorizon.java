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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Level 2: Event Horizon Field (Toggle).
 * Generates an oppressive local gravitational field around the user.
 * Incoming projectiles are forced down; nearby enemies are weighed down and continuously crushed.
 */
public final class EventHorizon implements SkillBehavior {
    private static final double RADIUS = 7.5;

    @Override
    public ActivationOutcome start(ActivationContext c) {
        ServerPlayer player = c.naturalUser();
        if (player == null) return ActivationOutcome.REFUSED_INVALID;

        if (!c.pay(BlackHoleCosts.EVENT_HORIZON_INIT_CP, BlackHoleCosts.EVENT_HORIZON_INIT_OVERHEAT)) {
            return ActivationOutcome.REFUSED_RESOURCE;
        }

        BlackHoleState state = BlackHoleState.of(player);
        state.eventHorizon = true;

        ServerLevel level = player.serverLevel();
        BlackHoleFx.sound(level, player.position(), SoundEvents.BEACON_ACTIVATE, 1.4f, 0.6f);
        BlackHoleFx.sound(level, player.position(), SoundEvents.WARDEN_HEARTBEAT, 1.5f, 0.7f);
        BlackHoleFx.burst(level, ParticleTypes.FLASH, player.position().add(0, 1, 0), 1, 0, 0);

        return ActivationOutcome.STARTED;
    }

    @Override
    public boolean tick(ActivationContext c) {
        ServerPlayer player = c.naturalUser();
        BlackHoleState state = player == null ? null : BlackHoleState.peek(player);
        if (player == null || state == null || !state.eventHorizon) return false;

        // Drain CP every tick
        if (!c.pay(BlackHoleCosts.EVENT_HORIZON_TICK_CP, 0.2f)) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        Vec3 pos = player.position();
        int age = c.age();

        // Pulsing gravitational field rings
        if (age % 4 == 0) {
            double r = (age % 24) / 24.0 * RADIUS;
            BlackHoleFx.ring(level, BlackHoleFx.BLACK, pos.add(0, 0.15, 0), r, 28);
        }
        if (age % 8 == 0) {
            BlackHoleFx.ring(level, BlackHoleFx.PURPLE, pos.add(0, 0.2, 0), RADIUS, 36);
        }

        // Heavy gravity effect on enemies
        if (age % 10 == 0) {
            for (LivingEntity target : Targeting.within(level, pos, RADIUS, player)) {
                if (!AcademySafety.mayAttack(c.actor(), target, BlackHoleMod.EVENT_HORIZON)) continue;

                // Heavy downward force & slowness
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2, false, false));
                target.setDeltaMovement(target.getDeltaMovement().multiply(0.4, 0.2, 0.4));
                target.hurtMarked = true;

                // Gravitational crush damage
                float damage = 2.0f + 1.5f * c.proficiency();
                AcademySafety.attack(c.actor(), target, BlackHoleMod.EVENT_HORIZON, damage);

                BlackHoleFx.burst(level, BlackHoleFx.BLACK, target.position().add(0, 0.5, 0), 4, 0.2, 0.05);
            }
        }

        return true;
    }

    @Override
    public void stop(ActivationContext c, StopReason reason) {
        ServerPlayer player = c.naturalUser();
        BlackHoleState state = player == null ? null : BlackHoleState.peek(player);
        if (state != null) {
            state.eventHorizon = false;
        }
        if (player != null) {
            ServerLevel level = player.serverLevel();
            BlackHoleFx.sound(level, player.position(), SoundEvents.BEACON_DEACTIVATE, 1.2f, 0.6f);
            BlackHoleFx.burst(level, BlackHoleFx.PURPLE, player.position().add(0, 1, 0), 20, 1.0, 0.05);
        }
        c.setCooldown(40);
    }
}
