package ru.ulto.blackhole.accessors;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerEntityManager;

public interface EntityManagerAccessor {
    ServerEntityManager<Entity> getEntityManager();
}
