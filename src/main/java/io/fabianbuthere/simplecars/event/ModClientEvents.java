package io.fabianbuthere.simplecars.event;

import io.fabianbuthere.simplecars.SimplecarsMod;
import io.fabianbuthere.simplecars.entity.custom.BaseCarEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SimplecarsMod.MOD_ID, value = Dist.CLIENT)
public class ModClientEvents {
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
}
