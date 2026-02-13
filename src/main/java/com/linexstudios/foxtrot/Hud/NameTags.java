package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NameTags {
    public static final NameTags instance = new NameTags();
    public static boolean enabled = false;
    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onRenderNametag(RenderLivingEvent.Specials.Pre<EntityPlayer> event) {
        if (!enabled || mc.thePlayer == null || mc.theWorld == null) return;
        
        // We only want to render this for players
        if (!(event.entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.entity;
        
        // Don't render our own nametag or invisible players
        if (player == mc.thePlayer || player.isInvisible()) return;

        // Cancel the vanilla Minecraft nametag so we can replace it
        event.setCanceled(true);

        float distance = mc.thePlayer.getDistanceToEntity(player);
        if (distance > 150.0F) return; // Prevent rendering players across the entire map

        double x = event.x;
        double y = event.y + player.height + 0.45D; // Hover just above their head
        double z = event.z;

        // Dynamic scale so it gets larger the further away they are (readable from far away)
        float baseScale = 0.016666668F * 1.6F;
        float scale = (distance / 5.0F) * baseScale;
        if (scale < 0.025F) scale = 0.025F; // Minimum size so it's readable up close

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);

        // Rotate the text to always face your camera natively
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);

        // GL Setup to render through walls (ESP)
        GlStateManager.disableLighting();
        GlStateManager.disableDepth(); // This is what makes it show through walls
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Build the text string (Prestige, Name, Health)
        String name = player.getDisplayName().getFormattedText();
        float health = player.getHealth() + player.getAbsorptionAmount();
        String healthStr = EnumChatFormatting.GREEN + String.format(Locale.US, "%.1f", health);
        String text = name + " " + healthStr;

        int width = mc.fontRendererObj.getStringWidth(text) / 2;

        // Draw the dark background rectangle behind the text
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.disableTexture2D();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(-width - 2, -2, 0.0D).color(0.0F, 0.0F, 0.0F, 0.5F).endVertex();
        worldrenderer.pos(-width - 2, 9, 0.0D).color(0.0F, 0.0F, 0.0F, 0.5F).endVertex();
        worldrenderer.pos(width + 2, 9, 0.0D).color(0.0F, 0.0F, 0.0F, 0.5F).endVertex();
        worldrenderer.pos(width + 2, -2, 0.0D).color(0.0F, 0.0F, 0.0F, 0.5F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();

        // Draw the text
        mc.fontRendererObj.drawStringWithShadow(text, -width, 0, 0xFFFFFF);

        // Collect Items (Held Item, then Helmet -> Boots)
        List<ItemStack> items = new ArrayList<>();
        if (player.getHeldItem() != null) items.add(player.getHeldItem());
        for (int i = 3; i >= 0; i--) {
            if (player.inventory.armorInventory[i] != null) {
                items.add(player.inventory.armorInventory[i]);
            }
        }

        // Draw Items above the name
        if (!items.isEmpty()) {
            int itemOffset = -(items.size() * 16) / 2;
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, -18, 0); // Move 18 pixels above the text
            
            GlStateManager.enableDepth(); // Depth must be temporarily enabled for standard item lighting
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().zLevel = -150.0F; // Prevent Z-fighting glitches

            for (ItemStack item : items) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(itemOffset, 0, 0);
                
                mc.getRenderItem().renderItemAndEffectIntoGUI(item, 0, 0);
                mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, item, 0, 0);
                
                GlStateManager.popMatrix();
                itemOffset += 16;
            }

            mc.getRenderItem().zLevel = 0.0F;
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth(); // Re-disable depth to restore the ESP state
            GlStateManager.popMatrix();
        }

        // Restore vanilla GL state so we don't break the rest of the game's rendering
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}