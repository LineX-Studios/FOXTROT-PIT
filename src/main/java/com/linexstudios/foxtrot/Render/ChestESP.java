package com.linexstudios.foxtrot.Render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
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
                
                // Get exact coordinates relative to the player's camera
                double x = tileEntity.getPos().getX() - mc.getRenderManager().viewerPosX;
                double y = tileEntity.getPos().getY() - mc.getRenderManager().viewerPosY;
                double z = tileEntity.getPos().getZ() - mc.getRenderManager().viewerPosZ;

                // Chests are slightly smaller than a full block (0.875 height, 0.0625 inset)
                AxisAlignedBB bbox = new AxisAlignedBB(
                        x + 0.0625, y, z + 0.0625, 
                        x + 0.9375, y + 0.875, z + 0.9375
                );

                // Setup OpenGL for transparent rendering
                GlStateManager.pushMatrix();
                GlStateManager.enableBlend();
                GlStateManager.disableDepth(); // Renders through walls
                GlStateManager.disableTexture2D();
                GlStateManager.disableLighting();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                // Color logic: Ender Chests = Purple, Normal Chests = Green
                if (tileEntity instanceof TileEntityEnderChest) {
                    GlStateManager.color(0.6F, 0.0F, 0.8F, 0.3F); // Purple Fill
                } else {
                    GlStateManager.color(0.0F, 0.8F, 0.0F, 0.3F); // Green Fill
                }

                // Draw solid inner box
                RenderGlobal.drawSelectionBoundingBox(bbox);

                // Draw Outline
                GL11.glLineWidth(2.0F);
                if (tileEntity instanceof TileEntityEnderChest) {
                    GlStateManager.color(0.6F, 0.0F, 0.8F, 1.0F); // Purple Outline
                } else {
                    GlStateManager.color(0.0F, 0.8F, 0.0F, 1.0F); // Green Outline
                }
                RenderGlobal.drawSelectionBoundingBox(bbox);

                // Restore OpenGL settings
                GlStateManager.enableLighting();
                GlStateManager.enableTexture2D();
                GlStateManager.enableDepth();
                GlStateManager.disableBlend();
                GlStateManager.popMatrix();
            }
        }
    }
}