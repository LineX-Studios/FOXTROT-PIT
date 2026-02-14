package com.linexstudios.foxtrot.Render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class ChestESP {
    public static final ChestESP instance = new ChestESP();
    public static boolean enabled = false;
    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!enabled || mc.theWorld == null || mc.thePlayer == null) return;

        for (TileEntity tileEntity : mc.theWorld.loadedTileEntityList) {
            if (tileEntity instanceof TileEntityChest || tileEntity instanceof TileEntityEnderChest) {
                double x = tileEntity.getPos().getX() - mc.getRenderManager().viewerPosX;
                double y = tileEntity.getPos().getY() - mc.getRenderManager().viewerPosY;
                double z = tileEntity.getPos().getZ() - mc.getRenderManager().viewerPosZ;

                AxisAlignedBB bbox = new AxisAlignedBB(
                        x + 0.0625, y, z + 0.0625, 
                        x + 0.9375, y + 0.875, z + 0.9375
                );

                GlStateManager.pushMatrix();
                GlStateManager.enableBlend();
                GlStateManager.disableDepth();
                GlStateManager.depthMask(false);
                GlStateManager.disableTexture2D();
                GlStateManager.disableLighting();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                // FILL COLOR LOGIC
                if (tileEntity instanceof TileEntityEnderChest) {
                    GlStateManager.color(0.6F, 0.0F, 0.8F, 0.3F); // Purple
                } else {
                    GlStateManager.color(0.9F, 0.0F, 0.0F, 0.3F); // RED (R: 0.9, G: 0.0, B: 0.0)
                }
                drawSolidBox(bbox); 

                // OUTLINE COLOR LOGIC
                GL11.glLineWidth(2.0F);
                if (tileEntity instanceof TileEntityEnderChest) {
                    GlStateManager.color(0.6F, 0.0F, 0.8F, 1.0F); // Solid Purple
                } else {
                    GlStateManager.color(1.0F, 0.0F, 0.0F, 1.0F); // Solid RED
                }
                RenderGlobal.drawSelectionBoundingBox(bbox); 

                GlStateManager.depthMask(true);
                GlStateManager.enableLighting();
                GlStateManager.enableTexture2D();
                GlStateManager.enableDepth();
                GlStateManager.disableBlend();
                GlStateManager.popMatrix();
            }
        }
    }

    private void drawSolidBox(AxisAlignedBB bb) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();

        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();

        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();

        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        tessellator.draw();
    }
}