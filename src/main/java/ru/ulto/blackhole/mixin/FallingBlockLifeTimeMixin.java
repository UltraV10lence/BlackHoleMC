package ru.ulto.blackhole.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.ulto.blackhole.accessors.FallingBlockTimeToLiveAccessor;

@Mixin(FallingBlockEntity.class)
public class FallingBlockLifeTimeMixin implements FallingBlockTimeToLiveAccessor {
    @Unique
    public int timeToLive = -1;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void tick(CallbackInfo ci) {
        if (shouldDiscard()) {
            var e = (Entity) (Object) this;
            e.discard();
            ci.cancel();
        }
    }

    @Unique
    public boolean shouldDiscard() {
        if (timeToLive == 0) return true;
        timeToLive--;
        return false;
    }

    @Override
    public int timeToLive() {
        return timeToLive;
    }

    @Override
    public void timeToLive(int lifeTime) {
        timeToLive = lifeTime;
    }
}
