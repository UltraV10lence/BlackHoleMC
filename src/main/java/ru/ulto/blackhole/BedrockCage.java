package ru.ulto.blackhole;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public record BedrockCage(ServerWorld world, BlockPos pos) {
    public static BedrockCage from(ServerPlayerEntity player) {
        return new BedrockCage(player.getServerWorld(), player.getBlockPos());
    }

    public void build() {
        buildOutline(Blocks.BEDROCK.getDefaultState());
    }

    public void remove() {
        buildOutline(Blocks.AIR.getDefaultState());
    }

    public void buildOutline(BlockState state) {
        world.setBlockState(pos.down(), state);

        world.setBlockState(pos.east(), state);
        world.setBlockState(pos.north(), state);
        world.setBlockState(pos.west(), state);
        world.setBlockState(pos.south(), state);

        world.setBlockState(pos.east().up(), state);
        world.setBlockState(pos.north().up(), state);
        world.setBlockState(pos.west().up(), state);
        world.setBlockState(pos.south().up(), state);

        world.setBlockState(pos.up(2), state);

        world.setBlockState(pos, Blocks.AIR.getDefaultState());
        world.setBlockState(pos.up(), Blocks.AIR.getDefaultState());
    }
}
