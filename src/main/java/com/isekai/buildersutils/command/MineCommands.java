package com.isekai.buildersutils.command;

import com.isekai.buildersutils.mining.MiningManager;
import com.isekai.buildersutils.mining.MiningTask;
import com.isekai.buildersutils.selection.SelectionManager;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Registers the {@code //mine ...} and {@code //desel} client commands.
 *
 * <p>The leading {@code /} of the client-command prefix plus a literal named
 * {@code /mine} produces the WorldEdit-style {@code //mine} when typed in chat.
 */
public final class MineCommands {
    private MineCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                literal("/mine")
                        .then(literal("area")
                                .executes(ctx -> startMine(ctx.getSource(), MiningTask.Mode.AREA)))
                        .then(literal("walls")
                                .executes(ctx -> startMine(ctx.getSource(), MiningTask.Mode.WALLS)))
                        .then(literal("off")
                                .executes(ctx -> stopMine(ctx.getSource())))
        );

        dispatcher.register(
                literal("/desel").executes(ctx -> {
                    SelectionManager.getInstance().clear();
                    MiningManager.getInstance().stop();
                    feedback(ctx.getSource(), "Selection cleared.", Formatting.YELLOW);
                    return 1;
                })
        );
    }

    private static int startMine(FabricClientCommandSource source, MiningTask.Mode mode) {
        SelectionManager sel = SelectionManager.getInstance();
        if (!sel.hasBoth()) {
            feedback(source,
                    "Set both positions first (wooden axe: left-click = pos1, right-click = pos2).",
                    Formatting.RED);
            return 0;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        MiningTask task = MiningTask.create(sel, mode, client.world);
        int count = task.remaining();

        if (count == 0) {
            feedback(source, "Nothing to mine in that selection.", Formatting.YELLOW);
            return 0;
        }
        if (count >= MiningTask.MAX_BLOCKS) {
            feedback(source,
                    "Selection too large (>= " + MiningTask.MAX_BLOCKS + " blocks). Shrink it.",
                    Formatting.RED);
            return 0;
        }

        MiningManager.getInstance().start(task);
        feedback(source, "Mining " + mode.name().toLowerCase() + " started: " + count + " blocks.",
                Formatting.GREEN);
        return 1;
    }

    private static int stopMine(FabricClientCommandSource source) {
        boolean wasRunning = MiningManager.getInstance().isRunning();
        MiningManager.getInstance().stop();
        feedback(source, wasRunning ? "Mining stopped." : "No mining task running.", Formatting.YELLOW);
        return 1;
    }

    private static void feedback(FabricClientCommandSource source, String msg, Formatting color) {
        source.sendFeedback(Text.literal("[BuildersUtils] " + msg).formatted(color));
    }
}
