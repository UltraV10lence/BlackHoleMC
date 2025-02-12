package ru.ulto.blackhole;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import net.minecraft.world.TeleportTarget;
import ru.ulto.blackhole.arguments.TimeArgument;
import ru.ulto.blackhole.command.BlackHoleCommand;
import ru.ulto.blackhole.packets.AddVelocityPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BlackHole implements ModInitializer {
	public static final String MOD_ID = "blackhole";

	private static final List<Vec3d> velocitiesToAdd = new ArrayList<>();

	@Override
	public void onInitialize() {
		ArgumentTypeRegistry.registerArgumentType(Identifier.of(MOD_ID, "time_argument"),
				TimeArgument.class, ConstantArgumentSerializer.of(TimeArgument::new));

		PayloadTypeRegistry.playS2C().register(AddVelocityPayload.ID, AddVelocityPayload.CODEC);

		CommandRegistrationCallback.EVENT.register(((dispatcher, access, environment) -> BlackHoleCommand.register(dispatcher)));

		ClientPlayNetworking.registerGlobalReceiver(AddVelocityPayload.ID, (addVel, ctx) -> velocitiesToAdd.add(addVel.velocity()));
		ClientTickEvents.END_CLIENT_TICK.register((client) -> {
			if (client.player == null) return;

			for (var vel : velocitiesToAdd) {
				applyVelocity(client.player, vel);
				if (client.player.hasVehicle()) applyVelocity(client.player.getVehicle(), vel);
			}
			velocitiesToAdd.clear();
		});
	}

	public static void applyVelocity(Entity e, Vec3d force) {
		force = force.multiply(1, 1.25, 1);
		e.addVelocity(force);
		e.setVelocity(e.getVelocity().multiply(BlackHoleController.AIR_FRICTION_MULT));
	}

	public static void repickPlace(MinecraftServer server) {
		stopGame(server);
		final var parameters = BlackHoleSaveData.of(server);
		final var overWorld = server.getOverworld();

		var random = new Random();
		final var randomCenter = new BlockPos(random.nextInt(-25_000_000, 25_000_000), overWorld.getSeaLevel(), random.nextInt(-25_000_000, 25_000_000));

		final var playerManager = server.getPlayerManager();

		var playersCount = playerManager.getCurrentPlayerCount();
		final var deltaDegrees = Math.toRadians(360d / playersCount);

		final var radius = parameters.borderRadius();

		overWorld.getWorldBorder().setCenter(randomCenter.getX(), randomCenter.getZ());
		overWorld.getWorldBorder().setSize(radius * 2);

		for (var i = 0; i < playersCount; i++) {
			var player = playerManager.getPlayerList().get(i);

			var degrees = Math.toRadians(i * deltaDegrees);

			var playerX = (int) (Math.cos(degrees) * (radius - 5) + randomCenter.getX());
			var playerZ = (int) (Math.sin(degrees) * (radius - 5) + randomCenter.getZ());

			overWorld.getWorldChunk(new BlockPos(playerX, 0, playerZ));

			var playerY = overWorld.getTopY(Heightmap.Type.WORLD_SURFACE, playerX, playerZ) + 3;
			var playerBlock = new BlockPos(playerX, playerY, playerZ);

			var target = new TeleportTarget(overWorld, playerBlock.toBottomCenterPos(), Vec3d.ZERO, player.getYaw(), player.getPitch(), TeleportTarget.NO_OP);
			player.teleportTo(target);

			if (parameters.shouldResetPlayers()) resetPlayer(player);
			BedrockCage.from(player).build();
		}

		var blackHoleWorld = (BlackHoleWorld) overWorld;
		blackHoleWorld.initializeBlackHole(randomCenter);
	}

	public static void resetPlayer(ServerPlayerEntity player) {
		player.getInventory().clear();
		player.getEnderChestInventory().clear();
		player.clearActiveItem();

		player.detach();

		player.changeGameMode(GameMode.SURVIVAL);

		player.clearStatusEffects();
		player.clearSleepingPosition();
		player.resetLastAttackedTicks();
		player.setStingerCount(0);
		player.setStuckArrowCount(0);

		player.setHealth(player.getMaxHealth());
		player.getHungerManager().setFoodLevel(20);
		player.getHungerManager().setSaturationLevel(5);
	}

	public static void stopGame(MinecraftServer server) {
		var world = server.getOverworld();
		world.getWorldBorder().setCenter(0, 0);
		world.getWorldBorder().setSize(30_000_000);

		var blackHoleWorld = (BlackHoleWorld) world;
		blackHoleWorld.stopBlackHole();
	}

	public static void startGame(MinecraftServer server) {
		final var parameters = BlackHoleSaveData.of(server);

		final var gameStartedText = Text.literal("Игра началась").formatted(Formatting.BOLD, Formatting.GREEN);
		for (var player : server.getPlayerManager().getPlayerList()) {
			BedrockCage.from(player).remove();
			player.networkHandler.sendPacket(new TitleS2CPacket(gameStartedText));
		}

		var world = server.getOverworld();
		world.getWorldBorder().interpolateSize(parameters.borderRadius() * 2, 20, parameters.borderShrinkTime() * 50L); // ticks to milliseconds

		var blackHoleWorld = (BlackHoleWorld) world;
		blackHoleWorld.startBlackHole();
	}
}