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
        
        if (!(event.entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.entity;
        
        if (player == mc.thePlayer || player.isInvisible()) return;

        // Cancel the vanilla Minecraft nametag so we can replace it
        event.setCanceled(true);

        float distance = mc.thePlayer.getDistanceToEntity(player);
        
        // CLUTTER FIX 1: Don't render anything past 60 blocks (saves massive FPS)
        if (distance > 60.0F) return; 

        double x = event.x;
        double y = event.y + player.height + 0.45D;
        double z = event.z;

        // Smoother scaling so it doesn't get huge and block your screen up close
        float scale = Math.max(0.025F, distance / 8.0F * 0.02F);

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);

        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);

        // GL Setup to render through walls (ESP)
        GlStateManager.disableLighting();
        GlStateManager.disableDepth(); 
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        // FLICKER FIX: Stops the background boxes from Z-fighting with each other
        GlStateManager.depthMask(false); 

        // Build the text string
        String name = player.getDisplayName().getFormattedText();
        float health = player.getHealth() + player.getAbsorptionAmount();
        
        // Dynamic health colors
        String healthColor = health > 15 ? EnumChatFormatting.GREEN.toString() : (health > 7 ? EnumChatFormatting.YELLOW.toString() : EnumChatFormatting.RED.toString());
        String healthStr = healthColor + String.format(Locale.US, "%.1f", health);
        String text = name + " " + healthStr;

        int width = mc.fontRendererObj.getStringWidth(text) / 2;

        // Draw the dark background rectangle
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

        // CLUTTER FIX 2: Only show Armor and Weapons for players within 15 blocks
        if (distance <= 15.0F) {
            List<ItemStack> items = new ArrayList<>();
            if (player.getHeldItem() != null) items.add(player.getHeldItem());
            for (int i = 3; i >= 0; i--) {
                if (player.inventory.armorInventory[i] != null) {
                    items.add(player.inventory.armorInventory[i]);
                }
            }

            if (!items.isEmpty()) {
                int itemOffset = -(items.size() * 16) / 2;
                GlStateManager.pushMatrix();
                GlStateManager.translate(0, -18, 0); 
                
                // Isolates the item lighting so it doesn't break the world
                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                RenderHelper.enableGUIStandardItemLighting();
                GlStateManager.enableDepth(); // Depth must be ON for items to look 3D

                for (ItemStack item : items) {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(itemOffset, 0, 0);
                    
                    mc.getRenderItem().renderItemAndEffectIntoGUI(item, 0, 0);
                    mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, item, 0, 0);
                    
                    GlStateManager.popMatrix();
                    itemOffset += 16;
                }

                // Restores whatever lighting state we had before drawing the items
                GL11.glPopAttrib(); 
                GlStateManager.popMatrix();
            }
        }

        // Restore vanilla GL state
        GlStateManager.depthMask(true); // Re-enable depth mask
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        // LIGHTING BUG FIX: This tells Minecraft to turn the 3D sun/shadows back on
        RenderHelper.enableStandardItemLighting(); 

        GlStateManager.popMatrix();
    }
}