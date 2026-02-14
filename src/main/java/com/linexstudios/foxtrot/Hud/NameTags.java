package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
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
            // Don't render on ourselves unless we are in F5 mode
            if (player == mc.thePlayer && mc.gameSettings.thirdPersonView == 0) continue;
            if (player.isDead || player.isInvisible()) continue; 

            // Calculate precise interpolated positions for smooth rendering
            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks - mc.getRenderManager().viewerPosX;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks - mc.getRenderManager().viewerPosY;
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks - mc.getRenderManager().viewerPosZ;

            renderNameTag(player, x, y, z);
        }
    }

    private void renderNameTag(EntityPlayer player, double x, double y, double z) {
        // --- 1. DISTANCE SCALING ---
        float distance = mc.thePlayer.getDistanceToEntity(player);
        float scale = (distance / 4.0F) * 0.015F;
        if (scale < 0.025F) scale = 0.025F; // Keeps it readable when very close

        // Save GL State and setup positioning
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)x, (float)y + player.height + 0.6F, (float)z);
        
        // Make the tag constantly face the camera perfectly
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);

        // --- 2. FIX LIGHTING & WALL HACKS ---
        GlStateManager.disableLighting();
        GlStateManager.disableDepth(); // Renders through walls
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);

        // --- 3. BUILD TEXT (Preserves server prefixes like [107]) ---
        String name = player.getDisplayName().getFormattedText();
        if (showHealth) {
            float health = player.getHealth() + player.getAbsorptionAmount();
            String colorCode = "\u00a7a"; // Default Green
            if (health <= player.getMaxHealth() * 0.5f) colorCode = "\u00a7e"; // Yellow
            if (health <= player.getMaxHealth() * 0.25f) colorCode = "\u00a7c"; // Red
            name = name + " " + colorCode + String.format("%.1f", health);
        }

        int width = mc.fontRendererObj.getStringWidth(name) / 2;

        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        // Draws a black box with 0.5 Alpha (Semi-transparent)
        worldrenderer.pos(-width - 2, -2, 0.0D).color(0.0F, 0.0F, 0.0F, 0.5F).endVertex();
        worldrenderer.pos(-width - 2, 9, 0.0D).color(0.0F, 0.0F, 0.0F, 0.5F).endVertex();
        worldrenderer.pos(width + 2, 9, 0.0D).color(0.0F, 0.0F, 0.0F, 0.5F).endVertex();
        worldrenderer.pos(width + 2, -2, 0.0D).color(0.0F, 0.0F, 0.0F, 0.5F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();

        // --- 5. RENDER TEXT ---
        mc.fontRendererObj.drawStringWithShadow(name, -width, 0, -1);

        // --- 6. RENDER ARMOR & HELD ITEM ---
        if (showItems) {
            List<ItemStack> items = new ArrayList<>();
            // Add Held Item first (Left side)
            if (player.getHeldItem() != null) items.add(player.getHeldItem());
            
            // Add Armor from Helmet down to Boots
            for (int i = 3; i >= 0; i--) { 
                ItemStack armor = player.inventory.armorInventory[i];
                if (armor != null) items.add(armor);
            }

            if (!items.isEmpty()) {
                int startX = -(items.size() * 16) / 2;
                int itemY = -18; // Offset perfectly above the text box

                for (ItemStack item : items) {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(startX, itemY, 0);
                    GlStateManager.scale(1.0F, 1.0F, 1.0F);

                    // Re-enable lighting JUST for the 3D items so they don't render black
                    RenderHelper.enableGUIStandardItemLighting();
                    mc.getRenderItem().renderItemIntoGUI(item, 0, 0);
                    mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, item, 0, 0);
                    RenderHelper.disableStandardItemLighting();

                    GlStateManager.popMatrix();
                    startX += 16;
                }
            }
        }

        // --- 7. RESTORE GL STATE ---
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}