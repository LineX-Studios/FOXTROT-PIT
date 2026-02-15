package com.linexstudios.foxtrot.Render;

import com.linexstudios.foxtrot.Denick.CacheManager;
import com.linexstudios.foxtrot.Denick.NickedManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class NickedRender {

    public static boolean enabled = true; 
    
    @SubscribeEvent
    public void onRenderNametag(RenderLivingEvent.Specials.Pre event) {
        if (!enabled) return; 
        
        // Prevent double-rendering! If NameTags module is ON, don't draw this one.
        if (com.linexstudios.foxtrot.Hud.NameTags.enabled) return;
        
        if (!(event.entity instanceof EntityOtherPlayerMP)) return;
        EntityOtherPlayerMP player = (EntityOtherPlayerMP) event.entity;
        
        String nick = player.getName();
        
        boolean isFriend = com.linexstudios.foxtrot.Hud.FriendsHUD.isFriend(nick);
        boolean isEnemy = com.linexstudios.foxtrot.Hud.EnemyHUD.isTarget(nick);
        boolean isNicked = com.linexstudios.foxtrot.Denick.AutoDenick.isNicked(player.getUniqueID());

        // Takes over nametag rendering for any tracked player
        if (isFriend || isEnemy || isNicked) {
            event.setCanceled(true); 
            
            // Grabs the base name
            String formattedName = player.getDisplayName().getFormattedText();

            // Fetch the Real IGN
            String realName = CacheManager.getFromCache(nick);
            if (realName == null) {
                realName = NickedManager.getResolvedIGN(nick);
            }

            // EXACT MATCH: Ignores "Scraping" (no dots), "Failed", and "No Nonce"
            if (realName != null && !realName.equals("Scraping") && !realName.equals("Scraping...") && !realName.equals("Failed") && !realName.equals("No Nonce")) {
                if (!formattedName.contains("(" + realName + ")")) {
                    formattedName = formattedName + " " + EnumChatFormatting.YELLOW + "(" + realName + ")";
                }
            }
            
            // STRICT FAIL-SAFE: Forcibly delete status texts so they NEVER render over their head
            // Targets exactly "(Scraping)" without the dots
            formattedName = formattedName.replace(EnumChatFormatting.YELLOW + " (Scraping)", "");
            formattedName = formattedName.replace(EnumChatFormatting.YELLOW + " (Failed)", "");
            formattedName = formattedName.replace(EnumChatFormatting.YELLOW + " (No Nonce)", "");
            formattedName = formattedName.replace(" (Scraping)", "");
            formattedName = formattedName.replace(" (Failed)", "");
            formattedName = formattedName.replace(" (No Nonce)", "");

            renderVanillaNametag(player, formattedName, event.x, event.y, event.z);
        }
    }

    private void renderVanillaNametag(EntityLivingBase entity, String name, double x, double y, double z) {
        double distanceSq = entity.getDistanceSqToEntity(Minecraft.getMinecraft().thePlayer);
        if (distanceSq > 4096.0D) return;

        FontRenderer fontrenderer = Minecraft.getMinecraft().fontRendererObj;
        float f = 1.6F;
        float f1 = 0.016666668F * f;
        
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)x + 0.0F, (float)y + entity.height + 0.5F, (float)z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(Minecraft.getMinecraft().getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-f1, -f1, f1);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        int i = 0;
        int j = fontrenderer.getStringWidth(name) / 2;
        GlStateManager.disableTexture2D();
        
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos((double)(-j - 1), (double)(-1 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos((double)(-j - 1), (double)(8 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos((double)(j + 1), (double)(8 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos((double)(j + 1), (double)(-1 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tessellator.draw();
        
        GlStateManager.enableTexture2D();
        
        fontrenderer.drawString(name, -fontrenderer.getStringWidth(name) / 2, i, 553648127);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        
        fontrenderer.drawString(name, -fontrenderer.getStringWidth(name) / 2, i, -1);
        
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}