package dev.blackhole.addon;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Visual and sound FX helpers using high-visibility Minecraft particles.
 */
public final class BlackHoleFx {
    private BlackHoleFx() {}

    /**
     * Spawns a cluster of particles using the standard ServerLevel particle pipeline.
     */
    public static void burst(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double spread, double speed) {
        level.sendParticles(particle, at.x, at.y, at.z, count, spread, spread, spread, speed);
    }

    /**
     * Circular ring of particles.
     */
    public static void ring(ServerLevel level, ParticleOptions particle, Vec3 centre, double radius, int points) {
        if (radius <= 0.05) return;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            level.sendParticles(particle, centre.x + Math.cos(angle) * radius, centre.y, centre.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Pitch-black Event Horizon core formed with dense ink clouds and dark void motes.
     */
    public static void eventHorizonSphere(ServerLevel level, Vec3 centre, double radius, int count) {
        // Dense black smoke core
        level.sendParticles(ParticleTypes.SQUID_INK, centre.x, centre.y, centre.z, count, radius * 0.4, radius * 0.4, radius * 0.4, 0.02);
        // Purple dimensional suction motes
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, centre.x, centre.y, centre.z, Math.max(5, count / 3), radius * 0.5, radius * 0.5, radius * 0.5, 0.05);
        // Deep purple cosmic mist
        level.sendParticles(ParticleTypes.DRAGON_BREATH, centre.x, centre.y, centre.z, Math.max(2, count / 6), radius * 0.3, radius * 0.3, radius * 0.3, 0.01);
    }

    /**
     * Glowing Accretion Disk around the black hole equator: fiery orange plasma & purple void fire.
     */
    public static void accretionDisk(ServerLevel level, Vec3 centre, double innerRadius, double outerRadius, double rotationAngle, int density) {
        for (int i = 0; i < density; i++) {
            double frac = i / (double) density;
            double r = innerRadius + frac * (outerRadius - innerRadius);
            double angle = rotationAngle + frac * Math.PI * 3.5;
            double x = Math.cos(angle) * r;
            double z = Math.sin(angle) * r;
            double y = Math.sin(angle * 2.0) * 0.2;

            ParticleOptions p = (i % 3 == 0) ? ParticleTypes.FLAME : ((i % 3 == 1) ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.LAVA);
            level.sendParticles(p, centre.x + x, centre.y + y, centre.z + z, 1, 0.02, 0.02, 0.02, 0.01);
        }
    }

    /**
     * Inward vacuum trails: streams of particles getting sucked into the singularity.
     */
    public static void inwardVortex(ServerLevel level, Vec3 centre, double outerRadius, int arms, int age) {
        for (int arm = 0; arm < arms; arm++) {
            double baseAngle = (arm * Math.PI * 2.0 / arms) + (age * 0.3);
            for (int step = 1; step <= 4; step++) {
                double r = outerRadius * (1.0 - (step / 5.0) * 0.7);
                double a = baseAngle + (step * 0.5);
                double x = Math.cos(a) * r;
                double z = Math.sin(a) * r;
                double y = (step * 0.25) - 0.3;

                ParticleOptions p = (arm % 2 == 0) ? ParticleTypes.PORTAL : ParticleTypes.DRAGON_BREATH;
                level.sendParticles(p, centre.x + x, centre.y + y, centre.z + z, 1, 0.05, 0.05, 0.05, 0.02);
            }
        }
    }

    public static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void hint(ServerPlayer player, String text) {
        player.displayClientMessage(Component.literal(text), true);
    }
}
