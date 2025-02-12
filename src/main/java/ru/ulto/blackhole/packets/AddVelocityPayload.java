package ru.ulto.blackhole.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import ru.ulto.blackhole.BlackHole;

public record AddVelocityPayload(Vec3d velocity) implements CustomPayload {
    public static final Id<AddVelocityPayload> ID = new Id<>(Identifier.of(BlackHole.MOD_ID, ""));
    public static final PacketCodec<RegistryByteBuf, AddVelocityPayload> CODEC = PacketCodec.of(
            (payload, buf) -> buf.writeDouble(payload.velocity().x).writeDouble(payload.velocity().y).writeDouble(payload.velocity().z),
            buf -> new AddVelocityPayload(new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble())));

    @Override
    public Id<AddVelocityPayload> getId() {
        return ID;
    }
}
