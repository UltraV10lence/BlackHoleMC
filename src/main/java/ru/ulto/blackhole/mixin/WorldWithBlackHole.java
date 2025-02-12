package ru.ulto.blackhole.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.spawner.SpecialSpawner;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.ulto.blackhole.BlackHoleController;
import ru.ulto.blackhole.BlackHoleSaveData;
import ru.ulto.blackhole.BlackHoleWorld;
import ru.ulto.blackhole.accessors.SaveDataAccessor;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

@Mixin(ServerWorld.class)
public abstract class WorldWithBlackHole implements SaveDataAccessor, BlackHoleWorld {
    @Shadow public abstract PersistentStateManager getPersistentStateManager();

    @Shadow @Final private ServerEntityManager<Entity> entityManager;
    @Unique
    public BlackHoleController blackHole;
    @Unique
    public BlackHoleSaveData saveData;

    @Inject(method = "<init>", at = @At("TAIL"))
    public void addPersistentState(MinecraftServer server, Executor workerExecutor, LevelStorage.Session session,
                                   ServerWorldProperties properties, RegistryKey<World> worldKey, DimensionOptions dimensionOptions,
                                   WorldGenerationProgressListener worldGenerationProgressListener, boolean debugWorld, long seed,
                                   List<SpecialSpawner> spawners, boolean shouldTickTime, RandomSequencesState randomSequencesState, CallbackInfo ci) {
        saveData = getPersistentStateManager().getOrCreate(BlackHoleSaveData.getType(), "black_hole");
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void tickBlackHole(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if (blackHole == null) return;

        blackHole.tick();
    }

    @Override
    public BlackHoleSaveData getSaveData() {
        return saveData;
    }

    @Override
    public void initializeBlackHole(BlockPos center) {
        var world = (ServerWorld) (Object) this;
        blackHole = new BlackHoleController(world, center.toCenterPos());
    }

    @Override
    public void startBlackHole() {
        if (blackHole != null) blackHole.start();
    }

    @Override
    public void stopBlackHole() {
        blackHole = null;
    }

    @Override
    public boolean hasBlackHole() {
        return blackHole != null;
    }

    @Override
    public BlackHoleController getController() {
        return blackHole;
    }
}
