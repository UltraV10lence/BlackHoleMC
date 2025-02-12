package ru.ulto.blackhole.mixin;

import net.minecraft.block.AbstractBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import ru.ulto.blackhole.accessors.BlackHoleBlockAccessor;

@Mixin(AbstractBlock.AbstractBlockState.class)
public class BlackHoleBlockState implements BlackHoleBlockAccessor {
    @Unique
    public boolean isBlackHoleBlock;

    @Override
    public void setBlackHoleBlock(boolean bhb) {
        isBlackHoleBlock = bhb;
    }

    @Override
    public boolean isBlackHoleBlock() {
        return isBlackHoleBlock;
    }
}
