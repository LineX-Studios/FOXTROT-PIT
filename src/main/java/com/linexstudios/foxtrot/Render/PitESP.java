package com.linexstudios.foxtrot.Render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemNameTag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class PitESP {
    public static final PitESP instance = new PitESP();
    private final Minecraft mc = Minecraft.getMinecraft();

    // --- GUI Toggles ---
    public static boolean espChests = true; 
    public static boolean espDragonEggs = true;
    public static boolean espRaffleTickets = true;
    public static boolean espMystics = true;

    private final List<BlockPos> dragonEggs = new ArrayList<>();
    private int scanTimer = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.theWorld == null || mc.thePlayer == null) return;
        
        // Dragon Egg Block Scanner - INSTANT (Zero Delay)
        if (espDragonEggs) {
            scanTimer++;
            // Scan every 2 ticks (0.1 seconds) for instant updates without lagging out the game
            if (scanTimer >= 2) {
                scanTimer = 0;
                dragonEggs.clear();
                
                int radiusX = 70; // Massive horizontal radius
                int radiusY = 10; // Tight vertical radius to prevent FPS drops
                
                BlockPos playerPos = mc.thePlayer.getPosition();
                
                // Optimized 3D loop
                for (int x = -radiusX; x <= radiusX; x++) {
                    for (int y = -radiusY; y <= radiusY; y++) {
                        for (int z = -radiusX; z <= radiusX; z++) {
                            BlockPos pos = playerPos.add(x, y, z);
                            // Fast check to see if block is the Dragon Egg
                            if (mc.theWorld.getBlockState(pos).getBlock() == Blocks.dragon_egg) {
                                dragonEggs.add(pos);
                            }
                        }
                    }
                }
            }
        } else {
            if (!dragonEggs.isEmpty()) dragonEggs.clear();
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        double renderPosX = mc.getRenderManager().viewerPosX;
        double renderPosY = mc.getRenderManager().viewerPosY;
        double renderPosZ = mc.getRenderManager().viewerPosZ;

        ICamera camera = new Frustum();
        camera.setPosition(renderPosX, renderPosY, renderPosZ);

        // 1. Sewer Chests (Solid Red Overlay + Outline)
        if (espChests) {
            for (TileEntity tile : mc.theWorld.loadedTileEntityList) {
                if (tile instanceof TileEntityChest) {
                    BlockPos pos = tile.getPos();
                    
                    // Chests are exactly 0.875 blocks tall, and pushed in 0.0625 from the edges
                    AxisAlignedBB bb = new AxisAlignedBB(
                            pos.getX() - renderPosX + 0.0625, pos.getY() - renderPosY, pos.getZ() - renderPosZ + 0.0625,
                            pos.getX() + 0.9375 - renderPosX, pos.getY() + 0.875 - renderPosY, pos.getZ() + 0.9375 - renderPosZ
                        );
                        
                    if (camera.isBoundingBoxInFrustum(bb.offset(renderPosX, renderPosY, renderPosZ))) {
                        drawFilledAndOutlinedBox(bb, 255, 0, 0); // PERFECT RED
                    }
                }
            }
        }

        // 2. Dropped Entities (INFINITE RANGE, SHAPE + NAMETAG)
        if (espRaffleTickets || espMystics) {
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityItem) {
                    if (!camera.isBoundingBoxInFrustum(entity.getEntityBoundingBox())) continue;

                    EntityItem itemEntity = (EntityItem) entity;
                    ItemStack stack = itemEntity.getEntityItem();
                    if (stack == null || stack.getItem() == null) continue;

                    // Raffle Tickets (Orange)
                    if (espRaffleTickets && stack.getItem() instanceof ItemNameTag) {
                        renderItemESP(entity, event.partialTicks, 0xFFFFAA00, "Raffle Ticket");
                        continue;
                    }

                    // Mystics (Yellow)
                    if (espMystics && stack.hasTagCompound()) {
                        NBTTagCompound nbt = stack.getTagCompound();
                        if (nbt.hasKey("ExtraAttributes")) {
                            NBTTagCompound extra = nbt.getCompoundTag("ExtraAttributes");
                            if (extra.hasKey("Nonce") || extra.hasKey("CustomEnchants")) {
                                renderItemESP(entity, event.partialTicks, 0xFFFFFF55, "Mystic Drop");
                            }
                        }
                    }
                }
            }
        }

        // 3. Dragon Eggs (Purple Box)
        if (espDragonEggs) {
            for (BlockPos pos : dragonEggs) {
                AxisAlignedBB bb = new AxisAlignedBB(
                        pos.getX() - renderPosX + 0.0625, pos.getY() - renderPosY, pos.getZ() - renderPosZ + 0.0625,
                        pos.getX() + 0.9375 - renderPosX, pos.getY() + 1 - renderPosY, pos.getZ() + 0.9375 - renderPosZ
                    );
                if (camera.isBoundingBoxInFrustum(bb.offset(renderPosX, renderPosY, renderPosZ))) {
                    drawFilledAndOutlinedBox(bb, 170, 0, 255); 
                }
            }
        }
    }

    /**
     * Renders the real item shape through walls, plus the colored text above it.
     */
    private void renderItemESP(Entity entity, float partialTicks, int hexColor, String label) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - mc.getRenderManager().viewerPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();

        // Force Wallhack
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        mc.getRenderManager().setRenderShadow(false);

        // Rendering the entity NORMALLY keeps the true shape and stops the "White Square" bug
        mc.getRenderManager().doRenderEntity(entity, x, y, z, entity.rotationYaw, partialTicks, false);

        mc.getRenderManager().setRenderShadow(true);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();

        // Draw floating text
        renderFloatingText(label, x, y + 0.6, z, hexColor);
    }

    private void renderFloatingText(String text, double x, double y, double z, int color) {
        FontRenderer fontrenderer = mc.fontRendererObj;
        float f = 1.6F;
        float f1 = 0.016666668F * f;
        
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-f1, -f1, f1);
        
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int textWidth = fontrenderer.getStringWidth(text) / 2;
        fontrenderer.drawStringWithShadow(text, -textWidth, 0, color);

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void drawFilledAndOutlinedBox(AxisAlignedBB bb, int r, int g, int b) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth(); 
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.color(r / 255.0F, g / 255.0F, b / 255.0F, 60 / 255.0F); // Transparent inner fill

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
        worldrenderer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        worldrenderer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        worldrenderer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        tessellator.draw();

        GL11.glLineWidth(2.5F);
        GlStateManager.color(r / 255.0F, g / 255.0F, b / 255.0F, 1.0F); // Solid outer outline
        RenderGlobal.drawOutlinedBoundingBox(bb, r, g, b, 255);
        GL11.glLineWidth(1.0F); 

        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}