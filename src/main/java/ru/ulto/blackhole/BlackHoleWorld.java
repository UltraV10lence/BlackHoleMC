package ru.ulto.blackhole;

import net.minecraft.util.math.BlockPos;

public interface BlackHoleWorld {
    void initializeBlackHole(BlockPos center);
    void startBlackHole();
    void stopBlackHole();
    boolean hasBlackHole();
    BlackHoleController getController();
}
