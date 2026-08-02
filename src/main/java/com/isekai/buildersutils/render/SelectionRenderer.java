package com.isekai.buildersutils.render;

import com.isekai.buildersutils.selection.SelectionManager;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Draws a green wireframe box around the active selection using the 1.21.11
 * line render layer.
 */
public final class SelectionRenderer {
    private static final float R = 0.18f;
    private static final float G = 1.0f;
    private static final float B = 0.18f;
    private static final float A = 1.0f;
    private static final float WIDTH = 2.0f;

    private SelectionRenderer() {
    }

    public static void render(WorldRenderContext context) {
        SelectionManager sel = SelectionManager.getInstance();
        if (!sel.hasPos1()) {
            return;
        }

        Box box;
        if (sel.hasBoth()) {
            BlockPos min = sel.min();
            BlockPos max = sel.max();
            box = new Box(min.getX(), min.getY(), min.getZ(),
                    max.getX() + 1.0, max.getY() + 1.0, max.getZ() + 1.0);
        } else {
            BlockPos p = sel.getPos1();
            box = new Box(p.getX(), p.getY(), p.getZ(),
                    p.getX() + 1.0, p.getY() + 1.0, p.getZ() + 1.0);
        }

        MatrixStack matrices = context.matrices();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || consumers == null) {
            return;
        }

        Vec3d cam = MinecraftClient.getInstance().gameRenderer.getCamera().getCameraPos();

        matrices.push();
        // Move from world space into camera-relative space.
        matrices.translate(-cam.x, -cam.y, -cam.z);

        VertexConsumer lines = consumers.getBuffer(RenderLayers.lines());
        drawBox(matrices.peek(), lines, box);

        matrices.pop();

        // Flush the line layer so it is committed this frame.
        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw(RenderLayers.lines());
        }
    }

    private static void drawBox(MatrixStack.Entry entry, VertexConsumer vc, Box box) {
        float x0 = (float) box.minX, y0 = (float) box.minY, z0 = (float) box.minZ;
        float x1 = (float) box.maxX, y1 = (float) box.maxY, z1 = (float) box.maxZ;

        // Bottom face
        line(entry, vc, x0, y0, z0, x1, y0, z0);
        line(entry, vc, x1, y0, z0, x1, y0, z1);
        line(entry, vc, x1, y0, z1, x0, y0, z1);
        line(entry, vc, x0, y0, z1, x0, y0, z0);

        // Top face
        line(entry, vc, x0, y1, z0, x1, y1, z0);
        line(entry, vc, x1, y1, z0, x1, y1, z1);
        line(entry, vc, x1, y1, z1, x0, y1, z1);
        line(entry, vc, x0, y1, z1, x0, y1, z0);

        // Vertical edges
        line(entry, vc, x0, y0, z0, x0, y1, z0);
        line(entry, vc, x1, y0, z0, x1, y1, z0);
        line(entry, vc, x1, y0, z1, x1, y1, z1);
        line(entry, vc, x0, y0, z1, x0, y1, z1);
    }

    private static void line(MatrixStack.Entry entry, VertexConsumer vc,
                             float x1, float y1, float z1, float x2, float y2, float z2) {
        float nx = x2 - x1;
        float ny = y2 - y1;
        float nz = z2 - z1;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len == 0.0f) {
            return;
        }
        nx /= len;
        ny /= len;
        nz /= len;

        vc.vertex(entry, x1, y1, z1).color(R, G, B, A).normal(entry, nx, ny, nz).lineWidth(WIDTH);
        vc.vertex(entry, x2, y2, z2).color(R, G, B, A).normal(entry, nx, ny, nz).lineWidth(WIDTH);
    }
}
