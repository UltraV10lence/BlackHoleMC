package ru.ulto.blackhole.mixin;

import net.minecraft.world.dimension.NetherPortal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NetherPortal.class)
public class DisableNetherPortals {
    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
    public void disable(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
