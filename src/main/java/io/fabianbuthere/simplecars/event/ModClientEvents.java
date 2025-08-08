package io.fabianbuthere.simplecars.event;

import com.mojang.blaze3d.platform.InputConstants;
import io.fabianbuthere.simplecars.SimplecarsMod;
import io.fabianbuthere.simplecars.entity.custom.BaseCarEntity;
import io.fabianbuthere.simplecars.network.ModMessages;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = SimplecarsMod.MOD_ID, value = Dist.CLIENT)
public class ModClientEvents {
    public static final String SIMPLECARS_CATEGORY = "key.categories.simplecars";
    public static KeyMapping CAR_BRAKE_KEY = new KeyMapping("key.simplecars.car_brake", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_SPACE, SIMPLECARS_CATEGORY);

    // Map to track client-side state
    private static final Map<String, Boolean> CLIENT_STATE = new HashMap<>();

    @SubscribeEvent
    public static void onRenderGUIOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player != null && player.getVehicle() instanceof BaseCarEntity car) {
            double speed = Math.abs(car.getCarForwardVelocity());
            double speedBps = speed * 20.0; // blocks/second

            String speedText = String.format("Speed: %5.2f m/s", speedBps);

            // Draw the text (bottom left)
            int x = 10;
            int y = event.getWindow().getGuiScaledHeight() - 30;
            event.getGuiGraphics().drawString(mc.font, speedText, x, y, 0xFFFFFF, false);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getVehicle() instanceof BaseCarEntity car) {
                // Get the current key state
                boolean currentBraking = CAR_BRAKE_KEY.isDown();

                // Store previous key state in a static field
                if (!hasField("prevBraking")) {
                    setField("prevBraking", false);
                }
                boolean prevBraking = getField("prevBraking");

                // Only send a packet if the state has changed
                if (currentBraking != prevBraking) {
                    SimplecarsMod.LOGGER.debug("Sending braking packet: {}", currentBraking);
                    ModMessages.INSTANCE.sendToServer(
                            new ModMessages.BrakingC2SPacket(currentBraking, car.getId())
                    );
                    // Update the local state too (for client-side prediction)
                    car.setBraking(currentBraking);
                    // Store the new state
                    setField("prevBraking", currentBraking);
                }
            } else {
                // Reset the state when not in a car
                setField("prevBraking", false);
            }
        }
    }

    // Helper methods for storing state
    private static boolean hasField(String name) {
        return CLIENT_STATE.containsKey(name);
    }

    private static boolean getField(String name) {
        return CLIENT_STATE.getOrDefault(name, false);
    }

    private static void setField(String name, boolean value) {
        CLIENT_STATE.put(name, value);
    }
}