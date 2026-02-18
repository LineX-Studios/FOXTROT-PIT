package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Handler.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public class HUDSettingsGui extends GuiScreen {
    private final GuiScreen previousScreen;
    private String[] tabs = {"Potion Status", "Armor Status", "Coordinates", "Enemy List", "Nicked List", "Friend List", "Session Stats", "Event Tracker", "Regularity List"};
    private int selectedTab = 0;
    private boolean draggingSlider = false;

    private int[] palette = {0xFFFFFF, 0xAAAAAA, 0x555555, 0xFF5555, 0x55FF55, 0x5555FF, 0xFFFF55, 0x55FFFF, 0xFFAA00, 0xFF55FF, 0x000000};

    public HUDSettingsGui(GuiScreen previousScreen) { 
        this.previousScreen = previousScreen; 
    }

    public HUDSettingsGui(GuiScreen previousScreen, int defaultTab) { 
        this.previousScreen = previousScreen; 
        this.selectedTab = defaultTab;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Frosty background
        drawSolidRect(0, 0, this.width, this.height, 0x55000000);
        
        // --- OPENGL SAFETY LOCK ---
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        // --- RENDER HUDS (FIXED: Passed 'true' so dummy data shows up!) ---
        if (PotionHUD.enabled) PotionHUD.instance.render(true, mouseX, mouseY);
        if (ArmorHUD.enabled) ArmorHUD.instance.render(true, mouseX, mouseY);
        if (CoordsHUD.enabled) CoordsHUD.instance.render(true, mouseX, mouseY);
        if (EnemyHUD.enabled) EnemyHUD.instance.render(true, mouseX, mouseY);
        if (NickedHUD.enabled) NickedHUD.instance.render(true, mouseX, mouseY);
        if (FriendsHUD.enabled) FriendsHUD.instance.render(true, mouseX, mouseY);
        if (SessionStatsHUD.enabled) SessionStatsHUD.instance.render(true, mouseX, mouseY);
        if (EventHUD.enabled) EventHUD.instance.render(true, mouseX, mouseY);
        if (RegHUD.enabled) RegHUD.instance.render(true, mouseX, mouseY);

        // --- RESTORE PURE OPENGL STATE AFTER RENDERING HUDS ---
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();

        int panelW = 460; 
        int panelH = 280;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        // --- SEAMLESS GRADIENT BASE ---
        drawRoundedRect(panelX + 4, panelY + 4, panelW, panelH, 6, 0x33000000); 
        drawGradientRoundedRect(panelX, panelY, panelW, panelH, 6, 0xFA1E1E1E, 0xFA141414); 
        
        // Seamless sidebar
        drawSolidRect(panelX + 130, panelY + 15, 1, panelH - 30, 0x1AFFFFFF); 
        
        this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.WHITE + "Settings", panelX + 15, panelY + 15, -1);

        // --- TABS ---
        int tY = panelY + 35;
        for (int i = 0; i < tabs.length; i++) {
            boolean hovered = isInside(mouseX, mouseY, panelX + 10, tY, 115, 18);
            if (selectedTab == i) {
                // REDUCED UNDERGLOW: Spread is now 3.0f, Opacity is 0x66
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
        int rX = panelX + 145; 
        int rY = panelY + 15;
        
        this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + tabs[selectedTab], rX, rY, -1);
        rY += 30; 

        // 1. Scale Card
        drawSettingsCard(rX, rY, 300, 60); 
        this.fontRendererObj.drawStringWithShadow("HUD Scale", rX + 10, rY + 8, 0xDDDDDD);
        drawIOSSlider(rX + 10, rY + 22, getScaleForTab(), 0.5f, 1.5f, 210); 
        
        // Reset Button underneath the slider
        drawIOSButton(rX + 10, rY + 40, 45, 14, "Reset", mouseX, mouseY); 
        
        rY += 70; 

        // 2. Module Settings
        if (selectedTab == 0) { 
            drawSettingsCard(rX, rY, 300, 80);
            drawIOSToggle(rX + 10, rY + 10, "Horizontal Layout", PotionHUD.instance.isHorizontal, mouseX, mouseY);
            drawSolidRect(rX + 10, rY + 32, 280, 1, 0x11FFFFFF); 
            this.fontRendererObj.drawStringWithShadow("Text Color", rX + 10, rY + 42, 0xDDDDDD);
            drawPalette(rX + 10, rY + 55, PotionHUD.nameColor, mouseX, mouseY);
        } else if (selectedTab == 1) { 
            drawSettingsCard(rX, rY, 300, 35);
            drawIOSToggle(rX + 10, rY + 10, "Horizontal Layout", ArmorHUD.instance.isHorizontal, mouseX, mouseY);
        } else if (selectedTab == 2) { 
            drawSettingsCard(rX, rY, 300, 135);
            drawIOSToggle(rX + 10, rY + 10, "Horizontal Layout", CoordsHUD.instance.isHorizontal, mouseX, mouseY);
            drawSolidRect(rX + 10, rY + 30, 280, 1, 0x11FFFFFF);
            
            this.fontRendererObj.drawStringWithShadow("Axis Color (X: Y: Z:)", rX + 10, rY + 40, 0xDDDDDD);
            drawPalette(rX + 10, rY + 52, CoordsHUD.axisColor, mouseX, mouseY);
            
            this.fontRendererObj.drawStringWithShadow("Value Color (+100)", rX + 10, rY + 72, 0xDDDDDD);
            drawPalette(rX + 10, rY + 84, CoordsHUD.numberColor, mouseX, mouseY);
            
            this.fontRendererObj.drawStringWithShadow("Cardinal Color (NE, +)", rX + 10, rY + 104, 0xDDDDDD);
            drawPalette(rX + 10, rY + 116, CoordsHUD.directionColor, mouseX, mouseY);
        }

        drawIOSButton(panelX + panelW - 75, panelY + panelH - 25, 60, 16, "Return", mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawSettingsCard(int x, int y, int w, int h) {
        drawRoundedRect(x, y, w, h, 4, 0x11FFFFFF);
        drawRoundedOutline(x, y, w, h, 4, 1.0f, 0x11FFFFFF);
    }

    // ====================================================================
    // --- VECTORIZED PURE OPENGL SHAPES & TRUE NEON GLOW ---
    // ====================================================================

    private void setupSmoothRender(boolean isGradient) {
        GlStateManager.pushMatrix(); 
        GlStateManager.enableBlend(); 
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0); 
        
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glDisable(GL11.GL_CULL_FACE); 
        
        if (isGradient) {
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
        }
    }

    private void endSmoothRender() {
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
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

    // ADJUSTABLE NEON GLOW (Spread dictates width, Alpha dictates brightness)
    private void drawNeonGlow(float x, float y, float width, float height, float radius, float spread, int color) {
        setupSmoothRender(true);
        
        float a = (color >> 24 & 0xFF) / 255.0F;
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        GL11.glBegin(GL11.GL_QUAD_STRIP);
        
        float x1 = x + width; float y1 = y + height;
        
        for (int i = 180; i <= 270; i += 5) {
            GL11.glColor4f(r, g, b, 0.0f);
            GL11.glVertex2f((float)(x + radius - spread + Math.cos(Math.toRadians(i)) * (radius + spread)), (float)(y + radius - spread + Math.sin(Math.toRadians(i)) * (radius + spread)));
            GL11.glColor4f(r, g, b, a);
            GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(i)) * radius), (float)(y + radius + Math.sin(Math.toRadians(i)) * radius));
        }
        for (int i = 270; i <= 360; i += 5) {
            GL11.glColor4f(r, g, b, 0.0f);
            GL11.glVertex2f((float)(x1 - radius + spread + Math.cos(Math.toRadians(i)) * (radius + spread)), (float)(y + radius - spread + Math.sin(Math.toRadians(i)) * (radius + spread)));
            GL11.glColor4f(r, g, b, a);
            GL11.glVertex2f((float)(x1 - radius + Math.cos(Math.toRadians(i)) * radius), (float)(y + radius + Math.sin(Math.toRadians(i)) * radius));
        }
        for (int i = 0; i <= 90; i += 5) {
            GL11.glColor4f(r, g, b, 0.0f);
            GL11.glVertex2f((float)(x1 - radius + spread + Math.cos(Math.toRadians(i)) * (radius + spread)), (float)(y1 - radius + spread + Math.sin(Math.toRadians(i)) * (radius + spread)));
            GL11.glColor4f(r, g, b, a);
            GL11.glVertex2f((float)(x1 - radius + Math.cos(Math.toRadians(i)) * radius), (float)(y1 - radius + Math.sin(Math.toRadians(i)) * radius));
        }
        for (int i = 90; i <= 180; i += 5) {
            GL11.glColor4f(r, g, b, 0.0f);
            GL11.glVertex2f((float)(x + radius - spread + Math.cos(Math.toRadians(i)) * (radius + spread)), (float)(y1 - radius + spread + Math.sin(Math.toRadians(i)) * (radius + spread)));
            GL11.glColor4f(r, g, b, a);
            GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(i)) * radius), (float)(y1 - radius + Math.sin(Math.toRadians(i)) * radius));
        }
        
        GL11.glColor4f(r, g, b, 0.0f);
        GL11.glVertex2f((float)(x + radius - spread + Math.cos(Math.toRadians(180)) * (radius + spread)), (float)(y + radius - spread + Math.sin(Math.toRadians(180)) * (radius + spread)));
        GL11.glColor4f(r, g, b, a);
        GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(180)) * radius), (float)(y + radius + Math.sin(Math.toRadians(180)) * radius));
        
        GL11.glEnd();
        endSmoothRender();
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

        GL11.glLineWidth(1.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 180; i <= 270; i += 5) GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(i)) * radius), (float)(y + radius + Math.sin(Math.toRadians(i)) * radius));
        for (int i = 270; i <= 360; i += 5) GL11.glVertex2f((float)(x1 - radius + Math.cos(Math.toRadians(i)) * radius), (float)(y + radius + Math.sin(Math.toRadians(i)) * radius));
        for (int i = 0; i <= 90; i += 5) GL11.glVertex2f((float)(x1 - radius + Math.cos(Math.toRadians(i)) * radius), (float)(y1 - radius + Math.sin(Math.toRadians(i)) * radius));
        for (int i = 90; i <= 180; i += 5) GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(i)) * radius), (float)(y1 - radius + Math.sin(Math.toRadians(i)) * radius));
        GL11.glEnd();
        
        endSmoothRender();
    }

    private void drawRoundedOutline(float x, float y, float width, float height, float radius, float lineWidth, int color) {
        setupSmoothRender(false);
        setColor(color);
        GL11.glLineWidth(lineWidth);
        float x1 = x + width; float y1 = y + height;
        
        GL11.glBegin(GL11.GL_LINE_LOOP);
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
        GL11.glLineWidth(1.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= 360; i += 5) GL11.glVertex2f((float)(cx + Math.cos(Math.toRadians(i)) * radius), (float)(cy + Math.sin(Math.toRadians(i)) * radius));
        GL11.glEnd();
        endSmoothRender();
    }

    private void drawCircleOutline(float cx, float cy, float radius, float lineWidth, int color) {
        setupSmoothRender(false);
        setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= 360; i += 5) GL11.glVertex2f((float)(cx + Math.cos(Math.toRadians(i)) * radius), (float)(cy + Math.sin(Math.toRadians(i)) * radius));
        GL11.glEnd();
        endSmoothRender();
    }

    private void drawSolidRect(float x, float y, float w, float h, int color) {
        drawRoundedRect(x, y, w, h, 0, color);
    }

    // --- REFINED SMALLER COMPONENTS ---

    private void drawPalette(float x, float y, int current, int mx, int my) {
        for (int i = 0; i < palette.length; i++) {
            float cx = x + (i * 22) + 6; 
            float cy = y + 6;
            boolean hovered = isInside(mx, my, x + (i * 22), y, 12, 12);
            
            if (hovered && palette[i] != current) {
                drawCircleOutline(cx, cy, 7.5f, 1.5f, 0x55FFFFFF);
            }
            
            drawCircle(cx, cy, 5.5f, palette[i] | 0xFF000000);
            if (palette[i] == current) {
                drawCircleOutline(cx, cy, 7.5f, 1.5f, 0xFFFFFFFF);
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
        
        drawCircleOutline(knobX, knobY, 4.5f, 1.0f, 0x88000000); 
        drawCircle(knobX, knobY, 3.5f, 0xFFFFFFFF); 
        
        this.fontRendererObj.drawStringWithShadow(String.format("%.2fx", val), x + trackW + 10, y + 1, 0xAAAAAA);
    }

    private void drawIOSToggle(float x, float y, String label, boolean isOn, int mx, int my) {
        this.fontRendererObj.drawStringWithShadow(label, x, y + 3, 0xDDDDDD);
        float swW = 28; float swH = 14; 
        float swX = x + 245; float swY = y;
        boolean hovered = isInside(mx, my, swX, swY, swW, swH);
        
        if (isOn) {
            // REDUCED TOGGLE GLOW (Spread 2.5f, Opacity 0x55)
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
        drawRoundedOutline(x, y, w, h, h / 2, 1.0f, hovered ? 0x66FFFFFF : 0x22FFFFFF);
        this.fontRendererObj.drawStringWithShadow(text, x + (w - this.fontRendererObj.getStringWidth(text)) / 2, y + (h - 8) / 2, -1);
    }

    // --- INTERACTION LOGIC ---

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) return;
        
        int panelW = 460; int panelH = 280;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        if (isInside(mouseX, mouseY, panelX + panelW - 75, panelY + panelH - 25, 60, 16)) {
            ConfigHandler.saveConfig(); this.mc.displayGuiScreen(previousScreen); return;
        }

        int tY = panelY + 35;
        for (int i = 0; i < tabs.length; i++) {
            if (isInside(mouseX, mouseY, panelX + 10, tY, 115, 18)) { selectedTab = i; return; }
            tY += 24;
        }

        int rX = panelX + 145; 
        int rY = panelY + 45; 

        // Update Reset Hitbox for new location
        if (isInside(mouseX, mouseY, rX + 10, rY + 40, 45, 14)) { resetScaleForTab(); return; } 
        if (isInside(mouseX, mouseY, rX + 10, rY + 20, 210, 10)) { draggingSlider = true; return; } 
        rY += 70; 

        if (selectedTab == 0) { 
            if (isInside(mouseX, mouseY, rX + 260, rY + 10, 28, 14)) { PotionHUD.instance.isHorizontal = !PotionHUD.instance.isHorizontal; return; } 
            for (int i = 0; i < palette.length; i++) {
                if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 55, 12, 12)) { PotionHUD.nameColor = palette[i]; return; }
            }
        }
        else if (selectedTab == 1) { 
            if (isInside(mouseX, mouseY, rX + 260, rY + 10, 28, 14)) { ArmorHUD.instance.isHorizontal = !ArmorHUD.instance.isHorizontal; return; }
        }
        else if (selectedTab == 2) { 
            if (isInside(mouseX, mouseY, rX + 260, rY + 10, 28, 14)) { CoordsHUD.instance.isHorizontal = !CoordsHUD.instance.isHorizontal; return; }
            for (int i = 0; i < palette.length; i++) {
                if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 52, 12, 12)) { CoordsHUD.axisColor = palette[i]; return; }
            }
            for (int i = 0; i < palette.length; i++) {
                if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 84, 12, 12)) { CoordsHUD.numberColor = palette[i]; return; }
            }
            for (int i = 0; i < palette.length; i++) {
                if (isInside(mouseX, mouseY, rX + 10 + (i * 22), rY + 116, 12, 12)) { CoordsHUD.directionColor = palette[i]; return; }
            }
        }
    }

    private boolean isInside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingSlider) {
            int panelW = 460; int panelX = (this.width - panelW) / 2; int rX = panelX + 145;
            float pct = (float)(mouseX - (rX + 10)) / 210f; 
            pct = Math.max(0, Math.min(1, pct));
            assignScale(0.5f + (1.0f * pct));
        }
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
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) { draggingSlider = false; ConfigHandler.saveConfig(); }

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
        return 1.0f;
    }

    private void resetScaleForTab() { assignScale(1.0f); }
}