package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CoordsHUD extends DraggableHUD {
    public static CoordsHUD instance = new CoordsHUD();
    public static boolean enabled = true;
    public static int axisColor = 0xFF5555;   
    public static int numberColor = 0xFFFFFF; 

    private Minecraft mc = Minecraft.getMinecraft();

    public CoordsHUD() {
        super("Coordinates", 10, 10);
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (mc.currentScreen instanceof EditHUDGui) return;
        if (mc.currentScreen instanceof HUDSettingsGui) return;
        render(false, 0, 0); 
    }

    @Override
    public void draw(boolean isEditing) {
        if (!enabled || mc.thePlayer == null) return;

        FontRenderer fr = mc.fontRendererObj;
        BlockPos pos = new BlockPos(mc.getRenderViewEntity().posX, mc.getRenderViewEntity().getEntityBoundingBox().minY, mc.getRenderViewEntity().posZ);
        
        // Add explicit + for positive numbers
        String posX = (pos.getX() > 0 ? "+" : "") + pos.getX();
        String posY = String.valueOf(pos.getY()); // Y doesn't usually get a + in CB
        String posZ = (pos.getZ() > 0 ? "+" : "") + pos.getZ();
        
        // Exact CheatBreaker 8-Way Math
        double yaw = MathHelper.wrapAngleTo180_double(mc.thePlayer.rotationYaw) + 180.0D;
        yaw += 22.5D; 
        yaw %= 360.0D;
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        String direction = directions[MathHelper.floor_double(yaw / 45.0D)];

        if (isHorizontal) {
            // Layout: X: +100 Y: 64 Z: -50 [NW]
            
            // X
            fr.drawStringWithShadow("X: ", 2, 2, axisColor);
            int off = fr.getStringWidth("X: ");
            fr.drawStringWithShadow(posX, 2 + off, 2, numberColor);
            off += fr.getStringWidth(posX) + 6;

            // Y
            fr.drawStringWithShadow("Y: ", 2 + off, 2, axisColor);
            off += fr.getStringWidth("Y: ");
            fr.drawStringWithShadow(posY, 2 + off, 2, numberColor);
            off += fr.getStringWidth(posY) + 6;

            // Z
            fr.drawStringWithShadow("Z: ", 2 + off, 2, axisColor);
            off += fr.getStringWidth("Z: ");
            fr.drawStringWithShadow(posZ, 2 + off, 2, numberColor);
            off += fr.getStringWidth(posZ) + 6;

            // Direction [NW]
            String dirText = "[" + direction + "]";
            fr.drawStringWithShadow(dirText, 2 + off, 2, numberColor);
            
            this.width = 2 + off + fr.getStringWidth(dirText) + 2;
            this.height = fr.FONT_HEIGHT + 4;
        } else {
            fr.drawStringWithShadow("X: ", 2, 2, axisColor);
            fr.drawStringWithShadow(posX, 2 + fr.getStringWidth("X: "), 2, numberColor);
            
            fr.drawStringWithShadow("Y: ", 2, 12, axisColor);
            fr.drawStringWithShadow(posY, 2 + fr.getStringWidth("Y: "), 12, numberColor);
            
            fr.drawStringWithShadow("Z: ", 2, 22, axisColor);
            fr.drawStringWithShadow(posZ, 2 + fr.getStringWidth("Z: "), 22, numberColor);
            
            String dirText = "[" + direction + "]";
            fr.drawStringWithShadow(dirText, 2, 32, numberColor);
            
            int maxW = Math.max(fr.getStringWidth("X: " + posX), Math.max(fr.getStringWidth("Y: " + posY), fr.getStringWidth("Z: " + posZ)));
            maxW = Math.max(maxW, fr.getStringWidth(dirText));
            this.width = maxW + 4;
            this.height = 42;
        }
    }
}