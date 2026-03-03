package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Handler.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import java.awt.Color;
import java.io.IOException;

public class HUDSettingsGui extends GuiScreen {
    private final GuiScreen previousScreen;
    
    // Added Player Counter to the end of the array
    private String[] modules = {"Potion Status", "Armor Status", "Coordinates", "Enemy List", "Nicked List", "Friend List", "Session Stats", "Event Tracker", "Regularity List", "Darks List", "Toggle Sprint", "CPS", "FPS", "Boss Bar", "Telebow Timer", "Player Counter"};
    
    private boolean inSettingsMenu = false;
    private int selectedModule = -1;
    
    private float scrollY = 0;
    private float targetScrollY = 0;
    private int maxScroll = 0;
    
    private boolean draggingSlider = false;
    private boolean draggingFlySlider = false; 
    private boolean draggingColorBox = false;
    private boolean draggingHueBar = false;

    private int[] palette = {0xFFFFFF, 0xAAAAAA, 0x555555, 0xFF5555, 0x55FF55, 0x5555FF, 0xFFFF55, 0x55FFFF, 0xFFAA00, 0xFF55FF, 0x000000};
    private int activeCustomColorTarget = -1; 
    private float currentHue = 0f;
    private float currentSat = 1f;
    private float currentBri = 1f;
    private int pickerX = 0, pickerY = 0;

    // --- COLORS & STYLING ---
    private final int COLOR_ENABLED = 0xFF28A061;  
    private final int COLOR_DISABLED = 0xFFB82C35; 
    private final int COLOR_TEXT_SECONDARY = 0xFFAAAAAA;
    private final int COLOR_SEPARATOR = 0x44FFFFFF;
    private final int COLOR_CARD_BG = 0x44000000;
    private final int COLOR_CARD_BG_HOVER = 0x66000000;
    private final int COLOR_BTN_HOVER_OVERLAY = 0x22FFFFFF;

    public HUDSettingsGui(GuiScreen previousScreen) { 
        this.previousScreen = previousScreen; 
    }

    public HUDSettingsGui(GuiScreen previousScreen, int defaultTab) { 
        this.previousScreen = previousScreen; 
        this.selectedModule = defaultTab;
        this.inSettingsMenu = true; 
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0 && !inSettingsMenu) {
            if (dWheel > 0) targetScrollY -= 45;
            if (dWheel < 0) targetScrollY += 45;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawSolidRect(0, 0, this.width, this.height, 0x99000000); 
        
        GlStateManager.pushMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        if (PotionHUD.enabled) PotionHUD.instance.render(true, mouseX, mouseY);
        if (ArmorHUD.enabled) ArmorHUD.instance.render(true, mouseX, mouseY);
        if (CoordsHUD.enabled) CoordsHUD.instance.render(true, mouseX, mouseY);
        if (EnemyHUD.enabled) EnemyHUD.instance.render(true, mouseX, mouseY);
        if (NickedHUD.enabled) NickedHUD.instance.render(true, mouseX, mouseY);
        if (FriendsHUD.enabled) FriendsHUD.instance.render(true, mouseX, mouseY);
        if (SessionStatsHUD.enabled) SessionStatsHUD.instance.render(true, mouseX, mouseY);
        if (EventHUD.enabled) EventHUD.instance.render(true, mouseX, mouseY);
        if (RegHUD.enabled) RegHUD.instance.render(true, mouseX, mouseY);
        if (DarksHUD.enabled) DarksHUD.instance.render(true, mouseX, mouseY); 
        if (ToggleSprintModule.instance.enabled) ToggleSprintModule.instance.render(true, mouseX, mouseY);
        if (CPSModule.enabled) CPSModule.instance.render(true, mouseX, mouseY);
        if (FPSModule.enabled) FPSModule.instance.render(true, mouseX, mouseY); 
        if (BossBarModule.enabled) BossBarModule.instance.render(true, mouseX, mouseY);
        if (TelebowHUD.enabled) TelebowHUD.instance.render(true, mouseX, mouseY); 
        if (PlayerCounterHUD.enabled) PlayerCounterHUD.instance.render(true, mouseX, mouseY); // NEW: Renders Dummy
        GlStateManager.popMatrix();

        GlStateManager.disableLighting();
        GlStateManager.enableBlend();

        int panelW = 400;
        int panelH = 300;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        drawGradientRoundedRect(panelX, panelY, panelW, panelH, 6, 0xCC1E1E1E, 0xCC141414);
        drawRoundedOutline(panelX, panelY, panelW, panelH, 6, 1.0f, 0x44FF0000);

        if (!inSettingsMenu) {
            this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + "HUD Customization", panelX + 15, panelY + 14, -1);
            
            boolean hoverX = isInside(mouseX, mouseY, panelX + panelW - 25, panelY + 10, 15, 15);
            drawCross(panelX + panelW - 17, panelY + 17, 3.5f, 1.5f, hoverX ? COLOR_DISABLED : COLOR_TEXT_SECONDARY);

            drawSolidRect(panelX + 15, panelY + 30, panelW - 30, 1, 0x1AFFFFFF); 

            int cols = 3;
            int cardW = 115;
            int cardH = 110;
            int spacingX = 12;
            int spacingY = 12;
            
            int gridWidth = (cols * cardW) + ((cols - 1) * spacingX);
            int startX = panelX + (panelW - gridWidth) / 2;
            int startY = panelY + 40;
            int viewH = panelH - 50;

            int rows = (modules.length + cols - 1) / cols;
            maxScroll = Math.max(0, (rows * (cardH + spacingY)) - viewH + 15);
            targetScrollY = Math.max(0, Math.min(maxScroll, targetScrollY));
            scrollY += (targetScrollY - scrollY) * 0.2f;

            doGlScissor(panelX, startY, panelW, viewH);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);

            for (int i = 0; i < modules.length; i++) {
                int col = i % cols;
                int row = i / cols;
                int cx = startX + col * (cardW + spacingX);
                int cy = startY + row * (cardH + spacingY) - (int)scrollY;
                
                if (cy + cardH < startY || cy > startY + viewH) continue; 
                
                int btnX = cx + 6;
                int btnW = cardW - 12;
                int btnH = 20;
                int optY = cy + 62;
                int togY = cy + 86;

                boolean isEnabled = isModuleEnabled(i);
                boolean hoveredCard = isInside(mouseX, mouseY, cx, cy, cardW, cardH) && mouseY >= startY && mouseY <= startY + viewH;
                boolean hoverOptions = isInside(mouseX, mouseY, btnX, optY, btnW, btnH) && mouseY >= startY && mouseY <= startY + viewH;
                boolean hoverToggle = isInside(mouseX, mouseY, btnX, togY, btnW, btnH) && mouseY >= startY && mouseY <= startY + viewH;

                drawRoundedRect(cx, cy, cardW, cardH, 4, hoveredCard ? COLOR_CARD_BG_HOVER : COLOR_CARD_BG);
                drawRoundedOutline(cx, cy, cardW, cardH, 4, 1.0f, 0x22FFFFFF);

                String name = modules[i];
                int tw = this.fontRendererObj.getStringWidth(name);
                this.fontRendererObj.drawStringWithShadow(name, (int)(cx + (cardW - tw) / 2.0f), (int)(cy + 27), isEnabled ? -1 : COLOR_TEXT_SECONDARY);
                
                drawInnerRoundedRect(btnX, optY, btnW, btnH, 3, hoverOptions ? 0x55FFFFFF : 0x33FFFFFF, hoverOptions);
                
                String optText = "OPTIONS";
                int optW = this.fontRendererObj.getStringWidth(optText);
                int optTxtX = (int)(btnX + (btnW - optW) / 2.0f); 
                int optTxtY = (int)(optY + (btnH - 8) / 2.0f); 
                this.fontRendererObj.drawStringWithShadow(optText, optTxtX, optTxtY, -1);

                float sepX = cx + cardW - 28; 
                drawSolidRect(sepX, optY + 4, 1, btnH - 8, COLOR_SEPARATOR);
                drawMathGear(sepX + 11, optY + (btnH / 2.0f), 4.5f, 0xFFFFFFFF, 0f);

                int toggleColor = isEnabled ? COLOR_ENABLED : COLOR_DISABLED;
                drawInnerRoundedRect(btnX, togY, btnW, btnH, 3, toggleColor, hoverToggle);
                
                String toggleText = isEnabled ? "ENABLED" : "DISABLED";
                int ttw = this.fontRendererObj.getStringWidth(toggleText);
                int togTxtX = (int)(btnX + (btnW - ttw) / 2.0f);
                int togTxtY = (int)(togY + (btnH - 8) / 2.0f);
                this.fontRendererObj.drawStringWithShadow(toggleText, togTxtX, togTxtY, -1);
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            if (maxScroll > 0) {
                float scrollPct = scrollY / maxScroll;
                float trackH = viewH;
                float barH = trackH * (trackH / (float)(rows * (cardH + spacingY)));
                barH = Math.max(20, barH);
                float barY = startY + scrollPct * (trackH - barH);
                drawRoundedRect(panelX + panelW - 8, barY, 4, barH, 2, 0x55FFFFFF);
            }

        } else {
            boolean hoverBack = isInside(mouseX, mouseY, panelX + 10, panelY + 10, 60, 18);
            drawInnerRoundedRect(panelX + 10, panelY + 10, 60, 18, 3, 0x33000000, hoverBack);
            this.fontRendererObj.drawStringWithShadow("< Back", panelX + 22, panelY + 15, -1);
            this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + modules[selectedModule] + " Settings", panelX + 85, panelY + 15, -1);
            drawSolidRect(panelX + 15, panelY + 35, panelW - 30, 1, 0x1AFFFFFF); 

            int rX = panelX + 20; 
            int rY = panelY + 50;

            drawSettingsCard(rX, rY, 360, 40); 
            this.fontRendererObj.drawStringWithShadow("HUD Scale", rX + 10, rY + 16, 0xDDDDDD);
            drawIOSSlider(rX + 80, rY + 14, getScaleForTab(selectedModule), 0.5f, 1.5f, 210); 
            drawSettingsButton(rX + 300, rY + 12, 50, 16, "Reset", mouseX, mouseY); 
            rY += 50; 

            if (selectedModule == 0) { 
                drawSettingsCard(rX, rY, 360, 85);
                this.fontRendererObj.drawStringWithShadow("Name Color", rX + 10, rY + 10, 0xDDDDDD);
                drawPalette(rX + 10, rY + 23, PotionHUD.nameColor, mouseX, mouseY, 1);
                this.fontRendererObj.drawStringWithShadow("Duration Color", rX + 10, rY + 50, 0xDDDDDD);
                drawPalette(rX + 10, rY + 63, PotionHUD.durationColor, mouseX, mouseY, 2);
            } else if (selectedModule == 1) { 
                drawSettingsCard(rX, rY, 360, 75);
                drawIOSToggle(rX + 10, rY + 10, 340, "Horizontal Layout", ArmorHUD.instance.isHorizontal, mouseX, mouseY);
                drawSolidRect(rX + 10, rY + 32, 340, 1, 0x11FFFFFF);
                this.fontRendererObj.drawStringWithShadow("Durability Color", rX + 10, rY + 42, 0xDDDDDD);
                drawPalette(rX + 10, rY + 55, ArmorHUD.durabilityColor, mouseX, mouseY, 3);
            } else if (selectedModule == 2) { 
                drawSettingsCard(rX, rY, 360, 135);
                drawIOSToggle(rX + 10, rY + 10, 340, "Horizontal Layout", CoordsHUD.instance.isHorizontal, mouseX, mouseY);
                drawSolidRect(rX + 10, rY + 30, 340, 1, 0x11FFFFFF);
                this.fontRendererObj.drawStringWithShadow("Axis Color", rX + 10, rY + 40, 0xDDDDDD);
                drawPalette(rX + 10, rY + 52, CoordsHUD.axisColor, mouseX, mouseY, 4);
                this.fontRendererObj.drawStringWithShadow("Value Color", rX + 10, rY + 72, 0xDDDDDD);
                drawPalette(rX + 10, rY + 84, CoordsHUD.numberColor, mouseX, mouseY, 5);
                this.fontRendererObj.drawStringWithShadow("Direction Color", rX + 10, rY + 104, 0xDDDDDD);
                drawPalette(rX + 10, rY + 116, CoordsHUD.directionColor, mouseX, mouseY, 6);
            } else if (selectedModule == 8) {
                drawSettingsCard(rX, rY, 360, 30);
                this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.GRAY + "Use scale to resize Reg List.", rX + 10, rY + 11, -1);
            } else if (selectedModule == 9) {
                drawSettingsCard(rX, rY, 360, 30);
                this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.GRAY + "Use scale to resize Darks List.", rX + 10, rY + 11, -1);
            } else if (selectedModule == 10) { 
                drawSettingsCard(rX, rY, 360, 160);
                drawIOSToggle(rX + 10, rY + 10, 340, "Enable Module", ToggleSprintModule.instance.enabled, mouseX, mouseY);
                drawIOSToggle(rX + 10, rY + 30, 340, "Toggle Sprint", ToggleSprintModule.instance.toggleSprint, mouseX, mouseY);
                drawIOSToggle(rX + 10, rY + 50, 340, "Toggle Sneak", ToggleSprintModule.instance.toggleSneak, mouseX, mouseY);
                drawIOSToggle(rX + 10, rY + 70, 340, "W-Tap", ToggleSprintModule.instance.wTapFix, mouseX, mouseY);
                drawIOSToggle(rX + 10, rY + 90, 340, "Fly Boost", ToggleSprintModule.instance.flyBoost, mouseX, mouseY);
                drawSolidRect(rX + 10, rY + 110, 340, 1, 0x11FFFFFF);
                this.fontRendererObj.drawStringWithShadow("Fly Boost", rX + 10, rY + 118, 0xDDDDDD);
                drawIOSSlider(rX + 80, rY + 116, ToggleSprintModule.instance.flyBoostAmount, 1.0f, 10.0f, 250);
                this.fontRendererObj.drawStringWithShadow("Text Color", rX + 10, rY + 140, 0xDDDDDD);
                drawPalette(rX + 80, rY + 135, ToggleSprintModule.instance.textColor, mouseX, mouseY, 7);
            } else if (selectedModule == 11) { 
                drawSettingsCard(rX, rY, 360, 70);
                drawIOSToggle(rX + 10, rY + 8, 340, "Show Background", CPSModule.showBackground, mouseX, mouseY);
                drawSolidRect(rX + 10, rY + 28, 340, 1, 0x11FFFFFF);
                this.fontRendererObj.drawStringWithShadow("Text Color", rX + 10, rY + 36, 0xDDDDDD);
                drawPalette(rX + 10, rY + 49, CPSModule.textColor, mouseX, mouseY, 8);
            } else if (selectedModule == 12) { 
                drawSettingsCard(rX, rY, 360, 70);
                drawIOSToggle(rX + 10, rY + 8, 340, "Show Background", FPSModule.showBackground, mouseX, mouseY);
                drawSolidRect(rX + 10, rY + 28, 340, 1, 0x11FFFFFF);
                this.fontRendererObj.drawStringWithShadow("Text Color", rX + 10, rY + 36, 0xDDDDDD);
                drawPalette(rX + 10, rY + 49, FPSModule.textColor, mouseX, mouseY, 10);
            } else if (selectedModule == 13) { 
                drawSettingsCard(rX, rY, 360, 30);
                this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.GRAY + "Use scale to resize Boss Bar.", rX + 10, rY + 11, -1);
            } else if (selectedModule == 14) { 
                drawSettingsCard(rX, rY, 360, 30);
                this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.GRAY + "Use scale to resize Telebow Timer.", rX + 10, rY + 11, -1);
            } else if (selectedModule == 15) { 
                // --- NEW PLAYER COUNTER COLOR PICKERS ---
                drawSettingsCard(rX, rY, 360, 85);
                this.fontRendererObj.drawStringWithShadow("Prefix Color (Text)", rX + 10, rY + 10, 0xDDDDDD);
                drawPalette(rX + 10, rY + 23, PlayerCounterHUD.prefixColor, mouseX, mouseY, 11);
                this.fontRendererObj.drawStringWithShadow("Number of Players", rX + 10, rY + 50, 0xDDDDDD);
                drawPalette(rX + 10, rY + 63, PlayerCounterHUD.countColor, mouseX, mouseY, 12);
            }

            if (activeCustomColorTarget != -1) drawColorPickerPopup(pickerX, pickerY, mouseX, mouseY);
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private boolean isModuleEnabled(int i) {
        if (i == 0) return PotionHUD.enabled;
        if (i == 1) return ArmorHUD.enabled;
        if (i == 2) return CoordsHUD.enabled;
        if (i == 3) return EnemyHUD.enabled;
        if (i == 4) return NickedHUD.enabled;
        if (i == 5) return FriendsHUD.enabled;
        if (i == 6) return SessionStatsHUD.enabled;
        if (i == 7) return EventHUD.enabled;
        if (i == 8) return RegHUD.enabled;
        if (i == 9) return DarksHUD.enabled; 
        if (i == 10) return ToggleSprintModule.instance.enabled;
        if (i == 11) return CPSModule.enabled;
        if (i == 12) return FPSModule.enabled;
        if (i == 13) return BossBarModule.enabled;
        if (i == 14) return TelebowHUD.enabled;
        if (i == 15) return PlayerCounterHUD.enabled;
        return false;
    }

    private void toggleModule(int i) {
        if (i == 0) PotionHUD.enabled = !PotionHUD.enabled;
        if (i == 1) ArmorHUD.enabled = !ArmorHUD.enabled;
        if (i == 2) CoordsHUD.enabled = !CoordsHUD.enabled;
        if (i == 3) EnemyHUD.enabled = !EnemyHUD.enabled;
        if (i == 4) NickedHUD.enabled = !NickedHUD.enabled;
        if (i == 5) FriendsHUD.enabled = !FriendsHUD.enabled;
        if (i == 6) SessionStatsHUD.enabled = !SessionStatsHUD.enabled;
        if (i == 7) EventHUD.enabled = !EventHUD.enabled;
        if (i == 8) RegHUD.enabled = !RegHUD.enabled;
        if (i == 9) DarksHUD.enabled = !DarksHUD.enabled; 
        if (i == 10) ToggleSprintModule.instance.enabled = !ToggleSprintModule.instance.enabled;
        if (i == 11) CPSModule.enabled = !CPSModule.enabled;
        if (i == 12) FPSModule.enabled = !FPSModule.enabled;
        if (i == 13) BossBarModule.enabled = !BossBarModule.enabled;
        if (i == 14) TelebowHUD.enabled = !TelebowHUD.enabled;
        if (i == 15) PlayerCounterHUD.enabled = !PlayerCounterHUD.enabled;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) return;
        
        int panelW = 400; int panelH = 300;
        int panelX = (this.width - panelW) / 2; int panelY = (this.height - panelH) / 2;

        if (!inSettingsMenu) {
            if (isInside(mouseX, mouseY, panelX + panelW - 25, panelY + 10, 15, 15)) {
                ConfigHandler.saveConfig(); this.mc.displayGuiScreen(previousScreen); return;
            }

            int cols = 3; int cardW = 115; int cardH = 110;
            int spacingX = 12; int spacingY = 12;
            int gridWidth = (cols * cardW) + ((cols - 1) * spacingX);
            int startX = panelX + (panelW - gridWidth) / 2;
            int startY = panelY + 40;
            int viewH = panelH - 50;

            for (int i = 0; i < modules.length; i++) {
                int col = i % cols; int row = i / cols;
                int cx = startX + col * (cardW + spacingX);
                int cy = startY + row * (cardH + spacingY) - (int)scrollY;
                if (cy + cardH < startY || cy > startY + viewH) continue;
                
                int btnX = cx + 6; int btnW = cardW - 12; int btnH = 20;
                int optY = cy + 62; int togY = cy + 86;

                boolean inCard = isInside(mouseX, mouseY, cx, cy, cardW, cardH) && mouseY >= startY && mouseY <= startY + viewH;
                boolean inOptions = isInside(mouseX, mouseY, btnX, optY, btnW, btnH) && mouseY >= startY && mouseY <= startY + viewH;
                boolean inToggle = isInside(mouseX, mouseY, btnX, togY, btnW, btnH) && mouseY >= startY && mouseY <= startY + viewH;

                if (inOptions || (inCard && !inToggle)) {
                    selectedModule = i;
                    inSettingsMenu = true;
                    mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
                    return;
                }
                if (inToggle) {
                    toggleModule(i);
                    mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
                    return;
                }
            }
        } else {
            if (activeCustomColorTarget != -1) {
                if (isInside(mouseX, mouseY, pickerX, pickerY, 100, 100)) { draggingColorBox = true; return; }
                if (isInside(mouseX, mouseY, pickerX + 110, pickerY, 10, 100)) { draggingHueBar = true; return; }
                activeCustomColorTarget = -1;
            }

            if (isInside(mouseX, mouseY, panelX + 10, panelY + 10, 60, 18)) {
                inSettingsMenu = false;
                mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
                return;
            }

            int rX = panelX + 20; int rY = panelY + 50; 

            if (isInside(mouseX, mouseY, rX + 300, rY + 12, 50, 16)) { assignScale(1.0f); return; } 
            if (isInside(mouseX, mouseY, rX + 80, rY + 14, 210, 15)) { 
                draggingSlider = true; 
                float pct = (float)(mouseX - (rX + 80)) / 210f; 
                pct = Math.max(0, Math.min(1, pct));
                assignScale(0.5f + (1.0f * pct)); return; 
            } 
            rY += 50; 

            if (selectedModule == 0) { 
                for (int i = 0; i < palette.length; i++) {
                    if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 23, 12, 12)) {
                        if (i == palette.length - 1) openCustomColorPicker(1, rX + 10 + (i * 22), rY + 23, PotionHUD.nameColor); 
                        else PotionHUD.nameColor = palette[i]; return; 
                    }
                }
                for (int i = 0; i < palette.length; i++) {
                    if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 63, 12, 12)) {
                        if (i == palette.length - 1) openCustomColorPicker(2, rX + 10 + (i * 22), rY + 63, PotionHUD.durationColor);
                        else PotionHUD.durationColor = palette[i]; return; 
                    }
                }
            } else if (selectedModule == 1) { 
                if (isInside(mouseX, mouseY, rX + 326, rY + 10, 24, 12)) { ArmorHUD.instance.isHorizontal = !ArmorHUD.instance.isHorizontal; return; }
                for (int i = 0; i < palette.length; i++) {
                    if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 55, 12, 12)) {
                        if (i == palette.length - 1) openCustomColorPicker(3, rX + 10 + (i * 22), rY + 55, ArmorHUD.durabilityColor);
                        else ArmorHUD.durabilityColor = palette[i]; return; 
                    }
                }
            } else if (selectedModule == 2) { 
                if (isInside(mouseX, mouseY, rX + 326, rY + 10, 24, 12)) { CoordsHUD.instance.isHorizontal = !CoordsHUD.instance.isHorizontal; return; }
                for (int i = 0; i < palette.length; i++) {
                    if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 52, 12, 12)) {
                        if (i == palette.length - 1) openCustomColorPicker(4, rX + 10 + (i * 22), rY + 52, CoordsHUD.axisColor);
                        else CoordsHUD.axisColor = palette[i]; return; 
                    }
                }
                for (int i = 0; i < palette.length; i++) {
                    if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 84, 12, 12)) {
                        if (i == palette.length - 1) openCustomColorPicker(5, rX + 10 + (i * 22), rY + 84, CoordsHUD.numberColor);
                        else CoordsHUD.numberColor = palette[i]; return; 
                    }
                }
                for (int i = 0; i < palette.length; i++) {
                    if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 116, 12, 12)) {
                        if (i == palette.length - 1) openCustomColorPicker(6, rX + 10 + (i * 22), rY + 116, CoordsHUD.directionColor);
                        else CoordsHUD.directionColor = palette[i]; return; 
                    }
                }
            } else if (selectedModule == 10) { 
                if (isInside(mouseX, mouseY, rX + 326, rY + 10, 24, 12)) { ToggleSprintModule.instance.enabled = !ToggleSprintModule.instance.enabled; return; }
                if (isInside(mouseX, mouseY, rX + 326, rY + 30, 24, 12)) { ToggleSprintModule.instance.toggleSprint = !ToggleSprintModule.instance.toggleSprint; return; }
                if (isInside(mouseX, mouseY, rX + 326, rY + 50, 24, 12)) { ToggleSprintModule.instance.toggleSneak = !ToggleSprintModule.instance.toggleSneak; return; }
                if (isInside(mouseX, mouseY, rX + 326, rY + 70, 24, 12)) { ToggleSprintModule.instance.wTapFix = !ToggleSprintModule.instance.wTapFix; return; }
                if (isInside(mouseX, mouseY, rX + 326, rY + 90, 24, 12)) { ToggleSprintModule.instance.flyBoost = !ToggleSprintModule.instance.flyBoost; return; }
                if (isInside(mouseX, mouseY, rX + 80, rY + 116, 250, 15)) { 
                    draggingFlySlider = true; 
                    float pct = (float)(mouseX - (rX + 80)) / 250f; 
                    pct = Math.max(0, Math.min(1, pct));
                    ToggleSprintModule.instance.flyBoostAmount = 1.0f + (9.0f * pct); return; 
                }
                for (int i = 0; i < palette.length; i++) {
                    if (isInside(mouseX, mouseY, rX + 80 + (i * 22), rY + 135, 12, 12)) {
                        if (i == palette.length - 1) openCustomColorPicker(7, rX + 80 + (i * 22), rY + 135, ToggleSprintModule.instance.textColor);
                        else ToggleSprintModule.instance.textColor = palette[i]; return; 
                    }
                }
            } else if (selectedModule == 11) { 
                if (isInside(mouseX, mouseY, rX + 326, rY + 8, 24, 12)) { CPSModule.showBackground = !CPSModule.showBackground; return; }
                for (int i = 0; i < palette.length; i++) {
                    if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 49, 12, 12)) {
                        if (i == palette.length - 1) openCustomColorPicker(8, rX + 10 + (i * 22), rY + 49, CPSModule.textColor); 
                        else CPSModule.textColor = palette[i]; return; 
                    }
                }
            } else if (selectedModule == 12) { 
                if (isInside(mouseX, mouseY, rX + 326, rY + 8, 24, 12)) { FPSModule.showBackground = !FPSModule.showBackground; return; }
                for (int i = 0; i < palette.length; i++) {
                    if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 49, 12, 12)) {
                        if (i == palette.length - 1) openCustomColorPicker(10, rX + 10 + (i * 22), rY + 49, FPSModule.textColor); 
                        else FPSModule.textColor = palette[i]; return; 
                    }
                }
            } else if (selectedModule == 15) { 
                // NEW: Player Counter Color Pickers
                for (int i = 0; i < palette.length; i++) {
                    if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 23, 12, 12)) {
                        if (i == palette.length - 1) openCustomColorPicker(11, rX + 10 + (i * 22), rY + 23, PlayerCounterHUD.prefixColor); 
                        else PlayerCounterHUD.prefixColor = palette[i]; return; 
                    }
                }
                for (int i = 0; i < palette.length; i++) {
                    if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 63, 12, 12)) {
                        if (i == palette.length - 1) openCustomColorPicker(12, rX + 10 + (i * 22), rY + 63, PlayerCounterHUD.countColor); 
                        else PlayerCounterHUD.countColor = palette[i]; return; 
                    }
                }
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (!inSettingsMenu) return;
        if (draggingColorBox) {
            currentSat = Math.max(0, Math.min(1, (mouseX - pickerX) / 100f));
            currentBri = Math.max(0, Math.min(1, 1f - ((mouseY - pickerY) / 100f)));
            updateTargetColor();
        } else if (draggingHueBar) {
            currentHue = Math.max(0, Math.min(1, (mouseY - pickerY) / 100f));
            updateTargetColor();
        } else if (draggingSlider) {
            int panelW = 400; int panelX = (this.width - panelW) / 2; int rX = panelX + 20;
            float pct = (float)(mouseX - (rX + 80)) / 210f; 
            pct = Math.max(0, Math.min(1, pct));
            assignScale(0.5f + (1.0f * pct));
        } else if (draggingFlySlider) {
            int panelW = 400; int panelX = (this.width - panelW) / 2; int rX = panelX + 20;
            float pct = (float)(mouseX - (rX + 80)) / 250f; 
            pct = Math.max(0, Math.min(1, pct));
            ToggleSprintModule.instance.flyBoostAmount = 1.0f + (9.0f * pct); 
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) { 
        draggingSlider = false; draggingColorBox = false; draggingHueBar = false; draggingFlySlider = false;
        ConfigHandler.saveConfig(); 
    }

    private void drawColorPickerPopup(int x, int y, int mouseX, int mouseY) {
        drawRoundedRect(x - 5, y - 5, 135, 110, 4, 0xFA1E1E1E);
        setupSmoothRender(true);
        GL11.glBegin(GL11.GL_QUADS);
        setColor(0xFFFFFFFF); GL11.glVertex2f(x, y); 
        setColor(0xFF000000); GL11.glVertex2f(x, y + 100); 
        setColor(0xFF000000); GL11.glVertex2f(x + 100, y + 100); 
        setColor(Color.HSBtoRGB(currentHue, 1.0f, 1.0f) | 0xFF000000); GL11.glVertex2f(x + 100, y); 
        GL11.glEnd(); endSmoothRender();
        float dotX = x + (currentSat * 100); float dotY = y + ((1f - currentBri) * 100);
        drawCircle(dotX, dotY, 4.0f, 0xFF000000); drawCircle(dotX, dotY, 3.0f, 0xFFFFFFFF);
        setupSmoothRender(true);
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i <= 20; i++) {
            float hueStep = i / 20f;
            setColor(Color.HSBtoRGB(hueStep, 1.0f, 1.0f) | 0xFF000000);
            GL11.glVertex2f(x + 110, y + (hueStep * 100)); GL11.glVertex2f(x + 120, y + (hueStep * 100));
        }
        GL11.glEnd(); endSmoothRender();
        float hueY = y + (currentHue * 100);
        drawSolidRect(x + 108, hueY - 1, 14, 3, 0xFFFFFFFF); drawSolidRect(x + 109, hueY, 12, 1, 0xFF000000);
    }

    private void updateTargetColor() {
        int finalColor = Color.HSBtoRGB(currentHue, currentSat, currentBri);
        if (activeCustomColorTarget == 1) PotionHUD.nameColor = finalColor & 0x00FFFFFF;
        else if (activeCustomColorTarget == 2) PotionHUD.durationColor = finalColor & 0x00FFFFFF;
        else if (activeCustomColorTarget == 3) ArmorHUD.durabilityColor = finalColor & 0x00FFFFFF;
        else if (activeCustomColorTarget == 4) CoordsHUD.axisColor = finalColor & 0x00FFFFFF;
        else if (activeCustomColorTarget == 5) CoordsHUD.numberColor = finalColor & 0x00FFFFFF;
        else if (activeCustomColorTarget == 6) CoordsHUD.directionColor = finalColor & 0x00FFFFFF;
        else if (activeCustomColorTarget == 7) ToggleSprintModule.instance.textColor = finalColor & 0x00FFFFFF;
        else if (activeCustomColorTarget == 8) CPSModule.textColor = finalColor & 0x00FFFFFF;
        else if (activeCustomColorTarget == 10) FPSModule.textColor = finalColor & 0x00FFFFFF;
        // Map new targets to Player Counter Colors
        else if (activeCustomColorTarget == 11) PlayerCounterHUD.prefixColor = finalColor & 0x00FFFFFF;
        else if (activeCustomColorTarget == 12) PlayerCounterHUD.countColor = finalColor & 0x00FFFFFF;
    }

    private void openCustomColorPicker(int targetId, int x, int y, int currentColor) {
        activeCustomColorTarget = targetId; pickerX = x + 15; pickerY = y - 45;
        float[] hsb = Color.RGBtoHSB((currentColor >> 16) & 0xFF, (currentColor >> 8) & 0xFF, currentColor & 0xFF, null);
        currentHue = hsb[0]; currentSat = hsb[1]; currentBri = hsb[2];
    }

    private void assignScale(float val) {
        if (selectedModule == 0) PotionHUD.instance.scale = val;
        else if (selectedModule == 1) ArmorHUD.instance.scale = val;
        else if (selectedModule == 2) CoordsHUD.instance.scale = val;
        else if (selectedModule == 3) EnemyHUD.instance.scale = val;
        else if (selectedModule == 4) NickedHUD.instance.scale = val;
        else if (selectedModule == 5) FriendsHUD.instance.scale = val;
        else if (selectedModule == 6) SessionStatsHUD.instance.scale = val;
        else if (selectedModule == 7) EventHUD.instance.scale = val;
        else if (selectedModule == 8) RegHUD.instance.scale = val;
        else if (selectedModule == 9) DarksHUD.instance.scale = val; 
        else if (selectedModule == 10) ToggleSprintModule.instance.scale = val;
        else if (selectedModule == 11) CPSModule.instance.scale = val;
        else if (selectedModule == 12) FPSModule.instance.scale = val;
        else if (selectedModule == 13) BossBarModule.instance.scale = val;
        else if (selectedModule == 14) TelebowHUD.instance.scale = val;
        else if (selectedModule == 15) PlayerCounterHUD.instance.scale = val; // Scale mapping
    }

    private float getScaleForTab(int tab) {
        if (tab == 0) return PotionHUD.instance.scale;
        if (tab == 1) return ArmorHUD.instance.scale;
        if (tab == 2) return CoordsHUD.instance.scale;
        if (tab == 3) return EnemyHUD.instance.scale;
        if (tab == 4) return NickedHUD.instance.scale;
        if (tab == 5) return FriendsHUD.instance.scale;
        if (tab == 6) return SessionStatsHUD.instance.scale;
        if (tab == 7) return EventHUD.instance.scale;
        if (tab == 8) return RegHUD.instance.scale;
        if (tab == 9) return DarksHUD.instance.scale; 
        if (tab == 10) return ToggleSprintModule.instance.scale;
        if (tab == 11) return CPSModule.instance.scale;
        if (tab == 12) return FPSModule.instance.scale;
        if (tab == 13) return BossBarModule.instance.scale;
        if (tab == 14) return TelebowHUD.instance.scale;
        if (tab == 15) return PlayerCounterHUD.instance.scale; // Scale mapping
        return 1.0f;
    }

    // --- ICONS & GEOMETRY ---
    
    public void drawMathGear(float x, float y, float radius, int color, float rotation) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        
        float scale = radius / 3.5f; 
        GL11.glScalef(scale, scale, 1.0f);
        
        String gear = "\u2630"; 
        
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        
        int tw = this.fontRendererObj.getStringWidth(gear);
        int th = this.fontRendererObj.FONT_HEIGHT;
        
        this.fontRendererObj.drawStringWithShadow(gear, -tw / 2.0f, -th / 2.0f + 1, color);
        
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    private void drawCross(float cx, float cy, float size, float thickness, int color) {
        setupSmoothRender(false);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        setColor(color); 
        GL11.glLineWidth(thickness);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(cx - size, cy - size); GL11.glVertex2f(cx + size, cy + size);
        GL11.glVertex2f(cx + size, cy - size); GL11.glVertex2f(cx - size, cy + size);
        GL11.glEnd(); 
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        endSmoothRender();
    }

    private void doGlScissor(int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getMinecraft();
        int scaleFactor = 1; int k = mc.gameSettings.guiScale; if (k == 0) k = 1000;
        while (scaleFactor < k && mc.displayWidth / (scaleFactor + 1) >= 320 && mc.displayHeight / (scaleFactor + 1) >= 240) { ++scaleFactor; }
        GL11.glScissor(x * scaleFactor, mc.displayHeight - (y + height) * scaleFactor, width * scaleFactor, height * scaleFactor);
    }

    private void drawInnerRoundedRect(float x, float y, float w, float h, float radius, int color, boolean hovered) {
        drawRoundedRect(x, y, w, h, radius, color);
        if (hovered) {
            drawRoundedRect(x, y, w, h, radius, COLOR_BTN_HOVER_OVERLAY); 
        }
        drawRoundedOutline(x, y, w, h, radius, 1.0f, 0x55FFFFFF);
    }

    private void drawIOSToggle(float x, float y, float cardW, String label, boolean isOn, int mx, int my) {
        this.fontRendererObj.drawStringWithShadow(label, x, y + 2, 0xDDDDDD);
        float swW = 24; float swH = 12; 
        float swX = x + cardW - swW - 4; float swY = y + 1;
        boolean hovered = isInside(mx, my, swX, swY, swW, swH);
        drawRoundedRect(swX, swY, swW, swH, 6, isOn ? COLOR_ENABLED : (hovered ? 0xFF555555 : 0xFF444444)); 
        float rad = swH / 2 - 1.0f; float cx = isOn ? swX + swW - rad - 1.0f : swX + rad + 1.0f;
        drawCircle(cx, swY + swH / 2, rad, 0xFFFFFFFF); 
    }

    private void drawSettingsButton(float x, float y, float w, float h, String text, int mx, int my) {
        boolean hovered = isInside(mx, my, x, y, w, h);
        drawInnerRoundedRect(x, y, w, h, 3, 0x33FFFFFF, hovered); 
        this.fontRendererObj.drawStringWithShadow(text, x + (w - this.fontRendererObj.getStringWidth(text)) / 2, y + (h - 8) / 2, -1);
    }

    private void drawSettingsCard(int x, int y, int w, int h) { drawRoundedRect(x, y, w, h, 4, 0x33000000); }

    private void drawIOSSlider(float x, float y, float val, float min, float max, float trackW) {
        String valText = String.format("%.1f", val);
        int textW = this.fontRendererObj.getStringWidth(valText);
        this.fontRendererObj.drawStringWithShadow(valText, x + trackW - textW, y - 6, 0xAAAAAA);
        float trackH = 4; float trackY = y + 4;
        drawRoundedRect(x, trackY, trackW, trackH, 2, 0xFF333333); 
        float pct = (val - min) / (max - min);
        float filledW = pct * trackW;
        if (filledW > trackH) drawRoundedRect(x, trackY, filledW, trackH, 2, COLOR_DISABLED); 
        float knobX = x + filledW; float knobY = trackY + 2;
        drawCircleOutline(knobX, knobY, 5.0f, 1.0f, 0x88000000); drawCircle(knobX, knobY, 4.0f, 0xFFFFFFFF); 
    }

    private void drawPalette(float x, float y, int current, int mx, int my, int targetId) {
        for (int i = 0; i < palette.length; i++) {
            float cx = x + (i * 22) + 6; float cy = y + 6;
            boolean hovered = isInside(mx, my, x + (i * 22), y, 12, 12);
            if (i == palette.length - 1) { 
                if (activeCustomColorTarget == targetId) drawCircle(cx, cy, 7.5f, COLOR_DISABLED); 
                else if (hovered) drawCircle(cx, cy, 7.5f, 0x55FFFFFF); 
                drawCircle(cx, cy, 6.5f, 0xFF222222); drawCircle(cx, cy, 5.5f, 0xFF111111); 
                this.fontRendererObj.drawStringWithShadow("+", cx - 2.5f, cy - 3.5f, 0xAAAAAA);
            } else {
                if (hovered && palette[i] != current) drawCircle(cx, cy, 7.5f, 0x55FFFFFF); 
                if ((palette[i] & 0x00FFFFFF) == (current & 0x00FFFFFF)) drawCircle(cx, cy, 7.5f, 0xFFFFFFFF); 
                drawCircle(cx, cy, 5.5f, palette[i] | 0xFF000000);
            }
        }
    }

    private void setupSmoothRender(boolean isGradient) { GlStateManager.pushMatrix(); GlStateManager.enableBlend(); GlStateManager.disableTexture2D(); GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0); GL11.glDisable(GL11.GL_LINE_SMOOTH); GL11.glDisable(GL11.GL_POLYGON_SMOOTH); GL11.glDisable(GL11.GL_CULL_FACE); if (isGradient) GlStateManager.shadeModel(GL11.GL_SMOOTH); }
    private void endSmoothRender() { GlStateManager.shadeModel(GL11.GL_FLAT); GL11.glEnable(GL11.GL_CULL_FACE); GlStateManager.enableTexture2D(); GlStateManager.disableBlend(); GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F); GlStateManager.popMatrix(); }
    private void setColor(int color) { float a = (color >> 24 & 0xFF) / 255.0F; float r = (color >> 16 & 0xFF) / 255.0F; float g = (color >> 8 & 0xFF) / 255.0F; float b = (color & 0xFF) / 255.0F; GlStateManager.color(r, g, b, a); }
    private void drawGradientRoundedRect(float x, float y, float width, float height, float radius, int topColor, int bottomColor) { setupSmoothRender(true); float x1 = x + width; float y1 = y + height; GL11.glBegin(GL11.GL_POLYGON); setColor(topColor); for (int i = 180; i <= 270; i += 5) GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(i)) * radius), (float)(y + radius + Math.sin(Math.toRadians(i)) * radius)); for (int i = 270; i <= 360; i += 5) GL11.glVertex2f((float)(x1 - radius + Math.cos(Math.toRadians(i)) * radius), (float)(y + radius + Math.sin(Math.toRadians(i)) * radius)); setColor(bottomColor); for (int i = 0; i <= 90; i += 5) GL11.glVertex2f((float)(x1 - radius + Math.cos(Math.toRadians(i)) * radius), (float)(y1 - radius + Math.sin(Math.toRadians(i)) * radius)); for (int i = 90; i <= 180; i += 5) GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(i)) * radius), (float)(y1 - radius + Math.sin(Math.toRadians(i)) * radius)); GL11.glEnd(); endSmoothRender(); }
    private void drawRoundedRect(float x, float y, float width, float height, float radius, int color) { setupSmoothRender(false); setColor(color); float x1 = x + width; float y1 = y + height; GL11.glBegin(GL11.GL_POLYGON); for (int i = 180; i <= 270; i += 5) GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(i)) * radius), (float)(y + radius + Math.sin(Math.toRadians(i)) * radius)); for (int i = 270; i <= 360; i += 5) GL11.glVertex2f((float)(x1 - radius + Math.cos(Math.toRadians(i)) * radius), (float)(y + radius + Math.sin(Math.toRadians(i)) * radius)); for (int i = 0; i <= 90; i += 5) GL11.glVertex2f((float)(x1 - radius + Math.cos(Math.toRadians(i)) * radius), (float)(y1 - radius + Math.sin(Math.toRadians(i)) * radius)); for (int i = 90; i <= 180; i += 5) GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(i)) * radius), (float)(y1 - radius + Math.sin(Math.toRadians(i)) * radius)); GL11.glEnd(); endSmoothRender(); }
    private void drawRoundedOutline(float x, float y, float width, float height, float radius, float lineWidth, int color) { setupSmoothRender(false); setColor(color); GL11.glLineWidth(lineWidth); float x1 = x + width; float y1 = y + height; GL11.glBegin(GL11.GL_LINE_LOOP); for (int i = 180; i <= 270; i += 5) GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(i)) * radius), (float)(y + radius + Math.sin(Math.toRadians(i)) * radius)); for (int i = 270; i <= 360; i += 5) GL11.glVertex2f((float)(x1 - radius + Math.cos(Math.toRadians(i)) * radius), (float)(y + radius + Math.sin(Math.toRadians(i)) * radius)); for (int i = 0; i <= 90; i += 5) GL11.glVertex2f((float)(x1 - radius + Math.cos(Math.toRadians(i)) * radius), (float)(y1 - radius + Math.sin(Math.toRadians(i)) * radius)); for (int i = 90; i <= 180; i += 5) GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(i)) * radius), (float)(y1 - radius + Math.sin(Math.toRadians(i)) * radius)); GL11.glEnd(); endSmoothRender(); }
    private void drawSolidRect(float x, float y, float w, float h, int color) { drawRoundedRect(x, y, w, h, 0, color); }
    private void drawCircle(float cx, float cy, float radius, int color) { setupSmoothRender(false); setColor(color); GL11.glBegin(GL11.GL_POLYGON); for (int i = 0; i <= 360; i += 5) GL11.glVertex2f((float)(cx + Math.cos(Math.toRadians(i)) * radius), (float)(cy + Math.sin(Math.toRadians(i)) * radius)); GL11.glEnd(); endSmoothRender(); }
    private void drawCircleOutline(float cx, float cy, float radius, float lineWidth, int color) { setupSmoothRender(false); setColor(color); GL11.glLineWidth(lineWidth); GL11.glBegin(GL11.GL_LINE_LOOP); for (int i = 0; i <= 360; i += 5) GL11.glVertex2f((float)(cx + Math.cos(Math.toRadians(i)) * radius), (float)(cy + Math.sin(Math.toRadians(i)) * radius)); GL11.glEnd(); endSmoothRender(); }
    private boolean isInside(float mx, float my, float x, float y, float w, float h) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }
}