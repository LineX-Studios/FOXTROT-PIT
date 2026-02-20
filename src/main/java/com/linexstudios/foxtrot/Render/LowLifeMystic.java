package com.linexstudios.foxtrot.Render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;
import java.util.List;

public class LowLifeMystic {
    
    public static final LowLifeMystic instance = new LowLifeMystic();
    private final Minecraft mc = Minecraft.getMinecraft();
    
    public static boolean enabled = true; 

    // Used to safely locate the inventory slots on your screen
    private Field guiLeftField = null;
    private Field guiTopField = null;
    private boolean reflectionInitialized = false;

    private void initReflection() {
        if (reflectionInitialized) return;
        try {
            guiLeftField = GuiContainer.class.getDeclaredField("guiLeft");
            guiLeftField.setAccessible(true);
            guiTopField = GuiContainer.class.getDeclaredField("guiTop");
            guiTopField.setAccessible(true);
        } catch (Exception e) {
            try {
                guiLeftField = GuiContainer.class.getDeclaredField("field_147003_i");
                guiLeftField.setAccessible(true);
                guiTopField = GuiContainer.class.getDeclaredField("field_147009_r");
                guiTopField.setAccessible(true);
            } catch (Exception ex) {
                // Ignore
            }
        }
        reflectionInitialized = true;
    }

    @SubscribeEvent
    public void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!enabled || mc.thePlayer == null) return;
        
        GuiScreen gui = event.gui;
        
        if (gui instanceof GuiContainer) {
            GuiContainer container = (GuiContainer) gui;
            initReflection();

            if (guiLeftField == null || guiTopField == null) return;

            int guiLeft = 0;
            int guiTop = 0;
            try {
                guiLeft = guiLeftField.getInt(container);
                guiTop = guiTopField.getInt(container);
            } catch (Exception e) {
                return;
            }
            
            for (Slot slot : container.inventorySlots.inventorySlots) {
                if (slot != null && slot.getHasStack()) {
                    ItemStack item = slot.getStack();
                    
                    // 1. MUST verify it is a real Hypixel Pit Mystic first!
                    if (isTrueMystic(item)) {
                        // 2. Only then do we parse the lore for the remaining lives
                        int currentLives = getMysticLives(item);
                        
                        if (currentLives != -1 && currentLives <= 4) {
                            drawDangerBorder(guiLeft + slot.xDisplayPosition, guiTop + slot.yDisplayPosition);
                        }
                    }
                }
            }
        }
    }

    /**
     * Strictly verifies the item has Hypixel Pit ExtraAttributes (Nonce / CustomEnchants)
     */
    private boolean isTrueMystic(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return false;
        
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt.hasKey("ExtraAttributes", 10)) { // 10 is the tag ID for Compounds
            NBTTagCompound extra = nbt.getCompoundTag("ExtraAttributes");
            // True mystics always have a Nonce and usually CustomEnchants
            return extra.hasKey("Nonce") || extra.hasKey("CustomEnchants");
        }
        return false;
    }

    /**
     * Scans the literal item tooltip line-by-line to guarantee we always find the exact lives.
     */
    private int getMysticLives(ItemStack stack) {
        try {
            List<String> tooltip = stack.getTooltip(mc.thePlayer, false);
            for (String line : tooltip) {
                String rawLine = EnumChatFormatting.getTextWithoutFormattingCodes(line);
                
                if (rawLine != null && rawLine.contains("Lives: ")) {
                    try {
                        String livesStr = rawLine.substring(rawLine.indexOf("Lives: ") + 7);
                        if (livesStr.contains("/")) {
                            String currentLivesStr = livesStr.split("/")[0].trim();
                            return Integer.parseInt(currentLivesStr);
                        }
                    } catch (Exception e) {
                        // Ignored
                    }
                }
            }
        } catch (Exception e) {
            // Ignored
        }
        return -1;
    }

    /**
     * Draws a very obvious red overlay and four corner markers to alert you of low lives.
     */
    private void drawDangerBorder(int x, int y) {
        float alpha = 0.6F + (float)(Math.sin(System.currentTimeMillis() / 200.0) * 0.3F);

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth(); // Crucial so it draws *over* the item
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        
        // 1. Faint solid red inner box to tint the item icon
        GL11.glColor4f(1.0F, 0.0F, 0.0F, 0.25F); 
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y + 16);
        GL11.glVertex2f(x + 16, y + 16);
        GL11.glVertex2f(x + 16, y);
        GL11.glEnd();

        // 2. Bright pulsating red corner markers
        GL11.glColor4f(1.0F, 0.0F, 0.0F, alpha); 
        GL11.glLineWidth(2.5F);
        
        float cornerLen = 4.5F; 
        
        GL11.glBegin(GL11.GL_LINES);
        // Top-Left Corner
        GL11.glVertex2f(x, y); GL11.glVertex2f(x + cornerLen, y);
        GL11.glVertex2f(x, y); GL11.glVertex2f(x, y + cornerLen);
        
        // Top-Right Corner
        GL11.glVertex2f(x + 16, y); GL11.glVertex2f(x + 16 - cornerLen, y);
        GL11.glVertex2f(x + 16, y); GL11.glVertex2f(x + 16, y + cornerLen);
        
        // Bottom-Left Corner
        GL11.glVertex2f(x, y + 16); GL11.glVertex2f(x + cornerLen, y + 16);
        GL11.glVertex2f(x, y + 16); GL11.glVertex2f(x, y + 16 - cornerLen);
        
        // Bottom-Right Corner
        GL11.glVertex2f(x + 16, y + 16); GL11.glVertex2f(x + 16 - cornerLen, y + 16);
        GL11.glVertex2f(x + 16, y + 16); GL11.glVertex2f(x + 16, y + 16 - cornerLen);
        GL11.glEnd();

        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}