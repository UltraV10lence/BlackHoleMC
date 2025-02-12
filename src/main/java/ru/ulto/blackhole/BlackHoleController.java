package ru.ulto.blackhole;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import ru.ulto.blackhole.accessors.BlackHoleBlockAccessor;
import ru.ulto.blackhole.accessors.FallingBlockTimeToLiveAccessor;
import ru.ulto.blackhole.packets.AddVelocityPayload;

import java.util.ArrayList;
import java.util.Random;

public class BlackHoleController {
    public static final double AIR_FRICTION_MULT = .997;

    public final ServerWorld world;
    public Vec3d position;
    public int thingsAte = 0;

    private final BlackHoleSaveData currentSaveData;
    private boolean isStarted;

    public BlackHoleController(ServerWorld world, Vec3d spawnPos) {
        this.world = world;
        this.position = spawnPos;
        currentSaveData = BlackHoleSaveData.of(world.getServer());
    }

    public void start() {
        isStarted = true;
    }

    public void tick() {
        if (!isStarted) return;

        var offset = new Vec3d(0, 0, 0);

        var normalizedRadius = 0.05 * Math.pow(thingsAte, 0.3);
        var radiusMultiplier = currentSaveData.radiusMultiplier();
        final var radius = normalizedRadius * radiusMultiplier + 2;

        for (var player : world.getServer().getPlayerManager().getPlayerList()) {
            if (!player.interactionManager.getGameMode().isSurvivalLike()) continue;

            var forceVector = player.getPos().subtract(position);
            offset = offset.add(forceVector);
        }

        if (offset.lengthSquared() > 0) {
            offset = offset.normalize().multiply(currentSaveData.blackHoleSpeed() / 20);

            position = position.add(offset);
        }

        var toRemove = new ArrayList<Entity>();
        world.iterateEntities().forEach(e -> {
            if (!(e instanceof LivingEntity) && !(e instanceof VehicleEntity) && !(e instanceof FallingBlockEntity)) return;

            if (e instanceof VehicleEntity vehicle &&
                    vehicle.hasPassenger(passenger -> passenger instanceof PlayerEntity)) return;

            var distance = e.getPos().distanceTo(position);

            var force = currentSaveData.gravityConstant() * radius / (distance * distance);
            if (force < 0.00005) return;

            var forceVector = position.subtract(e.getPos()).multiply(force);
            BlackHole.applyVelocity(e, forceVector);

            if (e instanceof ServerPlayerEntity player) {
                if (!player.interactionManager.getGameMode().isSurvivalLike()) return;
                ServerPlayNetworking.send(player, new AddVelocityPayload(forceVector));
            }

            if (distance <= radius) {
                if (!(e instanceof LivingEntity living)) {
                    toRemove.add(e);
                    return;
                }

                addBlackHoleEffect(living, StatusEffects.BLINDNESS);
                addBlackHoleEffect(living, StatusEffects.DARKNESS);
                addBlackHoleEffect(living, StatusEffects.NAUSEA);
                addBlackHoleEffect(living, StatusEffects.SLOWNESS);
                addBlackHoleEffect(living, StatusEffects.WEAKNESS);
                addBlackHoleEffect(living, StatusEffects.MINING_FATIGUE);

                if (e.age % 10 == 0) living.damage(world, getDamageSource(), 6);
                if (living.isDead()) thingsAte++;
            }
        });
        toRemove.forEach(e -> e.remove(Entity.RemovalReason.KILLED));

        var roundedRadius = (int) radius;
        for (var x = -roundedRadius; x <= roundedRadius; x++) {
            for (var y = -roundedRadius; y <= roundedRadius; y++) {
                for (var z = -roundedRadius; z <= roundedRadius; z++) {
                    world.setBlockState(BlockPos.ofFloored(position.getX() + x, position.getY() + y, position.getZ() + z), Blocks.AIR.getDefaultState());

                    var dst = Math.sqrt(x*x + y*y + z*z);
                    if (dst <= roundedRadius - 2) {
                        var state = asBlackHoleState(Blocks.BLACK_CONCRETE.getDefaultState());
                        world.setBlockState(BlockPos.ofFloored(position.getX() + x, position.getY() + y, position.getZ() + z), state);
                    }
                }
            }
        }

        final var random = new Random();

        var eatRadius = radius * 2 + 2;
        for (var x = -eatRadius; x <= eatRadius; x++) {
            for (var y = -eatRadius; y <= eatRadius; y++) {
                for (var z = -eatRadius; z <= eatRadius; z++) {
                    var distance = x*x + y*y + z*z;
                    if (distance > eatRadius * eatRadius) continue;

                    var blockPos = BlockPos.ofFloored(position.getX() + x, position.getY() + y, position.getZ() + z);

                    var state = world.getBlockState(blockPos);
                    if (isBlackHole(state)) continue;

                    if (state.isOf(Blocks.AIR) || state.isOf(Blocks.BEDROCK)) continue;

                    if (canAddFlyingBlock(random, state, blockPos, radius))
                        tryAddFlyingBlock(state, blockPos);

                    world.setBlockState(blockPos, Blocks.AIR.getDefaultState());
                    thingsAte++;
                }
            }
        }

        world.setBlockState(new BlockPos((int) position.x, (int) position.y, (int) position.z), asBlackHoleState(Blocks.BLACK_CONCRETE.getDefaultState()));
    }

    public boolean canAddFlyingBlock(Random random, BlockState blockState, BlockPos pos, double radius) {
        if (!blockState.isSolidBlock(world, pos)) return false;
        if (blockState.getBlock().getHardness() < 0) return false;

        var breakForce = random.nextDouble() * radius;
        if (breakForce > 1d / (blockState.getBlock().getHardness() / Blocks.STONE.getHardness())) return false;

        return world.getChunkManager().isChunkLoaded(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()));
    }

    public void tryAddFlyingBlock(BlockState state, BlockPos pos) {
        var block = FallingBlockEntity.spawnFromBlock(world, pos, state);
        block.addVelocity(new Vec3d(0, 0.05, 0));
        block.setDestroyedOnLanding();
        block.setNoGravity(true);
        var timeToLive = (FallingBlockTimeToLiveAccessor) block;
        timeToLive.timeToLive(100);
    }

    public void addBlackHoleEffect(LivingEntity e, RegistryEntry<StatusEffect> effect) {
        e.addStatusEffect(new StatusEffectInstance(effect, 200, 1, true, false, false));
    }

    public BlockState asBlackHoleState(BlockState state) {
        var bhb = (BlackHoleBlockAccessor) state;
        bhb.setBlackHoleBlock(true);
        return state;
    }

    public boolean isBlackHole(BlockState state) {
        var bhb = (BlackHoleBlockAccessor) state;
        return bhb.isBlackHoleBlock();
    }

    public DamageSource getDamageSource() {
        /*var registry = world.getServer().getRegistryManager().getOrThrow(RegistryKeys.DAMAGE_TYPE);
        var key = registry.getOrThrow(DamageTypes.BLACK_HOLE_TYPE);*/
        return world.getDamageSources().cramming();
    }
}
