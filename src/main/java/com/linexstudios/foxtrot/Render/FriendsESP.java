package com.linexstudios.foxtrot.Render;

import com.linexstudios.foxtrot.Hud.FriendsHUD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class FriendsESP {
    private final Minecraft mc = Minecraft.getMinecraft();
    public static boolean enabled = true;

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!enabled || mc.theWorld == null || mc.thePlayer == null) return;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player != mc.thePlayer && isFriend(player.getName())) {
                // 1. Render the translucent green fill
                renderFilledBox(player, event.partialTicks);
                // 2. Render the solid green outline
                renderOutlineBox(player, event.partialTicks);
                // 3. Render the green Skeleton Highlighter
                renderSkeleton(player, event.partialTicks);
            }
        }
    }

    private boolean isFriend(String name) {
        return FriendsHUD.friendsList.stream().anyMatch(friend -> friend.equalsIgnoreCase(name));
    }

    private void renderFilledBox(EntityPlayer player, float partialTicks) {
        AxisAlignedBB bb = getInterpolatedBB(player, partialTicks);
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);

        // Green Fill
        drawFilledBoundingBox(bb, 0.3F, 1.0F, 0.3F, 0.4F);

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void renderOutlineBox(EntityPlayer player, float partialTicks) {
        AxisAlignedBB bb = getInterpolatedBB(player, partialTicks);
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);

        // Green Outline
        GlStateManager.color(0.3F, 1.0F, 0.3F, 1.0F);
        drawBoundingBoxOutline(bb);

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void renderSkeleton(EntityPlayer player, float partialTicks) {
        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks - mc.getRenderManager().viewerPosY;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.5F); 
        
        // Green Skeleton
        GL11.glColor4f(0.3F, 1.0F, 0.3F, 1.0F); 

        GL11.glBegin(GL11.GL_LINES);
        
        // Spine (Middle of body)
        GL11.glVertex3d(x, y + 0.4, z);
        GL11.glVertex3d(x, y + 1.6, z);

        // Shoulders (Horizontal)
        GL11.glVertex3d(x - 0.35, y + 1.55, z);
        GL11.glVertex3d(x + 0.35, y + 1.55, z);

        // Arms (Vertical drops from shoulders)
        GL11.glVertex3d(x - 0.35, y + 1.55, z); GL11.glVertex3d(x - 0.35, y + 0.9, z);
        GL11.glVertex3d(x + 0.35, y + 1.55, z); GL11.glVertex3d(x + 0.35, y + 0.9, z);

        // Hips (Horizontal)
        GL11.glVertex3d(x - 0.2, y + 0.7, z);
        GL11.glVertex3d(x + 0.2, y + 0.7, z);

        // Legs (Vertical drops from hips)
        GL11.glVertex3d(x - 0.2, y + 0.7, z); GL11.glVertex3d(x - 0.2, y + 0.1, z);
        GL11.glVertex3d(x + 0.2, y + 0.7, z); GL11.glVertex3d(x + 0.2, y + 0.1, z);

        GL11.glEnd();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private AxisAlignedBB getInterpolatedBB(EntityPlayer player, float partialTicks) {
        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks - mc.getRenderManager().viewerPosY;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;

        float expand = 0.13f;
        float expandy = 0.11f;

        return new AxisAlignedBB(
                x - player.width / 2.0 - expand, y - expandy, z - player.width / 2.0 - expand,
                x + player.width / 2.0 + expand, y + player.height + expandy, z + player.width / 2.0 + expand
        );
    }

    public static void drawFilledBoundingBox(AxisAlignedBB bb, float r, float g, float b, float a) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.color(r, g, b, a);
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        
        // Bottom
        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        // Top
        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        // West
        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        // East
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        // North
        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        // South
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();

        tessellator.draw();
    }

    public static void drawBoundingBoxOutline(AxisAlignedBB bb) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);

        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();

        tessellator.draw();
    }
}