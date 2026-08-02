package com.isekai.buildersutils;

import com.isekai.buildersutils.command.MineCommands;
import com.isekai.buildersutils.mining.MiningManager;
import com.isekai.buildersutils.render.SelectionRenderer;
import com.isekai.buildersutils.selection.SelectionManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the BuildersUtils client mod.
 *
 * <p>Wire-up:
 * <ul>
 *     <li>Left-click a block while holding a wooden axe -> position 1.</li>
 *     <li>Right-click a block while holding a wooden axe -> position 2.</li>
 *     <li>A green outline is drawn around the active selection every frame.</li>
 *     <li>{@code //mine area}, {@code //mine walls}, {@code //mine off}, {@code //desel} commands.</li>
 * </ul>
 */
public class BuildersUtilsClient implements ClientModInitializer {
    public static final String MOD_ID = "buildersutils";
    public static final Logger LOGGER = LoggerFactory.getLogger("BuildersUtils");

    @Override
    public void onInitializeClient() {
        registerSelectionHandlers();

        // Render the green selection outline each frame.
        WorldRenderEvents.AFTER_ENTITIES.register(SelectionRenderer::render);

        // Drive the active mining task once per client tick.
        ClientTickEvents.END_CLIENT_TICK.register(client -> MiningManager.getInstance().tick(client));

        // Register the //mine and //desel client commands.
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                MineCommands.register(dispatcher));

        LOGGER.info("BuildersUtils initialized. Wooden axe: left-click = pos1, right-click = pos2.");
    }

    private void registerSelectionHandlers() {
        // Left-click (attack) a block with a wooden axe -> set position 1.
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClient()
                    && hand == Hand.MAIN_HAND
                    && player.getStackInHand(hand).isOf(Items.WOODEN_AXE)) {
                SelectionManager.getInstance().setPos1(pos);
                // Swallow the action so the block is not actually broken/damaged.
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        // Right-click (use) a block with a wooden axe -> set position 2.
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()
                    && hand == Hand.MAIN_HAND
                    && player.getStackInHand(hand).isOf(Items.WOODEN_AXE)) {
                SelectionManager.getInstance().setPos2(hitResult.getBlockPos());
                // Swallow the action so the axe does not strip logs / interact.
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
    }
}
