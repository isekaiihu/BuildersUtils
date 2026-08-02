package com.isekai.buildersutils.mining;

import net.minecraft.client.MinecraftClient;

/**
 * Owns the single active {@link MiningTask} and ticks it each client tick.
 */
public class MiningManager {
    private static final MiningManager INSTANCE = new MiningManager();

    public static MiningManager getInstance() {
        return INSTANCE;
    }

    private MiningTask task;

    private MiningManager() {
    }

    public void start(MiningTask task) {
        stop(); // cancel any previous task and release its keys
        this.task = task;
    }

    public boolean isRunning() {
        return task != null;
    }

    public void stop() {
        if (task != null) {
            task.release();
            task = null;
        }
    }

    public void tick(MinecraftClient client) {
        if (task == null) {
            return;
        }
        if (client.player == null || client.world == null) {
            stop();
            return;
        }
        boolean done = task.tick(client);
        if (done) {
            task.release();
            task = null;
        }
    }
}
