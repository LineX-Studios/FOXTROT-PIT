package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Handler.ConfigHandler;
import com.linexstudios.foxtrot.Denick.AutoDenick;
import com.linexstudios.foxtrot.Combat.AutoClicker;
import com.linexstudios.foxtrot.Render.PitESP;
import com.linexstudios.foxtrot.Enemy.EnemyESP;
import com.linexstudios.foxtrot.Render.FriendsESP;
import com.linexstudios.foxtrot.Render.NickedRender;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public class EditHUDGui extends GuiScreen {
    // --- INDEPENDENT POSITIONING ---
    public static int collapsedX = -1, collapsedY = -1;
    public static boolean panelCollapsed = true;
    
    private int mainPanelX, mainPanelY;
    private final int panelW = 315; 
    private final int panelH = 220; 

    private int selectedTab = 0; 
    
    private boolean draggingPanel = false;
    private int lastX, lastY;
    private int activeSlider = 0; 
    public static boolean randomDropdownExpanded = false;

    private DraggableHUD draggingModule = null;
    private DraggableHUD resizingModule = null;
    private int resizingCorner = 0;
    private long lastClickTime = 0;
    private DraggableHUD lastClickedHUD = null;

    private String[] tabs = {"Combat", "Render", "Denick", "HUD"};
    private GuiTextField whitelistField;

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        if (this.width <= 0) return;
        
        mainPanelX = (this.width - panelW) / 2;
        mainPanelY = (this.height - panelH) / 2;

        if (collapsedX == -1) { 
            collapsedX = mainPanelX + panelW - 115; 
            collapsedY = mainPanelY - 20; 
        }

        if (whitelistField == null) {
            whitelistField = new GuiTextField(100, this.fontRendererObj, 0, 0, 95, 12);
            whitelistField.setMaxStringLength(256);
            whitelistField.setText(String.join(", ", AutoClicker.itemWhitelist));
            whitelistField.setVisible(false);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        
        GlStateManager.pushMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.disableStandardItemLighting();

        // 1. DYNAMICALLY RENDER ALL ACTIVE HUDS FROM REGISTRY
        for (DraggableHUD hud : DraggableHUD.getRegistry()) {
            if (hud.isEnabled()) {
                hud.render(true, mouseX, mouseY);
            }
        }

        GlStateManager.popMatrix();

        GlStateManager.disableLighting();
        GlStateManager.enableBlend();

        if (panelCollapsed) {
            drawGradientRoundedRect(collapsedX, collapsedY, 115, 18, 3.0f, 0xFA1E1E1E, 0xFA141414);
            
            this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.WHITE + "Foxtrot Settings", collapsedX + 8, collapsedY + 5, -1);
            this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.WHITE + "+", collapsedX + 102, collapsedY + 5, -1);
        } else {
            drawGradientRoundedRect(mainPanelX, mainPanelY, panelW, panelH, 3.0f, 0xFA1E1E1E, 0xFA141414);
            
            drawSolidRect(mainPanelX + 80, mainPanelY + 15, 1, panelH - 30, 0x1AFFFFFF); 

            this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + "Foxtrot", mainPanelX + 8, mainPanelY + 8, -1);
            this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.WHITE + "-", mainPanelX + 300, mainPanelY + 8, -1);

            // --- LEFT TABS ---
            int tY = mainPanelY + 25;
            for (int i = 0; i < tabs.length; i++) {
                boolean hovered = isInside(mouseX, mouseY, mainPanelX + 6, tY, 70, 16);
                if (selectedTab == i) {
                    drawNeonGlow(mainPanelX + 6, tY, 70, 16, 3, 10.0f, 0x1AFF1111); // Faint soft red underglow
                    drawGradientRoundedRect(mainPanelX + 6, tY, 70, 16, 3, 0xFFE53935, 0xFFC62828); 
                    this.fontRendererObj.drawStringWithShadow(tabs[i], mainPanelX + 10, tY + 4, -1);
                } else {
                    if (hovered) drawGradientRoundedRect(mainPanelX + 6, tY, 70, 16, 3, 0xFF2A2A2A, 0xFF202020);
                    this.fontRendererObj.drawStringWithShadow(tabs[i], mainPanelX + 10, tY + 4, 0xAAAAAA);
                }
                tY += 20; 
            }

            // --- RIGHT CONTENT AREA ---
            int c1 = mainPanelX + 90; 
            int c2 = mainPanelX + 205; 
            int rY = mainPanelY + 10;

            this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + tabs[selectedTab], c1, rY, -1);
            rY += 15;

            if (selectedTab == 0) { // COMBAT
                int y1 = rY; int y2 = rY;
                
                drawSettingsCard(c1, y1, 105, AutoClicker.limitItems ? 165 : 145);
                this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + "Autoclicker", c1 + 5, y1 + 5, -1); y1 += 16;
                drawIOSToggle(c1 + 5, y1, 105, "Enabled", AutoClicker.enabled, mouseX, mouseY); y1 += 18;
                drawIOSToggle(c1 + 5, y1, 105, "Left Click", AutoClicker.leftClick, mouseX, mouseY); y1 += 18;
                drawIOSToggle(c1 + 5, y1, 105, "Hold Click", AutoClicker.holdToClick, mouseX, mouseY); y1 += 18;
                drawIOSToggle(c1 + 5, y1, 105, "Fast Place", AutoClicker.fastPlaceEnabled, mouseX, mouseY); y1 += 18;
                drawIOSToggle(c1 + 5, y1, 105, "Break Blocks", AutoClicker.breakBlocks, mouseX, mouseY); y1 += 18;
                drawIOSToggle(c1 + 5, y1, 105, "Inventory Fill", AutoClicker.inventoryFill, mouseX, mouseY); y1 += 18;
                drawIOSToggle(c1 + 5, y1, 105, "Limit Items", AutoClicker.limitItems, mouseX, mouseY); y1 += 18;
                
                if (AutoClicker.limitItems) {
                    whitelistField.xPosition = c1 + 5; 
                    whitelistField.yPosition = y1 + 2;
                    whitelistField.setVisible(true); 
                    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                    whitelistField.drawTextBox();
                } else whitelistField.setVisible(false);

                drawSettingsCard(c2, y2, 100, randomDropdownExpanded ? 135 : 90);
                drawIOSSlider(c2 + 5, y2 + 5, "Min CPS", AutoClicker.minCps, 1.0f, 20.0f, 90); y2 += 20;
                drawIOSSlider(c2 + 5, y2 + 5, "Max CPS", AutoClicker.maxCps, 1.0f, 20.0f, 90); y2 += 20;
                drawIOSSlider(c2 + 5, y2 + 5, "Fill CPS", AutoClicker.inventoryFillCps, 5.0f, 20.0f, 90); y2 += 26;
                
                String activeModeStr = AutoClicker.randomMode == 0 ? "Normal" : (AutoClicker.randomMode == 1 ? "Extra" : "Extra+");
                drawIOSButton(c2 + 5, y2, 90, 14, "Mode: " + activeModeStr, mouseX, mouseY);
                y2 += 16;
                
                if (randomDropdownExpanded) {
                    drawIOSButton(c2 + 5, y2, 90, 12, (AutoClicker.randomMode == 0 ? EnumChatFormatting.RED : EnumChatFormatting.GRAY) + "Normal", mouseX, mouseY); y2 += 14;
                    drawIOSButton(c2 + 5, y2, 90, 12, (AutoClicker.randomMode == 1 ? EnumChatFormatting.RED : EnumChatFormatting.GRAY) + "Extra", mouseX, mouseY); y2 += 14;
                    drawIOSButton(c2 + 5, y2, 90, 12, (AutoClicker.randomMode == 2 ? EnumChatFormatting.RED : EnumChatFormatting.GRAY) + "Extra+", mouseX, mouseY);
                }
            } 
            else if (selectedTab == 1) { // RENDER
                if (whitelistField != null) whitelistField.setVisible(false);
                int y1 = rY; int y2 = rY;
                
                drawSettingsCard(c1, y1, 105, 70);
                this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + "NameTags", c1 + 5, y1 + 5, -1); y1 += 18;
                drawIOSToggle(c1 + 5, y1, 105, "Enabled", NameTags.enabled, mouseX, mouseY); y1 += 18;
                drawIOSToggle(c1 + 5, y1, 105, "Show Health", NameTags.showHealth, mouseX, mouseY); y1 += 18;
                drawIOSToggle(c1 + 5, y1, 105, "Show Items", NameTags.showItems, mouseX, mouseY); y1 += 22;
                
                drawSettingsCard(c1, y1, 105, 52);
                this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + "Player ESP", c1 + 5, y1 + 5, -1); y1 += 18;
                drawIOSToggle(c1 + 5, y1, 105, "Enemy ESP", EnemyESP.enabled, mouseX, mouseY); y1 += 18;
                drawIOSToggle(c1 + 5, y1, 105, "Friends ESP", FriendsESP.enabled, mouseX, mouseY);

                drawSettingsCard(c2, y2, 100, 90);
                this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + "Pit ESP", c2 + 5, y2 + 5, -1); y2 += 18;
                drawIOSToggle(c2 + 5, y2, 100, "Sewer Chests", PitESP.espChests, mouseX, mouseY); y2 += 18;
                drawIOSToggle(c2 + 5, y2, 100, "Dragon Eggs", PitESP.espDragonEggs, mouseX, mouseY); y2 += 18;
                drawIOSToggle(c2 + 5, y2, 100, "Raffle Tickets", PitESP.espRaffleTickets, mouseX, mouseY); y2 += 18;
                drawIOSToggle(c2 + 5, y2, 100, "Mystic Drops", PitESP.espMystics, mouseX, mouseY);
            }
            else if (selectedTab == 2) { // DENICK
                if (whitelistField != null) whitelistField.setVisible(false);
                drawSettingsCard(c1, rY, 105, 50);
                drawIOSToggle(c1 + 5, rY + 6, 105, "Auto Denick", AutoDenick.enabled, mouseX, mouseY); 
                drawIOSToggle(c1 + 5, rY + 24, 105, "Nicked Tags", NickedRender.enabled, mouseX, mouseY);
            }
            else if (selectedTab == 3) { // HUD
                if (whitelistField != null) whitelistField.setVisible(false);
                int y1 = rY; int y2 = rY;
                
                drawSettingsCard(c1, y1, 105, 108);
                drawIOSToggle(c1 + 5, y1 + 6, 105, "Enemy HUD", EnemyHUD.enabled, mouseX, mouseY); y1 += 18;
                drawIOSToggle(c1 + 5, y1 + 6, 105, "Nicked HUD", NickedHUD.enabled, mouseX, mouseY); y1 += 18;
                drawIOSToggle(c1 + 5, y1 + 6, 105, "Friends HUD", FriendsHUD.enabled, mouseX, mouseY); y1 += 18;
                drawIOSToggle(c1 + 5, y1 + 6, 105, "Session Stats", SessionStatsHUD.enabled, mouseX, mouseY); y1 += 18;
                drawIOSToggle(c1 + 5, y1 + 6, 105, "Event Tracker", EventHUD.enabled, mouseX, mouseY); 

                drawSettingsCard(c2, y2, 100, 90);
                drawIOSToggle(c2 + 5, y2 + 6, 100, "Reg HUD", RegHUD.enabled, mouseX, mouseY); y2 += 18;
                drawIOSToggle(c2 + 5, y2 + 6, 100, "Potion HUD", PotionHUD.enabled, mouseX, mouseY); y2 += 18;
                drawIOSToggle(c2 + 5, y2 + 6, 100, "Armor HUD", ArmorHUD.enabled, mouseX, mouseY); y2 += 18;
                drawIOSToggle(c2 + 5, y2 + 6, 100, "Coords HUD", CoordsHUD.enabled, mouseX, mouseY); y2 += 28;

                drawIOSButton(c2, y2 + 4, 100, 14, "HUD Customization", mouseX, mouseY);
            }
        }
    }

    private void drawSettingsCard(int x, int y, int w, int h) {
        drawRoundedRect(x, y, w, h, 3.0f, 0x11FFFFFF); 
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
        float a = (color >> 24 & 0xFF) / 255.0F; float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F; float b = (color & 0xFF) / 255.0F;
        GlStateManager.color(r, g, b, a);
    }

    private void drawNeonGlow(float x, float y, float width, float height, float radius, float spread, int color) {
        float a = (color >> 24 & 0xFF) / 255.0F;
        for (float s = spread; s > 0; s -= 1.0f) {
            float currentAlpha = a * (1.0f - (s / spread)); 
            int c = ((int)(currentAlpha * 255) << 24) | (color & 0x00FFFFFF);
            drawRoundedOutline(x - s, y - s, width + (s*2), height + (s*2), radius + s, 1.0f, c);
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

    private void drawSolidRect(float x, float y, float w, float h, int color) {
        drawRoundedRect(x, y, w, h, 0, color);
    }

    private void drawCircle(float cx, float cy, float radius, int color) {
        setupSmoothRender(false); setColor(color);
        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i <= 360; i += 5) GL11.glVertex2f((float)(cx + Math.cos(Math.toRadians(i)) * radius), (float)(cy + Math.sin(Math.toRadians(i)) * radius));
        GL11.glEnd(); endSmoothRender();
    }

    private void drawCircleOutline(float cx, float cy, float radius, float lineWidth, int color) {
        setupSmoothRender(false); setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= 360; i += 5) GL11.glVertex2f((float)(cx + Math.cos(Math.toRadians(i)) * radius), (float)(cy + Math.sin(Math.toRadians(i)) * radius));
        GL11.glEnd(); endSmoothRender();
    }

    private void drawIOSSlider(float x, float y, String label, float val, float min, float max, float trackW) {
        this.fontRendererObj.drawStringWithShadow(label, x, y, 0xDDDDDD);
        String valText = String.format("%.1f", val);
        int textW = this.fontRendererObj.getStringWidth(valText);
        this.fontRendererObj.drawStringWithShadow(valText, x + trackW - textW, y, 0xAAAAAA);
        
        float trackH = 2; float trackY = y + 10;
        drawRoundedRect(x, trackY, trackW, trackH, 1, 0xFF444444); 
        float pct = (val - min) / (max - min);
        float filledW = pct * trackW;
        if (filledW > trackH) drawRoundedRect(x, trackY, filledW, trackH, 1, 0xFFE53935); 
        float knobX = x + filledW; float knobY = trackY + 1;
        drawCircleOutline(knobX, knobY, 4.0f, 1.0f, 0x88000000);
        drawCircle(knobX, knobY, 3.0f, 0xFFFFFFFF); 
    }

    private void drawIOSToggle(float x, float y, float cardW, String label, boolean isOn, int mx, int my) {
        this.fontRendererObj.drawStringWithShadow(label, x, y + 1, 0xDDDDDD);
        float swW = 16; float swH = 8; 
        float swX = x + cardW - swW - 4; 
        float swY = y + 1;
        boolean hovered = isInside(mx, my, x, y, cardW, 12);
        
        if (isOn) {
            drawNeonGlow(swX, swY, swW, swH, swH / 2, 6.0f, 0x1AFF3333); 
            drawRoundedRect(swX, swY, swW, swH, swH / 2, 0xFFE53935); 
        } else {
            drawRoundedRect(swX, swY, swW, swH, swH / 2, hovered ? 0xFF555555 : 0xFF444444); 
        }
        float rad = swH / 2 - 1.0f;
        float cx = isOn ? swX + swW - rad - 1.0f : swX + rad + 1.0f;
        drawCircle(cx, swY + swH / 2, rad, 0xFFFFFFFF); 
    }

    private void drawIOSButton(float x, float y, float w, float h, String text, int mx, int my) {
        boolean hovered = isInside(mx, my, x, y, w, h);
        int topColor = hovered ? 0xFF3D3D3D : 0xFF2F2F2F;
        int botColor = hovered ? 0xFF2A2A2A : 0xFF1C1C1C;
        
        drawGradientRoundedRect(x, y, w, h, 3, topColor, botColor); 
        
        this.fontRendererObj.drawStringWithShadow(text, x + (w - this.fontRendererObj.getStringWidth(text)) / 2, y + (h - 8) / 2, -1);
    }

    private boolean isInside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!panelCollapsed && selectedTab == 0 && AutoClicker.limitItems && whitelistField != null && whitelistField.isFocused()) {
            whitelistField.textboxKeyTyped(typedChar, keyCode);
            AutoClicker.itemWhitelist.clear();
            for (String item : whitelistField.getText().split(",")) if (!item.trim().isEmpty()) AutoClicker.itemWhitelist.add(item.trim().toLowerCase());
            ConfigHandler.saveConfig(); return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) return;

        if (panelCollapsed) {
            if (isInside(mouseX, mouseY, collapsedX + 115 - 20, collapsedY, 20, 18)) {
                panelCollapsed = false;
                return;
            }
            if (isInside(mouseX, mouseY, collapsedX, collapsedY, 115 - 20, 18)) {
                draggingPanel = true; lastX = mouseX; lastY = mouseY;
                return;
            }
        } else {
            if (isInside(mouseX, mouseY, mainPanelX + panelW - 25, mainPanelY, 25, 20)) {
                panelCollapsed = true;
                return;
            }

            int tY = mainPanelY + 25;
            for (int i = 0; i < tabs.length; i++) {
                if (isInside(mouseX, mouseY, mainPanelX + 6, tY, 70, 16)) { selectedTab = i; return; }
                tY += 20;
            }

            int c1 = mainPanelX + 90; 
            int c2 = mainPanelX + 205; 
            int rY = mainPanelY + 25; 

            if (selectedTab == 0) { 
                int y1 = rY; int y2 = rY;
                
                if (AutoClicker.limitItems && whitelistField != null) whitelistField.mouseClicked(mouseX, mouseY, mouseButton);

                y1 += 16;

                if (isInside(mouseX, mouseY, c1 + 5, y1, 100, 12)) AutoClicker.enabled = !AutoClicker.enabled; y1 += 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1, 100, 12)) AutoClicker.leftClick = !AutoClicker.leftClick; y1 += 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1, 100, 12)) AutoClicker.holdToClick = !AutoClicker.holdToClick; y1 += 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1, 100, 12)) AutoClicker.fastPlaceEnabled = !AutoClicker.fastPlaceEnabled; y1 += 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1, 100, 12)) AutoClicker.breakBlocks = !AutoClicker.breakBlocks; y1 += 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1, 100, 12)) AutoClicker.inventoryFill = !AutoClicker.inventoryFill; y1 += 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1, 100, 12)) AutoClicker.limitItems = !AutoClicker.limitItems;
                
                boolean clickedDropdownContent = false;
                
                if (randomDropdownExpanded) {
                    if (isInside(mouseX, mouseY, c2 + 5, y2 + 82, 90, 12)) { AutoClicker.randomMode = 0; randomDropdownExpanded = false; clickedDropdownContent = true; }
                    else if (isInside(mouseX, mouseY, c2 + 5, y2 + 96, 90, 12)) { AutoClicker.randomMode = 1; randomDropdownExpanded = false; clickedDropdownContent = true; }
                    else if (isInside(mouseX, mouseY, c2 + 5, y2 + 110, 90, 12)) { AutoClicker.randomMode = 2; randomDropdownExpanded = false; clickedDropdownContent = true; }
                }

                if (!clickedDropdownContent) {
                    if (isInside(mouseX, mouseY, c2 + 5, y2 + 66, 90, 14)) {
                        randomDropdownExpanded = !randomDropdownExpanded;
                    } 
                    else if (!randomDropdownExpanded) {
                        if (isInside(mouseX, mouseY, c2 + 5, y2 + 13, 90, 8)) { activeSlider = 1; }
                        else if (isInside(mouseX, mouseY, c2 + 5, y2 + 33, 90, 8)) { activeSlider = 2; }
                        else if (isInside(mouseX, mouseY, c2 + 5, y2 + 53, 90, 8)) { activeSlider = 3; }
                    }
                }
            }
            else if (selectedTab == 1) { 
                int y1 = rY + 18; int y2 = rY + 18; 

                if (isInside(mouseX, mouseY, c1 + 5, y1, 100, 12)) NameTags.enabled = !NameTags.enabled; y1 += 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1, 100, 12)) NameTags.showHealth = !NameTags.showHealth; y1 += 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1, 100, 12)) NameTags.showItems = !NameTags.showItems;
                
                y1 += 36; 
                if (isInside(mouseX, mouseY, c1 + 5, y1, 100, 12)) EnemyESP.enabled = !EnemyESP.enabled; y1 += 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1, 100, 12)) FriendsESP.enabled = !FriendsESP.enabled;

                if (isInside(mouseX, mouseY, c2 + 5, y2, 100, 12)) PitESP.espChests = !PitESP.espChests; y2 += 18;
                if (isInside(mouseX, mouseY, c2 + 5, y2, 100, 12)) PitESP.espDragonEggs = !PitESP.espDragonEggs; y2 += 18;
                if (isInside(mouseX, mouseY, c2 + 5, y2, 100, 12)) PitESP.espRaffleTickets = !PitESP.espRaffleTickets; y2 += 18;
                if (isInside(mouseX, mouseY, c2 + 5, y2, 100, 12)) PitESP.espMystics = !PitESP.espMystics;
            }
            else if (selectedTab == 2) { 
                int y1 = rY;
                if (isInside(mouseX, mouseY, c1 + 5, y1 + 6, 100, 12)) AutoDenick.enabled = !AutoDenick.enabled; 
                if (isInside(mouseX, mouseY, c1 + 5, y1 + 24, 100, 12)) NickedRender.enabled = !NickedRender.enabled;
            }
            else if (selectedTab == 3) { 
                int y1 = rY; int y2 = rY;
                
                if (isInside(mouseX, mouseY, c1 + 5, y1 + 6, 100, 12)) EnemyHUD.enabled = !EnemyHUD.enabled; y1 += 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1 + 6, 100, 12)) NickedHUD.enabled = !NickedHUD.enabled; y1 += 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1 + 6, 100, 12)) FriendsHUD.enabled = !FriendsHUD.enabled; y1 += 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1 + 6, 100, 12)) SessionStatsHUD.enabled = !SessionStatsHUD.enabled; y1 += 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1 + 6, 100, 12)) EventHUD.enabled = !EventHUD.enabled;

                if (isInside(mouseX, mouseY, c2 + 5, y2 + 6, 100, 12)) RegHUD.enabled = !RegHUD.enabled; y2 += 18;
                if (isInside(mouseX, mouseY, c2 + 5, y2 + 6, 100, 12)) PotionHUD.enabled = !PotionHUD.enabled; y2 += 18;
                if (isInside(mouseX, mouseY, c2 + 5, y2 + 6, 100, 12)) ArmorHUD.enabled = !ArmorHUD.enabled; y2 += 18;
                if (isInside(mouseX, mouseY, c2 + 5, y2 + 6, 100, 12)) CoordsHUD.enabled = !CoordsHUD.enabled; y2 += 28;

                if (isInside(mouseX, mouseY, c2, y2 + 4, 100, 14)) { 
                    this.mc.displayGuiScreen(new HUDSettingsGui(this)); 
                    return; 
                }
            }
            ConfigHandler.saveConfig();
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);

        // 2. AUTOMATICALLY LOOP THROUGH ALL REGISTRY HUDS FOR DRAGGING/CLICKING
        for (DraggableHUD hud : DraggableHUD.getRegistry()) {
            if (!hud.isEnabled()) continue;
            
            if (mouseButton == 2 && hud.isHovered(mouseX, mouseY)) { 
                hud.scale = 1.0f; ConfigHandler.saveConfig(); return; 
            }
            
            if (mouseButton == 0 && hud.isHovered(mouseX, mouseY)) {
                long currentTime = System.currentTimeMillis();
                
                // Double-click automatically maps to the correct Settings tab
                if (hud == lastClickedHUD && (currentTime - lastClickTime < 300)) { 
                    this.mc.displayGuiScreen(new HUDSettingsGui(this, getTabIndexFromName(hud.name))); 
                    return; 
                }
                
                lastClickTime = currentTime; 
                lastClickedHUD = hud;
                
                int corner = hud.getHoveredCorner(mouseX, mouseY);
                if (corner != 0) { resizingModule = hud; resizingCorner = corner; lastX = mouseX; lastY = mouseY; return; }
                
                draggingModule = hud; lastX = mouseX; lastY = mouseY; return;
            }
        }
    }

    // Helper method to automatically map HUD names to the correct tab index in HUDSettingsGui
    private int getTabIndexFromName(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("potion")) return 0;
        if (lower.contains("armor")) return 1;
        if (lower.contains("coord")) return 2;
        if (lower.contains("enemy")) return 3;
        if (lower.contains("nick")) return 4;
        if (lower.contains("friend")) return 5;
        if (lower.contains("session")) return 6;
        if (lower.contains("event")) return 7;
        if (lower.contains("reg")) return 8;
        if (lower.contains("sprint")) return 9;
        return 0; // Default to first tab
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        int deltaX = mouseX - lastX; int deltaY = mouseY - lastY;

        if (draggingPanel && panelCollapsed) {
            collapsedX = Math.max(0, Math.min(this.width - 115, collapsedX + deltaX));
            collapsedY = Math.max(0, Math.min(this.height - 18, collapsedY + deltaY));
        } else if (activeSlider != 0 && !panelCollapsed) {
            float pct = (mouseX - (mainPanelX + 210)) / 90f; 
            pct = Math.max(0, Math.min(1, pct));
            if (activeSlider == 1) AutoClicker.minCps = 1.0f + (pct * 19.0f);
            if (activeSlider == 2) AutoClicker.maxCps = 1.0f + (pct * 19.0f);
            if (activeSlider == 3) AutoClicker.inventoryFillCps = 5.0f + (pct * 15.0f);
        } else if (resizingModule != null) resizingModule.handleResize(deltaX, deltaY, resizingCorner);
        else if (draggingModule != null) {
            draggingModule.x += deltaX; draggingModule.y += deltaY;
            int scaledW = (int) (draggingModule.width * draggingModule.scale);
            int scaledH = (int) (draggingModule.height * draggingModule.scale);
            if (draggingModule.x < 0) draggingModule.x = 0;
            if (draggingModule.x > this.width - scaledW) draggingModule.x = this.width - scaledW;
            if (draggingModule.y < 0) draggingModule.y = 0;
            if (draggingModule.y > this.height - scaledH) draggingModule.y = this.height - scaledH;
        }
        lastX = mouseX; lastY = mouseY;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingPanel = false; activeSlider = 0;
        draggingModule = null; resizingModule = null; resizingCorner = 0;
        ConfigHandler.saveConfig();
    }

    @Override
    public void onGuiClosed() { Keyboard.enableRepeatEvents(false); ConfigHandler.saveConfig(); }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}