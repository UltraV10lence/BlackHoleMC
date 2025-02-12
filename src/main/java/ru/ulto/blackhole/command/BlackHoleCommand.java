package ru.ulto.blackhole.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
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

import java.util.function.Function;

public class BlackHoleCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        var blockHoleBlocksAte = CommandManager.literal("blocksAte")
                .executes(ctx -> {
                    getBlocksAte(ctx);
                    return 1;
                })
                .then(CommandManager.argument("blocksAte", IntegerArgumentType.integer(0))
                        .executes(ctx -> {
                            var blocksAte = ctx.getArgument("blocksAte", Integer.class);
                            setBlocksAte(ctx, blocksAte);
                            return 1;
                        }));

        var blackHoleGConst = CommandManager.literal("gconst")
                .executes(ctx ->
                        sendFeedback(ctx, data ->
                                Text.literal("Гравитационная постоянная: " + data.gravityConstant()), false))
                .then(CommandManager.argument("gravityConstant", DoubleArgumentType.doubleArg())
                        .executes(ctx -> {
                            var gravityConstant = ctx.getArgument("gravityConstant", Double.class);
                            return sendFeedback(ctx, data -> {
                                data.gravityConstant(gravityConstant);
                                return Text.literal("Теперь гравитационная постоянная равна " + gravityConstant);
                            }, true);
                        }));

        var blackHoleRadiusMultiplier = CommandManager.literal("radiusMultiplier")
                .executes(ctx ->
                        sendFeedback(ctx, data ->
                                Text.literal("Множитель радиуса дыры: " + data.radiusMultiplier()), false))
                .then(CommandManager.argument("radiusMultiplier", DoubleArgumentType.doubleArg())
                        .executes(ctx -> {
                            var radiusMultiplier = ctx.getArgument("radiusMultiplier", Double.class);
                            return sendFeedback(ctx, data -> {
                                data.radiusMultiplier(radiusMultiplier);
                                return Text.literal("Теперь множитель радиуса дыры равен " + radiusMultiplier);
                            }, true);
                        }));

        var blackHoleSpeed = CommandManager.literal("speed")
                .executes(ctx ->
                        sendFeedback(ctx, data ->
                                Text.literal("Скорость чёрной дыры: " + data.blackHoleSpeed() + " блоков в секунду"), false))
                .then(CommandManager.argument("speed", DoubleArgumentType.doubleArg())
                        .executes(ctx -> {
                            var speed = ctx.getArgument("speed", Double.class);
                            return sendFeedback(ctx, data -> {
                                data.blackHoleSpeed(speed);
                                return Text.literal("Теперь скорость чёрной дыры равна " + speed + " блоков в секунду");
                            }, true);
                        }));

        var shouldResetPlayers = CommandManager.literal("resetPlayers")
                .executes(ctx ->
                        sendFeedback(ctx, data ->
                                Text.literal("Сейчас игроки " + (data.shouldResetPlayers() ? "сбрасываются" : "не сбрасываются") + " после начала игры"), false))
                .then(CommandManager.argument("reset", BoolArgumentType.bool())
                        .executes(ctx -> {
                            var resetPlayers = ctx.getArgument("reset", Boolean.class);
                            return sendFeedback(ctx, data -> {
                                data.shouldResetPlayers(resetPlayers);
                                return Text.literal("Игроки теперь " + (resetPlayers ? "сбрасываются" : "не сбрасываются") + " после начала игры");
                            }, true);
                        }));

        var startGame = CommandManager.literal("start")
                .executes(ctx -> {
                    BlackHole.startGame(ctx.getSource().getServer());
                    ctx.getSource().sendFeedback(() -> Text.literal("Начинаем игру"), false);
                    return 1;
                });

        var rePickPlace = CommandManager.literal("repickPlace")
                .executes(ctx -> {
                    BlackHole.repickPlace(ctx.getSource().getServer());
                    ctx.getSource().sendFeedback(() -> Text.literal("Новое место выбрано"), false);
                    return 1;
                });


        var borderShrinkTime = CommandManager.literal("time")
                .executes(ctx ->
                        sendFeedback(ctx, data ->
                        Text.literal("Время сужения границы: " + (data.borderShrinkTime() / 20) + " секунд"), false))
                .then(CommandManager.argument("time", new TimeArgument())
                        .executes(ctx -> {
                            var time = ctx.getArgument("time", Integer.class);
                            return sendFeedback(ctx, data -> {
                                data.borderShrinkTime(time);
                                return Text.literal("Время сужения границы теперь " + (time / 20) + " секунд");
                            }, true);
                        }));


        var borderPushesPlayersOut = CommandManager.literal("push")
                .executes(ctx ->
                        sendFeedback(ctx, data ->
                                Text.literal("Сейчас граница " + (data.borderPushPlayers() ? "выталкивает" : "не выталкивает") + " игроков"), false))
                .then(CommandManager.argument("push", BoolArgumentType.bool())
                        .executes(ctx -> {
                            var push = ctx.getArgument("push", Boolean.class);
                            return sendFeedback(ctx, data -> {
                                data.borderPushPlayers(push);
                                return Text.literal("Граница теперь " + (push ? "выталкивает" : "не выталкивает") + " игроков");
                            }, true);
                        }));

        var borderRadius = CommandManager.literal("radius")
                .executes(ctx ->
                        sendFeedback(ctx, data ->
                                Text.literal("Радиус барьера на начало игры: " + data.borderRadius()), false))
                .then(CommandManager.argument("radius", IntegerArgumentType.integer(0))
                        .executes(ctx -> {
                            var radius = ctx.getArgument("radius", Integer.class);
                            return sendFeedback(ctx, data -> {
                                data.borderRadius(radius);
                                return Text.literal("Радиус барьера на начало игры теперь " + radius);
                            }, true);
                        }));

        dispatcher.register(CommandManager.literal("blackhole")
                .requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.literal("worldborder")
                        .then(borderPushesPlayersOut)
                        .then(borderRadius)
                        .then(borderShrinkTime))
                .then(CommandManager.literal("blackhole")
                        .then(blockHoleBlocksAte)
                        .then(blackHoleGConst)
                        .then(blackHoleRadiusMultiplier)
                        .then(blackHoleSpeed))
                .then(CommandManager.literal("game")
                        .then(startGame)
                        .then(rePickPlace)
                        .then(shouldResetPlayers)));
    }

    public static void getBlocksAte(CommandContext<ServerCommandSource> ctx) {
        var controller = getController(ctx);
        if (controller == null) return;

        ctx.getSource().sendFeedback(() -> Text.literal("Чёрная дыра съела " + controller.thingsAte + " блоков"), false);
    }

    public static void setBlocksAte(CommandContext<ServerCommandSource> ctx, int blocksAte) {
        var controller = getController(ctx);
        if (controller == null) return;

        controller.thingsAte = blocksAte;
        ctx.getSource().sendFeedback(() -> Text.literal("Теперь чёрная дыра съела " + controller.thingsAte + " блоков"), false);
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

    public static int sendFeedback(CommandContext<ServerCommandSource> ctx, Function<BlackHoleSaveData, Text> consumer, boolean broadcastToOps) {
        var saveData = BlackHoleSaveData.of(ctx);
        var text = consumer.apply(saveData);
        ctx.getSource().sendFeedback(() -> text, broadcastToOps);
        return 1;
    }
}
