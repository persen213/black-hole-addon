package dev.blackhole.addon;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Visual and sound FX helpers specialized for cosmic singularity / black hole effects.
 */
public final class BlackHoleFx {
    private BlackHoleFx() {}

    // Custom dust color palettes
    public static final DustParticleOptions BLACK = new DustParticleOptions(new Vector3f(0.01f, 0.01f, 0.03f), 1.6f);
    public static final DustParticleOptions PURPLE = new DustParticleOptions(new Vector3f(0.45f, 0.08f, 0.90f), 1.5f);
    public static final DustParticleOptions ORANGE = new DustParticleOptions(new Vector3f(1.0f, 0.42f, 0.04f), 1.4f);
    public static final DustParticleOptions CYAN = new DustParticleOptions(new Vector3f(0.12f, 0.88f, 1.0f), 1.3f);
    public static final DustParticleOptions WHITE = new DustParticleOptions(new Vector3f(0.96f, 0.96f, 1.0f), 1.3f);

    public static void burst(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double spread, double speed) {
        level.sendParticles(particle, at.x, at.y, at.z, count, spread, spread, spread, speed);
    }

    /** A flat circular ring of particles. */
    public static void ring(ServerLevel level, ParticleOptions particle, Vec3 centre, double radius, int points) {
        if (radius <= 0.01) return;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            level.sendParticles(particle, centre.x + Math.cos(angle) * radius, centre.y, centre.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
    }

    /** A dense sphere of particles representing the pitch-black Event Horizon. */
    public static void eventHorizonSphere(ServerLevel level, Vec3 centre, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double u = level.random.nextDouble();
            double v = level.random.nextDouble();
            double theta = u * 2.0 * Math.PI;
            double phi = Math.acos(2.0 * v - 1.0);
            double r = radius * Math.cbrt(level.random.nextDouble()); // fills sphere interior and surface
            double x = r * Math.sin(phi) * Math.cos(theta);
            double y = r * Math.sin(phi) * Math.sin(theta);
            double z = r * Math.cos(phi);
            level.sendParticles(BLACK, centre.x + x, centre.y + y, centre.z + z, 1, 0, 0, 0, 0);
        }
        // Surface highlight with reverse portal
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, centre.x, centre.y, centre.z, Math.max(1, count / 8), radius * 0.5, radius * 0.5, radius * 0.5, 0.02);
    }

    /** Rotating spiral Accretion Disk around the singularity's equatorial plane. */
    public static void accretionDisk(ServerLevel level, Vec3 centre, double innerRadius, double outerRadius, double rotationAngle, int density) {
        for (int i = 0; i < density; i++) {
            double frac = i / (double) density;
            double r = innerRadius + frac * (outerRadius - innerRadius);
            // Spiral distribution: angle rotates with radius and time
            double angle = rotationAngle + frac * Math.PI * 4.0;
            double x = Math.cos(angle) * r;
            double z = Math.sin(angle) * r;
            // Slight tilt and wobble
            double y = Math.sin(angle * 2.0) * 0.15;
            ParticleOptions p = (i % 2 == 0) ? ORANGE : ((i % 4 == 1) ? PURPLE : ParticleTypes.LAVA);
            level.sendParticles(p, centre.x + x, centre.y + y, centre.z + z, 1, 0.05, 0.02, 0.05, 0.01);
        }
    }

    /** Inward spiraling particle trails representing extreme gravitational suction. */
    public static void inwardVortex(ServerLevel level, Vec3 centre, double outerRadius, int arms, double progress, int age) {
        for (int arm = 0; arm < arms; arm++) {
            double baseAngle = (arm * Math.PI * 2.0 / arms) + (age * 0.25);
            for (int step = 0; step < 5; step++) {
                double r = outerRadius * (1.0 - (step / 5.0) * 0.7);
                double a = baseAngle + (step * 0.4);
                double x = Math.cos(a) * r;
                double z = Math.sin(a) * r;
                double y = (step * 0.3) - 0.5;
                ParticleOptions p = (arm % 2 == 0) ? PURPLE : CYAN;
                level.sendParticles(p, centre.x + x, centre.y + y, centre.z + z, 1, 0.02, 0.02, 0.02, 0.02);
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
