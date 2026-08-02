package com.isekai.buildersutils.mining;

import com.isekai.buildersutils.selection.SelectionManager;
import com.isekai.buildersutils.util.RotationUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A Baritone-style automated mining task. The player turns, walks, swaps tools,
 * and breaks blocks one by one until the selection (or its walls) is cleared.
 *
 * <p>The implementation is intentionally pragmatic rather than a full pathfinder:
 * it greedily targets the nearest remaining block, digs blocks within reach, and
 * walks toward them otherwise (jumping over / digging through obstructions). This
 * reliably clears open volumes and behaves much like a real player would, though
 * it is not as sophisticated as a complete A* path solver.
 */
public class MiningTask {
    /** Safety cap on selection size to avoid building an enormous block set. */
    public static final int MAX_BLOCKS = 250_000;

    private static final double REACH = 4.3;          // how close before we can mine a block
    private static final float MAX_TURN = 25.0f;      // max degrees turned per tick (human-like)

    public enum Mode {
        AREA,
        WALLS
    }

    private final Set<BlockPos> targets;
    private final Mode mode;

    private MiningTask(Set<BlockPos> targets, Mode mode) {
        this.targets = targets;
        this.mode = mode;
    }

    /**
     * Builds a task from the current selection. AREA collects every non-air block;
     * WALLS collects only the four vertical side walls (WorldEdit-style).
     */
    public static MiningTask create(SelectionManager sel, Mode mode, ClientWorld world) {
        BlockPos min = sel.min();
        BlockPos max = sel.max();
        Set<BlockPos> targets = new HashSet<>();

        for (BlockPos p : BlockPos.iterate(min, max)) {
            if (mode == Mode.WALLS) {
                boolean wall = p.getX() == min.getX() || p.getX() == max.getX()
                        || p.getZ() == min.getZ() || p.getZ() == max.getZ();
                if (!wall) {
                    continue;
                }
            }
            BlockState state = world.getBlockState(p);
            if (state.isAir()) {
                continue;
            }
            targets.add(p.toImmutable());
            if (targets.size() > MAX_BLOCKS) {
                break;
            }
        }
        return new MiningTask(targets, mode);
    }

    public int remaining() {
        return targets.size();
    }

    public Mode mode() {
        return mode;
    }

    /**
     * Advances the task one tick.
     *
     * @return true when the task is complete and should be discarded.
     */
    public boolean tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) {
            return true;
        }

        BlockPos target = pickTarget(player, world);
        if (target == null) {
            release();
            message(player, "Mining complete.", Formatting.GREEN);
            return true;
        }

        Vec3d eye = player.getEyePos();
        Vec3d center = Vec3d.ofCenter(target);
        double dist = eye.distanceTo(center);

        // Smoothly aim at the target block.
        float[] rot = RotationUtil.getRotations(eye, center);
        player.setYaw(RotationUtil.stepTowards(player.getYaw(), rot[0], MAX_TURN));
        player.setPitch(RotationUtil.stepTowards(player.getPitch(), rot[1], MAX_TURN));

        if (dist <= REACH) {
            stopWalking(client.options);
            mineBlock(client, player, world, target);
        } else {
            walkToward(client, player, target);
        }
        return false;
    }

    private BlockPos pickTarget(ClientPlayerEntity player, ClientWorld world) {
        Vec3d eye = player.getEyePos();
        int feetY = player.getBlockPos().getY();

        BlockPos bestAtOrAbove = null;
        double bestAtOrAboveDist = Double.MAX_VALUE;
        BlockPos bestAny = null;
        double bestAnyDist = Double.MAX_VALUE;

        Iterator<BlockPos> it = targets.iterator();
        while (it.hasNext()) {
            BlockPos p = it.next();
            BlockState s = world.getBlockState(p);
            // Drop blocks that are already gone or unbreakable (e.g. bedrock = -1 hardness).
            if (s.isAir() || s.getHardness(world, p) < 0) {
                it.remove();
                continue;
            }
            double d = eye.squaredDistanceTo(Vec3d.ofCenter(p));
            if (d < bestAnyDist) {
                bestAnyDist = d;
                bestAny = p;
            }
            // Prefer clearing blocks at or above the player's feet first so we do
            // not undermine ourselves; only dig below once the upper part is gone.
            if (p.getY() >= feetY && d < bestAtOrAboveDist) {
                bestAtOrAboveDist = d;
                bestAtOrAbove = p;
            }
        }
        return bestAtOrAbove != null ? bestAtOrAbove : bestAny;
    }

    private void mineBlock(MinecraftClient client, ClientPlayerEntity player, ClientWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            targets.remove(pos);
            return;
        }

        // Swap to the best tool in the hotbar before digging.
        int slot = ToolSelector.bestSlot(player, state);
        if (slot >= 0 && slot != player.getInventory().getSelectedSlot()) {
            player.getInventory().setSelectedSlot(slot);
            if (client.getNetworkHandler() != null) {
                client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            }
        }

        Direction face = faceToward(player.getEyePos(), pos);
        client.interactionManager.updateBlockBreakingProgress(pos, face);
        player.swingHand(Hand.MAIN_HAND);

        // The block becomes air locally the moment it breaks; drop it then.
        if (world.getBlockState(pos).isAir()) {
            targets.remove(pos);
        }
    }

    private void walkToward(MinecraftClient client, ClientPlayerEntity player, BlockPos target) {
        GameOptions o = client.options;
        o.forwardKey.setPressed(true);
        o.backKey.setPressed(false);
        o.leftKey.setPressed(false);
        o.rightKey.setPressed(false);
        o.sneakKey.setPressed(false);

        // Jump when bumping into terrain or when the target is above us.
        boolean needJump = player.horizontalCollision || target.getY() > player.getBlockPos().getY();
        o.jumpKey.setPressed(needJump);
    }

    private void stopWalking(GameOptions o) {
        o.forwardKey.setPressed(false);
        o.backKey.setPressed(false);
        o.leftKey.setPressed(false);
        o.rightKey.setPressed(false);
        o.jumpKey.setPressed(false);
        o.sneakKey.setPressed(false);
    }

    /** Picks the block face most directly facing the player's eyes. */
    private Direction faceToward(Vec3d eye, BlockPos pos) {
        Vec3d center = Vec3d.ofCenter(pos);
        double dx = eye.x - center.x;
        double dy = eye.y - center.y;
        double dz = eye.z - center.z;
        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double az = Math.abs(dz);
        if (ax >= ay && ax >= az) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        if (ay >= ax && ay >= az) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        }
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** Releases all movement keys; call when the task stops for any reason. */
    public void release() {
        MinecraftClient client = MinecraftClient.getInstance();
        GameOptions o = client.options;
        stopWalking(o);
        o.sprintKey.setPressed(false);
    }

    private static void message(ClientPlayerEntity player, String msg, Formatting color) {
        player.sendMessage(Text.literal("[BuildersUtils] " + msg).formatted(color), false);
    }
}
