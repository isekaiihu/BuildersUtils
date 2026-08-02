package com.isekai.buildersutils.util;

import net.minecraft.util.math.Vec3d;

/**
 * Helpers for aiming the player smoothly at a world point, like a human turning.
 */
public final class RotationUtil {
    private RotationUtil() {
    }

    /**
     * Computes the {yaw, pitch} needed to look from {@code from} toward {@code to}.
     */
    public static float[] getRotations(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, distXZ)));
        return new float[]{wrapDegrees(yaw), pitch};
    }

    /** Wraps an angle into the range [-180, 180). */
    public static float wrapDegrees(float deg) {
        deg %= 360.0f;
        if (deg >= 180.0f) {
            deg -= 360.0f;
        }
        if (deg < -180.0f) {
            deg += 360.0f;
        }
        return deg;
    }

    /**
     * Steps {@code current} toward {@code target} by at most {@code maxStep} degrees,
     * taking the shortest angular path. Produces smooth, human-like turning.
     */
    public static float stepTowards(float current, float target, float maxStep) {
        float delta = wrapDegrees(target - current);
        if (delta > maxStep) {
            delta = maxStep;
        }
        if (delta < -maxStep) {
            delta = -maxStep;
        }
        return current + delta;
    }
}
