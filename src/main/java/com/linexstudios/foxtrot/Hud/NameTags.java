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
    public static boolean showHealth = true;
    public static boolean showItems = true;
    public static boolean itemsThroughWalls = true; // Set to TRUE by default for ESP
    
    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onRenderNametag(RenderLivingEvent.Specials.Pre<EntityPlayer> event) {
        if (!enabled || mc.thePlayer == null || mc.theWorld == null) return;
        if (!(event.entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.entity;
        if (player == mc.thePlayer || player.isInvisible()) return;

        event.setCanceled(true); // Cancels vanilla nametag

        double x = event.x;
        double y = event.y + player.height + 0.5D;
        double z = event.z;

        float distance = mc.thePlayer.getDistanceToEntity(player);
        // Map-wide scaling
        float scale = Math.max(0.025F, distance / 8.0F * 0.02F);

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);

        // --- GLOBAL ESP STATE ---
        GlStateManager.disableLighting();
        GlStateManager.disableDepth(); // This ensures everything renders THROUGH walls
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Name and Health Text
        String text = player.getDisplayName().getFormattedText();
        if (showHealth) {
            float health = player.getHealth() + player.getAbsorptionAmount();
            String healthColor = health > 15 ? EnumChatFormatting.GREEN.toString() : (health > 7 ? EnumChatFormatting.YELLOW.toString() : EnumChatFormatting.RED.toString());
            text += " " + healthColor + String.format(Locale.US, "%.1f", health);
        }

        int width = mc.fontRendererObj.getStringWidth(text) / 2;

        // Background Box
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
        mc.fontRendererObj.drawStringWithShadow(text, -width, 0, 0xFFFFFF);

        // --- STUCK ARMOR RENDERING ---
        if (showItems) {
            List<ItemStack> items = new ArrayList<>();
            if (player.getHeldItem() != null) items.add(player.getHeldItem());
            for (int i = 3; i >= 0; i--) if (player.inventory.armorInventory[i] != null) items.add(player.inventory.armorInventory[i]);

            if (!items.isEmpty()) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0, -18, 0); // Stuck directly above text
                
                // ESP Logic for items
                if (itemsThroughWalls) GlStateManager.disableDepth();
                else GlStateManager.enableDepth();

                int itemOffset = -(items.size() * 16) / 2;
                for (ItemStack item : items) {
                    RenderHelper.enableGUIStandardItemLighting();
                    mc.getRenderItem().renderItemAndEffectIntoGUI(item, itemOffset, 0);
                    mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, item, itemOffset, 0);
                    itemOffset += 16;
                }
                GlStateManager.popMatrix();
            }
        }

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.popMatrix();
    }
}