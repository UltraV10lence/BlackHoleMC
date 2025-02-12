package ru.ulto.blackhole;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.world.PersistentState;
import ru.ulto.blackhole.accessors.SaveDataAccessor;

public class BlackHoleSaveData extends PersistentState {
    private int borderShrinkTime = 20 * 60 * 60; // 1h default
    private int borderRadius = 2501;
    private double radiusMultiplier = 1;
    private double gravityConstant = .2;
    private double blackHoleSpeed = .6;
    private boolean shouldResetPlayers = true;
    private boolean borderPushPlayers = false;

    public int borderRadius() {
        return borderRadius;
    }
    public void borderRadius(int borderRadius) {
        this.borderRadius = borderRadius;
        markDirty();
    }

    public int borderShrinkTime() {
        return borderShrinkTime;
    }
    public void borderShrinkTime(int borderShrinkTime) {
        this.borderShrinkTime = borderShrinkTime;
        markDirty();
    }

    public double radiusMultiplier() {
        return radiusMultiplier;
    }
    public void radiusMultiplier(double radiusMultiplier) { //todo make a command to control this
        this.radiusMultiplier = radiusMultiplier;
        markDirty();
    }

    public double gravityConstant() {
        return gravityConstant;
    }
    public void gravityConstant(double gravityConstant) {
        this.gravityConstant = gravityConstant;
        markDirty();
    }

    public double blackHoleSpeed() {
        return blackHoleSpeed;
    }
    public void blackHoleSpeed(double blackHoleSpeed) {
        this.blackHoleSpeed = blackHoleSpeed;
        markDirty();
    }

    public boolean shouldResetPlayers() {
        return shouldResetPlayers;
    }
    public void shouldResetPlayers(boolean shouldResetPlayers) {
        this.shouldResetPlayers = shouldResetPlayers;
        markDirty();
    }

    public boolean borderPushPlayers() {
        return borderPushPlayers;
    }
    public void borderPushPlayers(boolean borderPushesPlayers) {
        this.borderPushPlayers = borderPushesPlayers;
        markDirty();
    }

    public static PersistentState.Type<BlackHoleSaveData> getType() {
        return new Type<>(BlackHoleSaveData::new, (nbt, registry) -> readNbt(nbt), null);
    }

    public static BlackHoleSaveData readNbt(NbtCompound nbt) {
        var saveData = new BlackHoleSaveData();
        saveData.borderShrinkTime = getIntOrDefault(nbt, "shrink_time", saveData.borderShrinkTime);
        saveData.borderRadius = getIntOrDefault(nbt, "border_radius", saveData.borderRadius);
        saveData.radiusMultiplier = getDoubleOrDefault(nbt, "radius_multiplier", saveData.radiusMultiplier);
        saveData.gravityConstant = getDoubleOrDefault(nbt, "gravity_constant", saveData.gravityConstant);
        saveData.blackHoleSpeed = getDoubleOrDefault(nbt, "black_hole_speed", saveData.blackHoleSpeed);
        saveData.shouldResetPlayers = getBooleanOrDefault(nbt, "reset_players", saveData.shouldResetPlayers);
        saveData.borderPushPlayers = getBooleanOrDefault(nbt, "border_push_players", saveData.borderPushPlayers);
        return saveData;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        nbt.putInt("shrink_time", borderShrinkTime());
        nbt.putInt("border_radius", borderRadius());
        nbt.putDouble("radius_multiplier", radiusMultiplier());
        nbt.putDouble("gravity_constant", gravityConstant());
        nbt.putDouble("black_hole_speed", blackHoleSpeed());
        nbt.putBoolean("reset_players", shouldResetPlayers());
        nbt.putBoolean("border_push_players", borderPushPlayers());
        return nbt;
    }

    private static int getIntOrDefault(NbtCompound nbt, String key, int def) {
        return nbt.contains(key, NbtElement.INT_TYPE) ? nbt.getInt(key) : def;
    }

    private static double getDoubleOrDefault(NbtCompound nbt, String key, double def) {
        return nbt.contains(key, NbtElement.DOUBLE_TYPE) ? nbt.getDouble(key) : def;
    }

    private static boolean getBooleanOrDefault(NbtCompound nbt, String key, boolean def) {
        return nbt.contains(key, NbtElement.BYTE_TYPE) ? nbt.getBoolean(key) : def;
    }

    public static BlackHoleSaveData of(CommandContext<ServerCommandSource> ctx) {
        var server = ctx.getSource().getServer();
        return of(server);
    }

    public static BlackHoleSaveData of(MinecraftServer server) {
        var dataAccessor = (SaveDataAccessor) server.getOverworld();
        return dataAccessor.getSaveData();
    }
}
