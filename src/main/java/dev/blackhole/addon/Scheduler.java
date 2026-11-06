package dev.blackhole.addon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.server.MinecraftServer;

/**
 * Server-side tick scheduler for timed delays.
 */
public final class Scheduler {
    private Scheduler() {}

    private record Entry(long runAtTick, Runnable action) {}
    private static final List<Entry> TASKS = new ArrayList<>();

    public static synchronized void later(MinecraftServer server, int delayTicks, Runnable action) {
        long runAt = server.getTickCount() + Math.max(1, delayTicks);
        TASKS.add(new Entry(runAt, action));
    }

    public static synchronized void run(MinecraftServer server) {
        if (TASKS.isEmpty()) return;
        long now = server.getTickCount();
        Iterator<Entry> it = TASKS.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (e.runAtTick <= now) {
                it.remove();
                try {
                    e.action.run();
                } catch (Throwable t) {
                    BlackHoleMod.LOGGER.error("Error executing scheduled task", t);
                }
            }
        }
    }

    public static synchronized void clear() {
        TASKS.clear();
    }
}
