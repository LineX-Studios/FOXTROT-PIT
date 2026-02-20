package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Handler.ConfigHandler;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;

public class HUDSettingsGui extends GuiScreen {
    private final GuiScreen previousScreen;
    // Added "CPS" to the tabs list
    private String[] tabs = {"Potion Status", "Armor Status", "Coordinates", "Enemy List", "Nicked List", "Friend List", "Session Stats", "Event Tracker", "Regularity List", "Toggle Sprint", "CPS"};
    private int selectedTab = 0;
    
    // UI Interaction States
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

    public HUDSettingsGui(GuiScreen previousScreen) { 
        this.previousScreen = previousScreen; 
    }

    public HUDSettingsGui(GuiScreen previousScreen, int defaultTab) { 
        this.previousScreen = previousScreen; 
        this.selectedTab = defaultTab;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawSolidRect(0, 0, this.width, this.height, 0x55000000);
        
        // --- SAFE HUD RENDERING ---
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
        if (ToggleSprintModule.instance.enabled) ToggleSprintModule.instance.render(true, mouseX, mouseY);
        if (CPSModule.enabled) CPSModule.instance.render(true, mouseX, mouseY);

        GlStateManager.popMatrix();

        GlStateManager.disableLighting();
        GlStateManager.enableBlend();

        int panelW = 460; int panelH = 320;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        drawRoundedRect(panelX + 4, panelY + 4, panelW, panelH, 6, 0x33000000); 
        drawGradientRoundedRect(panelX, panelY, panelW, panelH, 6, 0xFA1E1E1E, 0xFA141414); 
        
        drawSolidRect(panelX + 130, panelY + 15, 1, panelH - 30, 0x1AFFFFFF); 
        this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.WHITE + "Settings", panelX + 15, panelY + 15, -1);

        // --- TABS ---
        int tY = panelY + 35;
        for (int i = 0; i < tabs.length; i++) {
            boolean hovered = isInside(mouseX, mouseY, panelX + 10, tY, 115, 18);
            if (selectedTab == i) {
                drawNeonGlow(panelX + 10, tY, 115, 18, 6, 3.0f, 0x66FF3333); 
                drawGradientRoundedRect(panelX + 10, tY, 115, 18, 4, 0xFFFF4444, 0xFFE53935); 
                this.fontRendererObj.drawStringWithShadow(tabs[i], panelX + 22, tY + 5, -1);
            } else {
                if (hovered) drawRoundedRect(panelX + 10, tY, 115, 18, 4, 0x1AFFFFFF);
                this.fontRendererObj.drawStringWithShadow(tabs[i], panelX + 22, tY + 5, 0xAAAAAA);
            }
            tY += 24; 
        }

        // --- CONTENT AREA ---
        int rX = panelX + 145; int rY = panelY + 15;
        
        this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + tabs[selectedTab], rX, rY, -1);
        rY += 30; 

        // Scale Slider
        drawSettingsCard(rX, rY, 300, 60); 
        this.fontRendererObj.drawStringWithShadow("HUD Scale", rX + 10, rY + 8, 0xDDDDDD);
        drawIOSSlider(rX + 10, rY + 22, getScaleForTab(), 0.5f, 1.5f, 210); 
        drawIOSButton(rX + 10, rY + 40, 45, 14, "Reset", mouseX, mouseY); 
        
        rY += 70; 

        if (selectedTab == 0) { // POTIONS
            drawSettingsCard(rX, rY, 300, 85);
            this.fontRendererObj.drawStringWithShadow("Name Color", rX + 10, rY + 10, 0xDDDDDD);
            drawPalette(rX + 10, rY + 23, PotionHUD.nameColor, mouseX, mouseY, 1);
            this.fontRendererObj.drawStringWithShadow("Duration Color", rX + 10, rY + 50, 0xDDDDDD);
            drawPalette(rX + 10, rY + 63, PotionHUD.durationColor, mouseX, mouseY, 2);
        } else if (selectedTab == 1) { // ARMOR
            drawSettingsCard(rX, rY, 300, 75);
            drawIOSToggle(rX + 10, rY + 10, "Horizontal Layout", ArmorHUD.instance.isHorizontal, mouseX, mouseY);
            drawSolidRect(rX + 10, rY + 32, 280, 1, 0x11FFFFFF);
            this.fontRendererObj.drawStringWithShadow("Durability Color", rX + 10, rY + 42, 0xDDDDDD);
            drawPalette(rX + 10, rY + 55, ArmorHUD.durabilityColor, mouseX, mouseY, 3);
        } else if (selectedTab == 2) { // COORDS
            drawSettingsCard(rX, rY, 300, 135);
            drawIOSToggle(rX + 10, rY + 10, "Horizontal Layout", CoordsHUD.instance.isHorizontal, mouseX, mouseY);
            drawSolidRect(rX + 10, rY + 30, 280, 1, 0x11FFFFFF);
            this.fontRendererObj.drawStringWithShadow("Axis Color (X: Y: Z:)", rX + 10, rY + 40, 0xDDDDDD);
            drawPalette(rX + 10, rY + 52, CoordsHUD.axisColor, mouseX, mouseY, 4);
            this.fontRendererObj.drawStringWithShadow("Value Color (+100, 50, 300)", rX + 10, rY + 72, 0xDDDDDD);
            drawPalette(rX + 10, rY + 84, CoordsHUD.numberColor, mouseX, mouseY, 5);
            this.fontRendererObj.drawStringWithShadow("Direction Color (NE, +)", rX + 10, rY + 104, 0xDDDDDD);
            drawPalette(rX + 10, rY + 116, CoordsHUD.directionColor, mouseX, mouseY, 6);
        } else if (selectedTab == 9) { // TOGGLE SPRINT
            drawSettingsCard(rX, rY, 300, 160);
            drawIOSToggle(rX + 10, rY + 5, "Enable Module", ToggleSprintModule.instance.enabled, mouseX, mouseY);
            drawIOSToggle(rX + 10, rY + 23, "Toggle Sprint", ToggleSprintModule.instance.toggleSprint, mouseX, mouseY);
            drawIOSToggle(rX + 10, rY + 41, "Toggle Sneak (Disabled in Hypixel)", ToggleSprintModule.instance.toggleSneak, mouseX, mouseY);
            drawIOSToggle(rX + 10, rY + 59, "W-Tap", ToggleSprintModule.instance.wTapFix, mouseX, mouseY);
            drawIOSToggle(rX + 10, rY + 77, "Fly Boost (Hold Key)", ToggleSprintModule.instance.flyBoost, mouseX, mouseY);
            
            drawSolidRect(rX + 10, rY + 95, 280, 1, 0x11FFFFFF);
            
            this.fontRendererObj.drawStringWithShadow("Fly Boost Amount", rX + 10, rY + 100, 0xDDDDDD);
            drawIOSSlider(rX + 10, rY + 112, ToggleSprintModule.instance.flyBoostAmount, 1.0f, 10.0f, 210);
            
            drawSolidRect(rX + 10, rY + 130, 280, 1, 0x11FFFFFF);
            
            this.fontRendererObj.drawStringWithShadow("Text Color", rX + 10, rY + 135, 0xDDDDDD);
            drawPalette(rX + 10, rY + 145, ToggleSprintModule.instance.textColor, mouseX, mouseY, 7);
        } else if (selectedTab == 10) { // CPS
            drawSettingsCard(rX, rY, 300, 85);
            this.fontRendererObj.drawStringWithShadow("Text Color", rX + 10, rY + 10, 0xDDDDDD);
            drawPalette(rX + 10, rY + 23, CPSModule.textColor, mouseX, mouseY, 8);
            this.fontRendererObj.drawStringWithShadow("Background Color", rX + 10, rY + 50, 0xDDDDDD);
            drawPalette(rX + 10, rY + 63, CPSModule.backgroundColor, mouseX, mouseY, 9);
        }

        // --- RENDER VISUAL COLOR PICKER POPUP ---
        if (activeCustomColorTarget != -1) {
            drawColorPickerPopup(pickerX, pickerY, mouseX, mouseY);
        }

        drawIOSButton(panelX + panelW - 75, panelY + panelH - 25, 60, 16, "Return", mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawColorPickerPopup(int x, int y, int mouseX, int mouseY) {
        if (draggingColorBox) {
            currentSat = Math.max(0, Math.min(1, (mouseX - x) / 100f));
            currentBri = Math.max(0, Math.min(1, 1f - ((mouseY - y) / 100f)));
            updateTargetColor();
        } else if (draggingHueBar) {
            currentHue = Math.max(0, Math.min(1, (mouseY - y) / 100f));
            updateTargetColor();
        }

        drawRoundedRect(x - 5, y - 5, 135, 110, 4, 0xFA1E1E1E);

        setupSmoothRender(true);
        GL11.glBegin(GL11.GL_QUADS);
        setColor(0xFFFFFFFF); GL11.glVertex2f(x, y); 
        setColor(0xFF000000); GL11.glVertex2f(x, y + 100); 
        setColor(0xFF000000); GL11.glVertex2f(x + 100, y + 100); 
        setColor(Color.HSBtoRGB(currentHue, 1.0f, 1.0f) | 0xFF000000); GL11.glVertex2f(x + 100, y); 
        GL11.glEnd();
        endSmoothRender();

        float dotX = x + (currentSat * 100);
        float dotY = y + ((1f - currentBri) * 100);
        drawCircle(dotX, dotY, 4.0f, 0xFF000000);
        drawCircle(dotX, dotY, 3.0f, 0xFFFFFFFF);

        setupSmoothRender(true);
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i <= 20; i++) {
            float hueStep = i / 20f;
            setColor(Color.HSBtoRGB(hueStep, 1.0f, 1.0f) | 0xFF000000);
            GL11.glVertex2f(x + 110, y + (hueStep * 100));
            GL11.glVertex2f(x + 120, y + (hueStep * 100));
        }
        GL11.glEnd();
        endSmoothRender();

        float hueY = y + (currentHue * 100);
        drawSolidRect(x + 108, hueY - 1, 14, 3, 0xFFFFFFFF);
        drawSolidRect(x + 109, hueY, 12, 1, 0xFF000000);
    }

    private void updateTargetColor() {
        // Only strip the alpha for specific variables if you don't want transparency,
        // but CPS background color usually uses an alpha, so we handle it uniquely!
        int finalColor = Color.HSBtoRGB(currentHue, currentSat, currentBri);
        
        if (activeCustomColorTarget == 1) PotionHUD.nameColor = finalColor & 0x00FFFFFF;
        else if (activeCustomColorTarget == 2) PotionHUD.durationColor = finalColor & 0x00FFFFFF;
        else if (activeCustomColorTarget == 3) ArmorHUD.durabilityColor = finalColor & 0x00FFFFFF;
        else if (activeCustomColorTarget == 4) CoordsHUD.axisColor = finalColor & 0x00FFFFFF;
        else if (activeCustomColorTarget == 5) CoordsHUD.numberColor = finalColor & 0x00FFFFFF;
        else if (activeCustomColorTarget == 6) CoordsHUD.directionColor = finalColor & 0x00FFFFFF;
        else if (activeCustomColorTarget == 7) ToggleSprintModule.instance.textColor = finalColor & 0x00FFFFFF;
        else if (activeCustomColorTarget == 8) CPSModule.textColor = finalColor & 0x00FFFFFF;
        else if (activeCustomColorTarget == 9) {
            // Keep it 43% transparent for the CPS background, exactly like CheatBreaker
            CPSModule.backgroundColor = (finalColor & 0x00FFFFFF) | 0x6F000000;
        }
    }

    private void openCustomColorPicker(int targetId, int x, int y, int currentColor) {
        activeCustomColorTarget = targetId;
        pickerX = x + 15;
        pickerY = y - 45;
        
        float[] hsb = Color.RGBtoHSB((currentColor >> 16) & 0xFF, (currentColor >> 8) & 0xFF, currentColor & 0xFF, null);
        currentHue = hsb[0];
        currentSat = hsb[1];
        currentBri = hsb[2];
    }

    private void setupSmoothRender(boolean isGradient) {
        GlStateManager.pushMatrix(); 
        GlStateManager.enableBlend(); 
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0); 
        GL11.glDisable(GL11.GL_LINE_SMOOTH); 
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH); 
        GL11.glDisable(GL11.GL_CULL_FACE); 
        if (isGradient) GlStateManager.shadeModel(GL11.GL_SMOOTH);
    }

    private void endSmoothRender() {
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GlStateManager.enableTexture2D(); 
        GlStateManager.disableBlend(); 
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F); 
        GlStateManager.popMatrix();
    }

    private void setColor(int color) {
        float a = (color >> 24 & 0xFF) / 255.0F;
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        GlStateManager.color(r, g, b, a);
    }

    private void drawNeonGlow(float x, float y, float width, float height, float radius, float spread, int color) {
        float a = (color >> 24 & 0xFF) / 255.0F;
        for (float s = spread; s > 0; s -= 1.0f) {
            float currentAlpha = a * (1.0f - (s / spread)); 
            int c = ((int)(currentAlpha * 255) << 24) | (color & 0x00FFFFFF);
            drawRoundedRect(x - s, y - s, width + (s*2), height + (s*2), radius + s, c);
        }
    }

    private void drawGradientRoundedRect(float x, float y, float width, float height, float radius, int topColor, int bottomColor) {
        setupSmoothRender(true);
        float x1 = x + width; float y1 = y + height;
        
        GL11.glBegin(GL11.GL_POLYGON);
        setColor(topColor);
        for (int i = 180; i <= 270; i += 5) GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(i)) * radius), (float)(y + radius + Math.sin(Math.toRadians(i)) * radius));
        for (int i = 270; i <= 360; i += 5) GL11.glVertex2f((float)(x1 - radius + Math.cos(Math.toRadians(i)) * radius), (float)(y + radius + Math.sin(Math.toRadians(i)) * radius));
        setColor(bottomColor);
        for (int i = 0; i <= 90; i += 5) GL11.glVertex2f((float)(x1 - radius + Math.cos(Math.toRadians(i)) * radius), (float)(y1 - radius + Math.sin(Math.toRadians(i)) * radius));
        for (int i = 90; i <= 180; i += 5) GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(i)) * radius), (float)(y1 - radius + Math.sin(Math.toRadians(i)) * radius));
        GL11.glEnd();
        endSmoothRender();
    }

    private void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        setupSmoothRender(false);
        setColor(color);
        float x1 = x + width; float y1 = y + height;
        
        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 180; i <= 270; i += 5) GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(i)) * radius), (float)(y + radius + Math.sin(Math.toRadians(i)) * radius));
        for (int i = 270; i <= 360; i += 5) GL11.glVertex2f((float)(x1 - radius + Math.cos(Math.toRadians(i)) * radius), (float)(y + radius + Math.sin(Math.toRadians(i)) * radius));
        for (int i = 0; i <= 90; i += 5) GL11.glVertex2f((float)(x1 - radius + Math.cos(Math.toRadians(i)) * radius), (float)(y1 - radius + Math.sin(Math.toRadians(i)) * radius));
        for (int i = 90; i <= 180; i += 5) GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(i)) * radius), (float)(y1 - radius + Math.sin(Math.toRadians(i)) * radius));
        GL11.glEnd();
        endSmoothRender();
    }

    private void drawCircle(float cx, float cy, float radius, int color) {
        setupSmoothRender(false);
        setColor(color);
        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i <= 360; i += 5) GL11.glVertex2f((float)(cx + Math.cos(Math.toRadians(i)) * radius), (float)(cy + Math.sin(Math.toRadians(i)) * radius));
        GL11.glEnd();
        endSmoothRender();
    }

    private void drawSolidRect(float x, float y, float w, float h, int color) {
        drawRoundedRect(x, y, w, h, 0, color);
    }

    private void drawPalette(float x, float y, int current, int mx, int my, int targetId) {
        for (int i = 0; i < palette.length; i++) {
            float cx = x + (i * 22) + 6; 
            float cy = y + 6;
            boolean hovered = isInside(mx, my, x + (i * 22), y, 12, 12);
            
            if (i == palette.length - 1) { 
                if (activeCustomColorTarget == targetId) {
                    drawCircle(cx, cy, 7.5f, 0xFFE53935); 
                } else if (hovered) {
                    drawCircle(cx, cy, 7.5f, 0x55FFFFFF); 
                }
                
                drawCircle(cx, cy, 6.5f, 0xFF222222); 
                drawCircle(cx, cy, 5.5f, 0xFF111111); 
                this.fontRendererObj.drawStringWithShadow("+", cx - 2.5f, cy - 3.5f, 0xAAAAAA);
                
            } else {
                if (hovered && palette[i] != current) {
                    drawCircle(cx, cy, 7.5f, 0x55FFFFFF); 
                }
                // Need to correctly ignore alpha when checking if color is active for CPS Background
                if ((palette[i] & 0x00FFFFFF) == (current & 0x00FFFFFF)) {
                    drawCircle(cx, cy, 7.5f, 0xFFFFFFFF); 
                }
                drawCircle(cx, cy, 5.5f, palette[i] | 0xFF000000);
            }
        }
    }

    private void drawIOSSlider(float x, float y, float val, float min, float max, float trackW) {
        float trackH = 2; 
        float trackY = y + 4;
        
        drawRoundedRect(x, trackY, trackW, trackH, 1, 0xFF444444); 
        
        float pct = (val - min) / (max - min);
        float filledW = pct * trackW;
        if (filledW > trackH) {
            drawRoundedRect(x, trackY, filledW, trackH, 1, 0xFFE53935); 
        }
        
        float knobX = x + filledW;
        float knobY = trackY + 1;
        
        drawCircle(knobX, knobY, 4.5f, 0xFF222222); 
        drawCircle(knobX, knobY, 3.5f, 0xFFFFFFFF); 
        
        this.fontRendererObj.drawStringWithShadow(String.format("%.2fx", val), x + trackW + 10, y + 1, 0xAAAAAA);
    }

    private void drawIOSToggle(float x, float y, String label, boolean isOn, int mx, int my) {
        this.fontRendererObj.drawStringWithShadow(label, x, y + 3, 0xDDDDDD);
        float swW = 28; float swH = 14; 
        float swX = x + 245; float swY = y;
        boolean hovered = isInside(mx, my, swX, swY, swW, swH);
        
        if (isOn) {
            drawNeonGlow(swX, swY, swW, swH, swH / 2, 2.5f, 0x55E53935); 
            drawRoundedRect(swX, swY, swW, swH, swH / 2, 0xFFE53935); 
        } else {
            drawRoundedRect(swX, swY, swW, swH, swH / 2, hovered ? 0xFF555555 : 0xFF444444); 
        }
        
        float rad = swH / 2 - 1.5f;
        float cx = isOn ? swX + swW - rad - 1.5f : swX + rad + 1.5f;
        
        drawCircle(cx, swY + swH / 2, rad, 0xFFFFFFFF); 
    }

    private void drawIOSButton(float x, float y, float w, float h, String text, int mx, int my) {
        boolean hovered = isInside(mx, my, x, y, w, h);
        drawRoundedRect(x, y, w, h, h / 2, hovered ? 0xFF3D3D3D : 0xFF2A2A2A); 
        this.fontRendererObj.drawStringWithShadow(text, x + (w - this.fontRendererObj.getStringWidth(text)) / 2, y + (h - 8) / 2, -1);
    }

    private void drawSettingsCard(int x, int y, int w, int h) {
        drawRoundedRect(x, y, w, h, 4, 0x11FFFFFF);
    }

    private boolean isInside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) return;
        
        if (activeCustomColorTarget != -1) {
            if (isInside(mouseX, mouseY, pickerX, pickerY, 100, 100)) { draggingColorBox = true; return; }
            if (isInside(mouseX, mouseY, pickerX + 110, pickerY, 10, 100)) { draggingHueBar = true; return; }
            activeCustomColorTarget = -1;
        }

        int panelW = 460; int panelH = 320;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        if (isInside(mouseX, mouseY, panelX + panelW - 75, panelY + panelH - 25, 60, 16)) {
            ConfigHandler.saveConfig(); this.mc.displayGuiScreen(previousScreen); return;
        }

        int tY = panelY + 35;
        for (int i = 0; i < tabs.length; i++) {
            if (isInside(mouseX, mouseY, panelX + 10, tY, 115, 18)) { 
                selectedTab = i; activeCustomColorTarget = -1; return; 
            }
            tY += 24;
        }

        int rX = panelX + 145; int rY = panelY + 45; 

        if (isInside(mouseX, mouseY, rX + 10, rY + 40, 45, 14)) { resetScaleForTab(); return; } 
        if (isInside(mouseX, mouseY, rX + 10, rY + 20, 210, 10)) { draggingSlider = true; return; } 
        rY += 70; 

        if (selectedTab == 0) { 
            for (int i = 0; i < palette.length; i++) {
                if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 23, 12, 12)) {
                    if (i == palette.length - 1) { openCustomColorPicker(1, rX + 10 + (i * 22), rY + 23, PotionHUD.nameColor); } 
                    else { PotionHUD.nameColor = palette[i]; } return; 
                }
            }
            for (int i = 0; i < palette.length; i++) {
                if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 63, 12, 12)) {
                    if (i == palette.length - 1) { openCustomColorPicker(2, rX + 10 + (i * 22), rY + 63, PotionHUD.durationColor); }
                    else { PotionHUD.durationColor = palette[i]; } return; 
                }
            }
        }
        else if (selectedTab == 1) { 
            if (isInside(mouseX, mouseY, rX + 260, rY + 10, 28, 14)) { ArmorHUD.instance.isHorizontal = !ArmorHUD.instance.isHorizontal; return; }
            for (int i = 0; i < palette.length; i++) {
                if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 55, 12, 12)) {
                    if (i == palette.length - 1) { openCustomColorPicker(3, rX + 10 + (i * 22), rY + 55, ArmorHUD.durabilityColor); }
                    else { ArmorHUD.durabilityColor = palette[i]; } return; 
                }
            }
        }
        else if (selectedTab == 2) { 
            if (isInside(mouseX, mouseY, rX + 260, rY + 10, 28, 14)) { CoordsHUD.instance.isHorizontal = !CoordsHUD.instance.isHorizontal; return; }
            for (int i = 0; i < palette.length; i++) {
                if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 52, 12, 12)) {
                    if (i == palette.length - 1) { openCustomColorPicker(4, rX + 10 + (i * 22), rY + 52, CoordsHUD.axisColor); }
                    else { CoordsHUD.axisColor = palette[i]; } return; 
                }
            }
            for (int i = 0; i < palette.length; i++) {
                if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 84, 12, 12)) {
                    if (i == palette.length - 1) { openCustomColorPicker(5, rX + 10 + (i * 22), rY + 84, CoordsHUD.numberColor); }
                    else { CoordsHUD.numberColor = palette[i]; } return; 
                }
            }
            for (int i = 0; i < palette.length; i++) {
                if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 116, 12, 12)) {
                    if (i == palette.length - 1) { openCustomColorPicker(6, rX + 10 + (i * 22), rY + 116, CoordsHUD.directionColor); }
                    else { CoordsHUD.directionColor = palette[i]; } return; 
                }
            }
        }
        else if (selectedTab == 9) { 
            if (isInside(mouseX, mouseY, rX + 255, rY + 5, 28, 14)) { ToggleSprintModule.instance.enabled = !ToggleSprintModule.instance.enabled; return; }
            if (isInside(mouseX, mouseY, rX + 255, rY + 23, 28, 14)) { ToggleSprintModule.instance.toggleSprint = !ToggleSprintModule.instance.toggleSprint; return; }
            if (isInside(mouseX, mouseY, rX + 255, rY + 41, 28, 14)) { ToggleSprintModule.instance.toggleSneak = !ToggleSprintModule.instance.toggleSneak; return; }
            if (isInside(mouseX, mouseY, rX + 255, rY + 59, 28, 14)) { ToggleSprintModule.instance.wTapFix = !ToggleSprintModule.instance.wTapFix; return; }
            if (isInside(mouseX, mouseY, rX + 255, rY + 77, 28, 14)) { ToggleSprintModule.instance.flyBoost = !ToggleSprintModule.instance.flyBoost; return; }
            
            if (isInside(mouseX, mouseY, rX + 10, rY + 110, 210, 10)) { draggingFlySlider = true; return; }
            
            for (int i = 0; i < palette.length; i++) {
                if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 145, 12, 12)) {
                    if (i == palette.length - 1) { openCustomColorPicker(7, rX + 10 + (i * 22), rY + 145, ToggleSprintModule.instance.textColor); }
                    else { ToggleSprintModule.instance.textColor = palette[i]; } return; 
                }
            }
        }
        else if (selectedTab == 10) { // CPS CLICKS
            for (int i = 0; i < palette.length; i++) {
                if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 23, 12, 12)) {
                    if (i == palette.length - 1) { openCustomColorPicker(8, rX + 10 + (i * 22), rY + 23, CPSModule.textColor); } 
                    else { CPSModule.textColor = palette[i]; } return; 
                }
            }
            for (int i = 0; i < palette.length; i++) {
                if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 63, 12, 12)) {
                    if (i == palette.length - 1) { openCustomColorPicker(9, rX + 10 + (i * 22), rY + 63, CPSModule.backgroundColor); }
                    else { CPSModule.backgroundColor = palette[i] | 0x6F000000; } return; 
                }
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingColorBox) {
            currentSat = Math.max(0, Math.min(1, (mouseX - pickerX) / 100f));
            currentBri = Math.max(0, Math.min(1, 1f - ((mouseY - pickerY) / 100f)));
            updateTargetColor();
        } else if (draggingHueBar) {
            currentHue = Math.max(0, Math.min(1, (mouseY - pickerY) / 100f));
            updateTargetColor();
        } else if (draggingSlider) {
            int panelW = 460; int panelX = (this.width - panelW) / 2; int rX = panelX + 145;
            float pct = (float)(mouseX - (rX + 10)) / 210f; 
            pct = Math.max(0, Math.min(1, pct));
            assignScale(0.5f + (1.0f * pct));
        } else if (draggingFlySlider) {
            int panelW = 460; int panelX = (this.width - panelW) / 2; int rX = panelX + 145;
            float pct = (float)(mouseX - (rX + 10)) / 210f; 
            pct = Math.max(0, Math.min(1, pct));
            ToggleSprintModule.instance.flyBoostAmount = 1.0f + (9.0f * pct); 
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) { 
        draggingSlider = false; 
        draggingColorBox = false;
        draggingHueBar = false;
        draggingFlySlider = false;
        ConfigHandler.saveConfig(); 
    }

    private void assignScale(float val) {
        if (selectedTab == 0) PotionHUD.instance.scale = val;
        else if (selectedTab == 1) ArmorHUD.instance.scale = val;
        else if (selectedTab == 2) CoordsHUD.instance.scale = val;
        else if (selectedTab == 3) EnemyHUD.instance.scale = val;
        else if (selectedTab == 4) NickedHUD.instance.scale = val;
        else if (selectedTab == 5) FriendsHUD.instance.scale = val;
        else if (selectedTab == 6) SessionStatsHUD.instance.scale = val;
        else if (selectedTab == 7) EventHUD.instance.scale = val;
        else if (selectedTab == 8) RegHUD.instance.scale = val;
        else if (selectedTab == 9) ToggleSprintModule.instance.scale = val;
        else if (selectedTab == 10) CPSModule.instance.scale = val;
    }

    private float getScaleForTab() {
        if (selectedTab == 0) return PotionHUD.instance.scale;
        if (selectedTab == 1) return ArmorHUD.instance.scale;
        if (selectedTab == 2) return CoordsHUD.instance.scale;
        if (selectedTab == 3) return EnemyHUD.instance.scale;
        if (selectedTab == 4) return NickedHUD.instance.scale;
        if (selectedTab == 5) return FriendsHUD.instance.scale;
        if (selectedTab == 6) return SessionStatsHUD.instance.scale;
        if (selectedTab == 7) return EventHUD.instance.scale;
        if (selectedTab == 8) return RegHUD.instance.scale;
        if (selectedTab == 9) return ToggleSprintModule.instance.scale;
        if (selectedTab == 10) return CPSModule.instance.scale;
        return 1.0f;
    }

    private void resetScaleForTab() { assignScale(1.0f); }
}