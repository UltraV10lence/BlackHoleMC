package ru.ulto.blackhole.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndPortalBlock.class)
public class DisableEndPortals {
    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    public void disable(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        ci.cancel();
    }
}
