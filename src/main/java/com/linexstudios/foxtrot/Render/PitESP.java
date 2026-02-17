package com.linexstudios.foxtrot.Render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
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
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

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

    // Local scanner for the Dragon Egg block
    private final List<BlockPos> dragonEggs = new ArrayList<>();
    private int scanTimer = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.theWorld == null || mc.thePlayer == null) return;
        
        // Dragon Egg Block Scanner (Runs twice a second to prevent lag)
        if (espDragonEggs) {
            scanTimer++;
            if (scanTimer >= 10) {
                scanTimer = 0;
                dragonEggs.clear();
                
                // Scans a 30x15x30 area around the player for the Egg
                int radius = 30;
                BlockPos playerPos = mc.thePlayer.getPosition();
                
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -15; y <= 15; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            BlockPos pos = playerPos.add(x, y, z);
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

        // Get the exact camera position so the ESP boxes don't shake when moving
        double renderPosX = mc.getRenderManager().viewerPosX;
        double renderPosY = mc.getRenderManager().viewerPosY;
        double renderPosZ = mc.getRenderManager().viewerPosZ;

        // 1. Tile Entities (Strictly Standard Chests for Sewers, NO Ender Chests)
        if (espChests) {
            for (TileEntity tile : mc.theWorld.loadedTileEntityList) {
                if (tile instanceof TileEntityChest) {
                    BlockPos pos = tile.getPos();
                    AxisAlignedBB bb = new AxisAlignedBB(
                            pos.getX() - renderPosX, pos.getY() - renderPosY, pos.getZ() - renderPosZ,
                            pos.getX() + 1 - renderPosX, pos.getY() + 1 - renderPosY, pos.getZ() + 1 - renderPosZ
                    );
                    // Green box for Lootable Sewer Chests
                    drawOutlinedBox(bb, 0, 255, 0, 200);
                }
            }
        }

        // 2. Dragon Eggs (From the local block scanner)
        if (espDragonEggs) {
            for (BlockPos pos : dragonEggs) {
                // The dragon egg model is slightly smaller than a full block, so we shrink the hitbox
                AxisAlignedBB bb = new AxisAlignedBB(
                        pos.getX() - renderPosX + 0.0625, pos.getY() - renderPosY, pos.getZ() - renderPosZ + 0.0625,
                        pos.getX() + 0.9375 - renderPosX, pos.getY() + 1 - renderPosY, pos.getZ() + 0.9375 - renderPosZ
                    );
                // Magenta/Purple box for Dragon Egg
                drawOutlinedBox(bb, 255, 0, 255, 200);
            }
        }

        // 3. Dropped Entities (Raffle Tickets & Mystics)
        if (espRaffleTickets || espMystics) {
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityItem) {
                    EntityItem itemEntity = (EntityItem) entity;
                    ItemStack stack = itemEntity.getEntityItem();
                    if (stack == null || stack.getItem() == null) continue;

                    // Match Raffle Tickets (Dropped Name Tags)
                    if (espRaffleTickets && stack.getItem() instanceof ItemNameTag) {
                        AxisAlignedBB bb = entity.getEntityBoundingBox().offset(-renderPosX, -renderPosY, -renderPosZ);
                        // Gold box for tickets
                        drawOutlinedBox(bb, 255, 215, 0, 255);
                        continue;
                    }

                    // Match Mystics (Checking for the 'Nonce' or 'CustomEnchants' NBT tag)
                    if (espMystics && stack.hasTagCompound()) {
                        NBTTagCompound nbt = stack.getTagCompound();
                        if (nbt.hasKey("ExtraAttributes")) {
                            NBTTagCompound extra = nbt.getCompoundTag("ExtraAttributes");
                            // If it has a Nonce or Enchants, it's definitely a Pit Mystic/Fresh item
                            if (extra.hasKey("Nonce") || extra.hasKey("CustomEnchants")) {
                                AxisAlignedBB bb = entity.getEntityBoundingBox().offset(-renderPosX, -renderPosY, -renderPosZ);
                                // Aqua/Blue box for Mystics
                                drawOutlinedBox(bb, 0, 255, 255, 255);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Renders a perfect X-Ray bounding box through walls
     */
    private void drawOutlinedBox(AxisAlignedBB bb, int r, int g, int b, int a) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth(); // Disables depth so you see it through walls
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        
        RenderGlobal.drawOutlinedBoundingBox(bb, r, g, b, a);
        
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}