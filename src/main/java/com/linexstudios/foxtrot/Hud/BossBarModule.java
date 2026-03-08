package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.entity.boss.BossStatus;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BossBarModule extends DraggableHUD {
    public static final BossBarModule instance = new BossBarModule();
    private final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = true;

    public BossBarModule() {
        super("Boss Bar", 100, 10); // Default near the top middle
        
        // FIX: We MUST initialize the dimensions here so the dragging Hitbox exists!
        this.width = 182;
        this.height = 20;
    }

    @SubscribeEvent
    public void onRenderPre(RenderGameOverlayEvent.Pre event) {
        // Cancel vanilla boss bar if our module is enabled
        if (enabled && event.type == RenderGameOverlayEvent.ElementType.BOSSHEALTH) {
            event.setCanceled(true);
        }
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
        if (!enabled && !isEditing) return;

        // In editing mode, force a fake boss bar to show so we can position it
        boolean hasRealBoss = BossStatus.bossName != null && BossStatus.statusBarTime > 0;
        if (!hasRealBoss && !isEditing) return;

        String bossName = hasRealBoss ? BossStatus.bossName : EnumChatFormatting.YELLOW + "" + EnumChatFormatting.BOLD + "BOSS BAR";
        float bossHealth = hasRealBoss ? BossStatus.healthScale : 1.0f;

        // Safely push Matrix so the Boss Bar doesn't tint other HUD elements
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        
        mc.getTextureManager().bindTexture(Gui.icons);
        FontRenderer fr = mc.fontRendererObj;
        
        int barWidth = 182;
        int filledWidth = (int)(bossHealth * (float)barWidth);
        int yOffset = 13;
        
        // Draw empty background bar (Drawn twice for vanilla shadow thickness)
        mc.ingameGUI.drawTexturedModalRect(0, yOffset, 0, 74, barWidth, 5);
        mc.ingameGUI.drawTexturedModalRect(0, yOffset, 0, 74, barWidth, 5);
        
        // Draw filled foreground bar
        if (filledWidth > 0) {
            mc.ingameGUI.drawTexturedModalRect(0, yOffset, 0, 79, filledWidth, 5);
        }

        // Draw Boss Name text centered above the bar
        fr.drawStringWithShadow(bossName, (barWidth / 2.0f) - (fr.getStringWidth(bossName) / 2.0f), yOffset - 10, 0xFFFFFF);
        
        // Safely pop Matrix
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        
        // Continually update dimensions just in case it scales
        this.width = 182;
        this.height = 20;
    }
}