package com.isekai.buildersutils.mining;

import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Chooses the hotbar slot that mines a given block the fastest, like a player
 * swapping to the right tool before digging.
 */
public final class ToolSelector {
    private ToolSelector() {
    }

    /**
     * @return the best hotbar slot (0-8) for mining {@code state}, or -1 to keep the current slot.
     */
    public static int bestSlot(ClientPlayerEntity player, BlockState state) {
        int bestSlot = -1;
        float bestSpeed = 1.0f; // bare-hand baseline

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            float speed = stack.getMiningSpeedMultiplier(state);
            // Strongly prefer a tool that is actually "suitable" (drops the block).
            if (stack.isSuitableFor(state)) {
                speed *= 1.5f;
            }
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot;
    }
}
