package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class NameTags {
    public static final NameTags instance = new NameTags();
    private final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = false;
    public static boolean showHealth = true;
    public static boolean showItems = true;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!enabled || mc.thePlayer == null || mc.theWorld == null) return;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            // FIX: Strictly ignore the local player so you never see your own tag
            if (player == mc.thePlayer) continue;
            
            if (player.isDead || player.isInvisible()) continue; 

            // Calculate precise interpolated positions
            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks - mc.getRenderManager().viewerPosX;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks - mc.getRenderManager().viewerPosY;
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks - mc.getRenderManager().viewerPosZ;

            renderNameTag(player, x, y, z);
        }
    }

    private void renderNameTag(EntityPlayer player, double x, double y, double z) {
        float distance = mc.thePlayer.getDistanceToEntity(player);
        float scale = (distance / 4.0F) * 0.015F;
        if (scale < 0.020F) scale = 0.020F; 

        GlStateManager.pushMatrix();
        GlStateManager.translate((float)x, (float)y + player.height + 0.6F, (float)z);
        
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);

        GlStateManager.disableLighting();
        GlStateManager.disableDepth(); 
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);

        String name = player.getDisplayName().getFormattedText();
        if (showHealth) {
            float health = player.getHealth() + player.getAbsorptionAmount();
            float maxHealth = player.getMaxHealth();
            float percentage = health / maxHealth;

            String colorCode = "\u00a7a"; 
            if (percentage <= 0.75f) colorCode = "\u00a7e"; 
            if (percentage <= 0.50f) colorCode = "\u00a76"; 
            if (percentage <= 0.25f) colorCode = "\u00a7c"; 

            name = name + " " + colorCode + String.format("%.1f", health);
        }

        int width = mc.fontRendererObj.getStringWidth(name) / 2;

        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        
        worldrenderer.pos(-width - 2, -2, 0.0D).color(0.0F, 0.0F, 0.0F, 0.5F).endVertex();
        worldrenderer.pos(-width - 2, 11, 0.0D).color(0.0F, 0.0F, 0.0F, 0.5F).endVertex();
        worldrenderer.pos(width + 2, 11, 0.0D).color(0.0F, 0.0F, 0.0F, 0.5F).endVertex();
        worldrenderer.pos(width + 2, -2, 0.0D).color(0.0F, 0.0F, 0.0F, 0.5F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();

        mc.fontRendererObj.drawStringWithShadow(name, -width, 0, -1);

        if (showItems) {
            List<ItemStack> items = new ArrayList<>();
            if (player.getHeldItem() != null) items.add(player.getHeldItem());
            
            for (int i = 3; i >= 0; i--) { 
                ItemStack armor = player.inventory.armorInventory[i];
                if (armor != null) items.add(armor);
            }

            if (!items.isEmpty()) {
                int startX = -(items.size() * 16) / 2;
                int itemY = -18; 

                // FIX: Temporarily enable depth to prevent item rendering from bugging the GUI/Overlay
                GlStateManager.enableDepth();
                RenderHelper.enableGUIStandardItemLighting();

                for (ItemStack item : items) {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(startX, itemY, 0);
                    GlStateManager.scale(1.0F, 1.0F, 0.01F);
                    
                    float prevZ = mc.getRenderItem().zLevel;
                    mc.getRenderItem().zLevel = -100.0F; 

                    mc.getRenderItem().renderItemIntoGUI(item, 0, 0);
                    mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, item, 0, 0);
                    
                    mc.getRenderItem().zLevel = prevZ;
                    GlStateManager.popMatrix();
                    startX += 16;
                }

                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableDepth(); // Re-disable to maintain Nametag priority
            }
        }

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onRenderVanillaNametag(RenderLivingEvent.Specials.Pre event) {
        // Cancel default nametags for all players if our module is active
        if (enabled && event.entity instanceof EntityPlayer) {
            event.setCanceled(true);
        }
    }
}