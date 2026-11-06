package dev.blackhole.addon;

/**
 * CP (Computation Points) and Overheat cost constants for Black Hole abilities.
 */
public final class BlackHoleCosts {
    private BlackHoleCosts() {}

    // Level 1: Gravitational Pull
    public static final float PULL_CP = 35f;
    public static final float PULL_OVERHEAT = 10f;

    // Level 2: Event Horizon Field (Toggle)
    public static final float EVENT_HORIZON_INIT_CP = 50f;
    public static final float EVENT_HORIZON_INIT_OVERHEAT = 14f;
    public static final float EVENT_HORIZON_TICK_CP = 3.5f;

    // Level 3: Gravitational Repulsion
    public static final float REPULSION_CP = 65f;
    public static final float REPULSION_OVERHEAT = 20f;

    // Level 4: Micro Singularity (Toggle flight)
    public static final float MICRO_INIT_CP = 110f;
    public static final float MICRO_INIT_OVERHEAT = 30f;
    public static final float MICRO_TICK_CP = 4f;

    // Level 5: Supermassive Black Hole (Ultimate)
    public static final float BLACKHOLE_CP = 280f;
    public static final float BLACKHOLE_OVERHEAT = 75f;
}
