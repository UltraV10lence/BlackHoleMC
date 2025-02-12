package ru.ulto.blackhole;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import ru.ulto.blackhole.accessors.BlackHoleBlockAccessor;
import ru.ulto.blackhole.accessors.EntityManagerAccessor;
import ru.ulto.blackhole.packets.AddVelocityPayload;

public class BlackHoleController {
    public static final double G = .3;
    public final ServerWorld world;
    public Vec3d position;
    public int blocksAte = 0;

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
    public boolean isStarted() {
        return isStarted;
    }

    public void tick() {
        if (!isStarted) return;

        var offset = new Vec3d(0, 0, 0);

        var normalizedRadius = 0.05 * Math.pow(blocksAte, 0.3);
        var radiusMultiplier = currentSaveData.radiusMultiplier();
        final var radius = normalizedRadius * radiusMultiplier + 2;

        for (var player : world.getServer().getPlayerManager().getPlayerList()) {
            if (player.interactionManager.getGameMode() == GameMode.SPECTATOR) continue;

            var forceVector = player.getPos().subtract(position);
            offset = offset.add(forceVector);
        }

        if (offset.lengthSquared() > 0) {
            offset = offset.normalize().multiply(1d / 20);

            position = position.add(offset);
        }

        for (var e : getEntityManager(world).getLookup().iterate()) {
            if (!(e instanceof LivingEntity) && !(e instanceof VehicleEntity)) continue;

            if (e instanceof VehicleEntity vehicle &&
                    vehicle.hasPassenger(passenger -> passenger instanceof PlayerEntity)) continue;

            var distance = e.getPos().distanceTo(position);

            var force = G * radius / (distance * distance);
            var forceVector = position.subtract(e.getPos()).multiply(force);

            if (force < 0.0001) continue;

            e.addVelocity(forceVector);
            if (e instanceof ServerPlayerEntity player) {
                if (!player.interactionManager.getGameMode().isSurvivalLike()) continue;
                ServerPlayNetworking.send(player, new AddVelocityPayload(forceVector));
            }

            if (e instanceof LivingEntity living && distance <= radius) {
                addBlackHoleEffect(living, StatusEffects.BLINDNESS);
                addBlackHoleEffect(living, StatusEffects.DARKNESS);
                addBlackHoleEffect(living, StatusEffects.NAUSEA);
                addBlackHoleEffect(living, StatusEffects.SLOWNESS);
                addBlackHoleEffect(living, StatusEffects.WEAKNESS);
                addBlackHoleEffect(living, StatusEffects.MINING_FATIGUE);
                if (e.age % 10 == 0) living.setHealth(living.getHealth() - 6);
            }
        }

        var roundedRadius = (int) radius;
        for (var x = -roundedRadius; x <= roundedRadius; x++) {
            for (var y = -roundedRadius; y <= roundedRadius; y++) {
                for (var z = -roundedRadius; z <= roundedRadius; z++) {
                    var dst = Math.sqrt(x*x + y*y + z*z);

                    if (dst <= roundedRadius)
                        world.setBlockState(BlockPos.ofFloored(position.getX() + x, position.getY() + y, position.getZ() + z), Blocks.AIR.getDefaultState());

                    if (dst <= roundedRadius - 2) {
                        var state = asBlackHoleState(Blocks.BLACK_CONCRETE.getDefaultState());
                        world.setBlockState(BlockPos.ofFloored(position.getX() + x, position.getY() + y, position.getZ() + z), state);
                    }
                }
            }
        }

        var eatRadius = radius * 2 + 5;

        for (var x = -eatRadius; x <= eatRadius; x++) {
            for (var y = -eatRadius; y <= eatRadius; y++) {
                for (var z = -eatRadius; z <= eatRadius; z++) {
                    var distance = x*x + y*y + z*z;
                    if (distance > eatRadius * eatRadius) continue;

                    var blockPos = BlockPos.ofFloored(position.getX() + x, position.getY() + y, position.getZ() + z);

                    var state = world.getBlockState(blockPos);
                    if (isBlackHole(state)) continue;

                    if (state.isOf(Blocks.AIR) || state.isOf(Blocks.BEDROCK)) continue;

                    addFlyingBlock(); //todo
                    world.setBlockState(blockPos, Blocks.AIR.getDefaultState());
                    blocksAte++;
                }
            }
        }

        world.setBlockState(new BlockPos((int) position.x, (int) position.y, (int) position.z), asBlackHoleState(Blocks.BLACK_CONCRETE.getDefaultState()));
    }

    public void addFlyingBlock() {

    }

    public ServerEntityManager<Entity> getEntityManager(ServerWorld world) {
        var accessor = (EntityManagerAccessor) world;
        return accessor.getEntityManager();
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
}
