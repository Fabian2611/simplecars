package io.fabianbuthere.simplecars.network;

import io.fabianbuthere.simplecars.SimplecarsMod;
import io.fabianbuthere.simplecars.entity.custom.BaseCarEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

@SuppressWarnings("removal")
public class ModMessages {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SimplecarsMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        INSTANCE.registerMessage(id(), BrakingC2SPacket.class,
                BrakingC2SPacket::encode,
                BrakingC2SPacket::new,
                BrakingC2SPacket::handle);
    }

    public static class BrakingC2SPacket {
        private final boolean braking;
        private final int entityId;

        public BrakingC2SPacket(boolean braking, int entityId) {
            this.braking = braking;
            this.entityId = entityId;
        }

        public BrakingC2SPacket(FriendlyByteBuf buf) {
            this.braking = buf.readBoolean();
            this.entityId = buf.readInt();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeBoolean(braking);
            buf.writeInt(entityId);
        }

        public boolean handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                // On the server
                ServerPlayer player = context.getSender();
                if (player != null) {
                    Entity entity = player.level().getEntity(entityId);
                    if (entity instanceof BaseCarEntity car && player.getVehicle() == car) {
                        // Only allow if the player is actually riding this car
                        car.setBraking(braking);
                        SimplecarsMod.LOGGER.debug("Server received braking packet: {} for entity {}", braking, entityId);
                    }
                }
            });
            context.setPacketHandled(true);
            return true;
        }
    }
}