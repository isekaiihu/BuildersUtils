package com.isekai.buildersutils.selection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * Holds the two corner positions of the current selection (client-side only).
 */
public class SelectionManager {
    private static final SelectionManager INSTANCE = new SelectionManager();

    public static SelectionManager getInstance() {
        return INSTANCE;
    }

    private BlockPos pos1;
    private BlockPos pos2;

    private SelectionManager() {
    }

    public void setPos1(BlockPos pos) {
        this.pos1 = pos.toImmutable();
        message("Position 1 set to " + format(this.pos1));
    }

    public void setPos2(BlockPos pos) {
        this.pos2 = pos.toImmutable();
        message("Position 2 set to " + format(this.pos2));
    }

    public BlockPos getPos1() {
        return pos1;
    }

    public BlockPos getPos2() {
        return pos2;
    }

    public boolean hasPos1() {
        return pos1 != null;
    }

    public boolean hasBoth() {
        return pos1 != null && pos2 != null;
    }

    /** Inclusive minimum corner of the selection box. */
    public BlockPos min() {
        return new BlockPos(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ()));
    }

    /** Inclusive maximum corner of the selection box. */
    public BlockPos max() {
        return new BlockPos(
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ()));
    }

    public void clear() {
        pos1 = null;
        pos2 = null;
    }

    private static String format(BlockPos p) {
        return "(" + p.getX() + ", " + p.getY() + ", " + p.getZ() + ")";
    }

    private static void message(String msg) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("[BuildersUtils] " + msg).formatted(Formatting.GREEN), false);
        }
    }
}
