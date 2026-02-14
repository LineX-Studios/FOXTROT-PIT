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
    
    // --- THE NEW SETTINGS VARIABLES ---
    public static boolean enabled = false;
    public static boolean showHealth = true;
    public static boolean showItems = true;
    public static boolean itemsThroughWalls = false;
    
    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onRenderNametag(RenderLivingEvent.Specials.Pre<EntityPlayer> event) {
        if (!enabled || mc.thePlayer == null || mc.theWorld == null) return;
        if (!(event.entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.entity;
        if (player == mc.thePlayer || player.isInvisible()) return;

        // Cancel the vanilla Minecraft nametag so we can replace it completely
        event.setCanceled(true);

        float distance = mc.thePlayer.getDistanceToEntity(player);
        double x = event.x;
        double y = event.y + player.height + 0.45D;
        double z = event.z;

        // Smoother scaling so it stays a readable size no matter how far away they are
        float scale = Math.max(0.025F, distance / 8.0F * 0.02F);

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);

        // --- ESP TEXT STATE (Visible Through Walls) ---
        GlStateManager.disableLighting();
        GlStateManager.disableDepth(); // Turns OFF depth so text ignores walls
        GlStateManager.depthMask(false); 
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Build the text string
        String text = player.getDisplayName().getFormattedText();
        
        // HEALTH TOGGLE LOGIC
        if (showHealth) {
            float health = player.getHealth() + player.getAbsorptionAmount();
            String healthColor = health > 15 ? EnumChatFormatting.GREEN.toString() : (health > 7 ? EnumChatFormatting.YELLOW.toString() : EnumChatFormatting.RED.toString());
            text += " " + healthColor + String.format(Locale.US, "%.1f", health);
        }

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

        // --- ITEM TOGGLE LOGIC ---
        if (showItems) {
            List<ItemStack> items = new ArrayList<>();
            if (player.getHeldItem() != null) items.add(player.getHeldItem());
            for (int i = 3; i >= 0; i--) {
                if (player.inventory.armorInventory[i] != null) {
                    items.add(player.inventory.armorInventory[i]);
                }
            }

            if (!items.isEmpty()) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0, -18, 0); // Move above the text
                
                // ITEMS THROUGH WALLS TOGGLE LOGIC
                if (itemsThroughWalls) {
                    GlStateManager.depthFunc(GL11.GL_ALWAYS); // Renders through blocks
                } else {
                    GlStateManager.enableDepth(); // Hides behind blocks
                    GlStateManager.depthMask(true);
                }

                int itemOffset = -(items.size() * 16) / 2;
                for (ItemStack item : items) {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(itemOffset, 0, 0);
                    
                    RenderHelper.enableGUIStandardItemLighting();
                    mc.getRenderItem().renderItemAndEffectIntoGUI(item, 0, 0);
                    mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, item, 0, 0);
                    RenderHelper.disableStandardItemLighting();
                    
                    GlStateManager.popMatrix();
                    itemOffset += 16;
                }

                GlStateManager.depthFunc(GL11.GL_LEQUAL); // Reset depth function
                GlStateManager.popMatrix();
            }
        }

        // --- RESTORE VANILLA STATE ---
        GlStateManager.depthMask(true); 
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        GlStateManager.popMatrix();
    }
}