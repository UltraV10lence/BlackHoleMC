package ru.ulto.blackhole.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import ru.ulto.blackhole.BlackHole;
import ru.ulto.blackhole.BlackHoleController;
import ru.ulto.blackhole.BlackHoleSaveData;
import ru.ulto.blackhole.BlackHoleWorld;
import ru.ulto.blackhole.arguments.TimeArgument;

public class BlackHoleCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("blackhole")
                .requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.literal("blocksAte")
                        .executes(ctx -> {
                            getBlocksAte(ctx);
                            return 1;
                        })
                        .then(CommandManager.argument("blocksAte", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    var blocksAte = ctx.getArgument("blocksAte", Integer.class);
                                    setBlocksAte(ctx, blocksAte);
                                    return 1;
                                })))
                .then(CommandManager.literal("resetPlayers")
                        .executes(ctx -> {
                            var resetPlayers = BlackHoleSaveData.of(ctx).shouldResetPlayers();
                            ctx.getSource().sendFeedback(() -> Text.literal("Сейчас игроки " + (resetPlayers ? "сбрасываются" : "не сбрасываются") + " после начала игры"), false);
                            return 1;
                        })
                        .then(CommandManager.argument("reset", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    var resetPlayers = ctx.getArgument("reset", Boolean.class);

                                    BlackHoleSaveData.of(ctx).shouldResetPlayers(resetPlayers);
                                    ctx.getSource().sendFeedback(() -> Text.literal("Игроки теперь " + (resetPlayers ? "сбрасываются" : "не сбрасываются") + " после начала игры"), true);
                                    return 1;
                                })))
                .then(CommandManager.literal("repickPlace")
                        .executes(ctx -> {
                            BlackHole.repickPlace(ctx.getSource().getServer());
                            return 1;
                        }))
                .then(CommandManager.literal("start")
                        .executes(ctx -> {
                            BlackHole.startGame(ctx.getSource().getServer());
                            return 1;
                        }))
                .then(CommandManager.literal("border")
                        .then(CommandManager.literal("time")
                                .executes(ctx -> {
                                    var time = BlackHoleSaveData.of(ctx).borderShrinkTime();
                                    ctx.getSource().sendFeedback(() -> Text.literal("Время сужения границы: " + (time / 20) + " секунд"), false);
                                    return 1;
                                })
                                .then(CommandManager.argument("time", new TimeArgument())
                                        .executes(ctx -> {
                                            var time = ctx.getArgument("time", Integer.class);

                                            BlackHoleSaveData.of(ctx).borderShrinkTime(time);
                                            ctx.getSource().sendFeedback(() -> Text.literal("Время сужения границы теперь " + (time / 20) + " секунд"), true);
                                            return 1;
                                        })))
                        .then(CommandManager.literal("push")
                                .executes(ctx -> {
                                    var pushing = BlackHoleSaveData.of(ctx).borderPushPlayers();
                                    ctx.getSource().sendFeedback(() -> Text.literal("Сейчас граница " + (pushing ? "выталкивает" : "не выталкивает") + " игроков"), false);
                                    return 1;
                                })
                                .then(CommandManager.argument("push", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            var push = ctx.getArgument("push", Boolean.class);

                                            BlackHoleSaveData.of(ctx).borderPushPlayers(push);
                                            ctx.getSource().sendFeedback(() -> Text.literal("Граница теперь " + (push ? "выталкивает" : "не выталкивает") + " игроков"), true);
                                            return 1;
                                        }))))
                .then(CommandManager.literal("radius")
                        .executes(ctx -> {
                            var radius = BlackHoleSaveData.of(ctx).borderRadius();
                            ctx.getSource().sendFeedback(() -> Text.literal("Радиус барьера на начало игры: " + radius), false);
                            return 1;
                        })
                        .then(CommandManager.argument("radius", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    var radius = ctx.getArgument("radius", Integer.class);

                                    BlackHoleSaveData.of(ctx).borderRadius(radius);
                                    ctx.getSource().sendFeedback(() -> Text.literal("Радиус барьера на начало игры теперь " + radius), true);
                                    return 1;
                                }))));
    }

    public static void getBlocksAte(CommandContext<ServerCommandSource> ctx) {
        var controller = getController(ctx);
        if (controller == null) return;

        ctx.getSource().sendFeedback(() -> Text.literal("Чёрная дыра съела " + controller.blocksAte + " блоков"), false);
    }

    public static void setBlocksAte(CommandContext<ServerCommandSource> ctx, int blocksAte) {
        var controller = getController(ctx);
        if (controller == null) return;

        controller.blocksAte = blocksAte;
        ctx.getSource().sendFeedback(() -> Text.literal("Теперь чёрная дыра съела " + controller.blocksAte + " блоков"), false);
    }

    private static BlackHoleController getController(CommandContext<ServerCommandSource> ctx) {
        var world = ctx.getSource().getServer().getOverworld();
        var blackHoleWorld = (BlackHoleWorld) world;
        if (!blackHoleWorld.hasBlackHole()) {
            ctx.getSource().sendFeedback(() -> Text.literal("В этом мире ещё нет активной чёрной дыры"), false);
            return null;
        }

        return blackHoleWorld.getController();
    }
}
