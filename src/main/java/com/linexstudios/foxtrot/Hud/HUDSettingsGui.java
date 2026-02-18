package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Handler.ConfigHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class HUDSettingsGui extends GuiScreen {
    private final GuiScreen previousScreen;
    private String[] tabs = {"Potion Status", "Armor Status", "Coordinates", "Enemy List", "Nicked List", "Friend List", "Session Stats", "Event Tracker", "Regularity List"};
    private int selectedTab = 0;
    private int[] colorPresets = {0xFFFFFF, 0xAAAAAA, 0x555555, 0xFF5555, 0x55FF55, 0x5555FF, 0xFFFF55, 0x55FFFF, 0xFFAA00, 0xFF55FF};
    private boolean draggingSlider = false;

    public HUDSettingsGui(GuiScreen previousScreen) { this.previousScreen = previousScreen; }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        // Render Previews
        if (PotionHUD.enabled) PotionHUD.instance.render(false, mouseX, mouseY);
        if (ArmorHUD.enabled) ArmorHUD.instance.render(false, mouseX, mouseY);
        if (CoordsHUD.enabled) CoordsHUD.instance.render(false, mouseX, mouseY);

        int panelW = 450;
        int panelH = 280;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        // Modern Seamless Translucent Dark Background (Like ModuleSettingsElement)
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC1A1A1A); 
        
        // Sidebar (Slightly lighter)
        Gui.drawRect(panelX, panelY, panelX + 130, panelY + panelH, 0xCC232323);
        Gui.drawRect(panelX + 129, panelY, panelX + 130, panelY + panelH, 0x22FFFFFF); // Separator

        this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.WHITE + "HUD Options", panelX + 15, panelY + 15, 0xFFFFFF);

        int tabY = panelY + 40;
        for (int i = 0; i < tabs.length; i++) {
            boolean hovered = mouseX >= panelX && mouseX <= panelX + 130 && mouseY >= tabY && mouseY < tabY + 20;
            if (selectedTab == i) {
                Gui.drawRect(panelX, tabY, panelX + 130, tabY + 20, 0x88FF0000); // Red Selection
            } else if (hovered) {
                Gui.drawRect(panelX, tabY, panelX + 130, tabY + 20, 0x22FFFFFF);
            }
            this.fontRendererObj.drawStringWithShadow(tabs[i], panelX + 15, tabY + 6, selectedTab == i ? 0xFFFFFF : 0xAAAAAA);
            tabY += 20;
        }

        int rightX = panelX + 145;
        int rightY = panelY + 15;
        this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + tabs[selectedTab], rightX, rightY, 0xFFFFFF);
        rightY += 30;

        // Slider (Based on SliderElement logic)
        float currentScale = getScaleForTab();
        drawCustomSlider(rightX, rightY, "Scale", currentScale, 0.5f, 1.5f);
        drawCustomButton(rightX + 190, rightY - 3, 60, 16, "Reset", mouseX, mouseY);
        rightY += 30;

        if (selectedTab == 0) { // Potion
            drawCustomToggleButton(rightX, rightY, "Layout", PotionHUD.instance.isHorizontal ? "Horizontal" : "Vertical", mouseX, mouseY);
            rightY += 30;
            this.fontRendererObj.drawStringWithShadow("Potion Name Color", rightX, rightY, 0xFFFFFF);
            rightY += 14;
            drawColorPaletteGrid(rightX, rightY, PotionHUD.nameColor, mouseX, mouseY);
            rightY += 30;
            this.fontRendererObj.drawStringWithShadow("Duration Color", rightX, rightY, 0xFFFFFF);
            rightY += 14;
            drawColorPaletteGrid(rightX, rightY, PotionHUD.durationColor, mouseX, mouseY);
        } else if (selectedTab == 1) { // Armor
            drawCustomToggleButton(rightX, rightY, "Layout", ArmorHUD.instance.isHorizontal ? "Horizontal" : "Vertical", mouseX, mouseY);
        } else if (selectedTab == 2) { // Coords
            drawCustomToggleButton(rightX, rightY, "Layout", CoordsHUD.instance.isHorizontal ? "Horizontal" : "Vertical", mouseX, mouseY);
            rightY += 30;
            this.fontRendererObj.drawStringWithShadow("Axis Letter Color", rightX, rightY, 0xFFFFFF);
            rightY += 14;
            drawColorPaletteGrid(rightX, rightY, CoordsHUD.axisColor, mouseX, mouseY);
            rightY += 30;
            this.fontRendererObj.drawStringWithShadow("Number Color", rightX, rightY, 0xFFFFFF);
            rightY += 14;
            drawColorPaletteGrid(rightX, rightY, CoordsHUD.numberColor, mouseX, mouseY);
        }

        drawCustomButton(panelX + panelW - 75, panelY + panelH - 25, 60, 16, "Return", mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawCustomSlider(int x, int y, String label, float value, float min, float max) {
        this.fontRendererObj.drawStringWithShadow(label, x, y + 2, 0xFFFFFF);
        int sliderX = x + 45;
        int sliderW = 110;
        // Background track (Dark)
        Gui.drawRect(sliderX, y + 5, sliderX + sliderW, y + 7, 0xFF333333);
        float percent = (value - min) / (max - min);
        // Filled track (Red)
        Gui.drawRect(sliderX, y + 5, sliderX + (int)(percent * sliderW), y + 7, 0xFFFF0000);
        // Knob
        int knobX = sliderX + (int)(percent * sliderW);
        Gui.drawRect(knobX - 2, y + 1, knobX + 2, y + 11, 0xFFFFFFFF);
        this.fontRendererObj.drawStringWithShadow(String.format("%.2fx", value), sliderX + sliderW + 10, y + 2, 0xAAAAAA);
    }

    private void drawCustomToggleButton(int x, int y, String label, String value, int mouseX, int mouseY) {
        this.fontRendererObj.drawStringWithShadow(label, x, y + 3, 0xFFFFFF);
        int btnX = x + 45;
        int btnW = 80;
        boolean hovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= y && mouseY <= y + 14;
        Gui.drawRect(btnX, y, btnX + btnW, y + 14, hovered ? 0xFF3D3D3D : 0xFF2A2A2A);
        Gui.drawRect(btnX, y, btnX + btnW, y + 1, 0xFF4A4A4A); // Top highlight
        int textW = this.fontRendererObj.getStringWidth(value);
        this.fontRendererObj.drawStringWithShadow(value, btnX + (btnW - textW) / 2, y + 3, 0xFFFFFF);
    }

    private void drawCustomButton(int x, int y, int w, int h, String text, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        Gui.drawRect(x, y, x + w, y + h, hovered ? 0xFF3D3D3D : 0xFF2A2A2A);
        Gui.drawRect(x, y, x + w, y + 1, 0xFF4A4A4A);
        int textW = this.fontRendererObj.getStringWidth(text);
        this.fontRendererObj.drawStringWithShadow(text, x + (w - textW) / 2, y + (h - 8) / 2, 0xFFFFFF);
    }

    private void drawColorPaletteGrid(int x, int y, int selectedColor, int mouseX, int mouseY) {
        for (int i = 0; i < colorPresets.length; i++) {
            int boxX = x + (i * 22); 
            int color = colorPresets[i];
            boolean hovered = mouseX >= boxX && mouseX <= boxX + 18 && mouseY >= y && mouseY <= y + 18;
            if (color == selectedColor) Gui.drawRect(boxX - 2, y - 2, boxX + 20, y + 20, 0xFFFFFFFF); 
            else if (hovered) Gui.drawRect(boxX - 1, y - 1, boxX + 19, y + 19, 0xFFAAAAAA); 
            Gui.drawRect(boxX, y, boxX + 18, y + 18, color | 0xFF000000); 
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) return;
        int panelW = 450; int panelH = 280; int panelX = (this.width - panelW) / 2; int panelY = (this.height - panelH) / 2;

        if (mouseX >= panelX + panelW - 75 && mouseX <= panelX + panelW - 15 && mouseY >= panelY + panelH - 25 && mouseY <= panelY + panelH - 9) {
            ConfigHandler.saveConfig(); this.mc.displayGuiScreen(previousScreen); return;
        }

        int tabY = panelY + 40;
        for (int i = 0; i < tabs.length; i++) {
            if (mouseX >= panelX && mouseX <= panelX + 130 && mouseY >= tabY && mouseY < tabY + 20) { selectedTab = i; return; }
            tabY += 20;
        }

        int rightX = panelX + 145; int rightY = panelY + 45;
        if (mouseX >= rightX + 190 && mouseX <= rightX + 250 && mouseY >= rightY - 3 && mouseY <= rightY + 13) { resetScaleForTab(); return; }
        if (mouseY >= rightY - 2 && mouseY <= rightY + 12 && mouseX >= rightX + 45 && mouseX <= rightX + 155) { draggingSlider = true; return; }
        rightY += 30;

        if (selectedTab == 0) { 
            if (mouseX >= rightX + 45 && mouseX <= rightX + 125 && mouseY >= rightY && mouseY <= rightY + 14) { PotionHUD.instance.isHorizontal = !PotionHUD.instance.isHorizontal; return; }
            rightY += 44;
            for (int i = 0; i < colorPresets.length; i++) {
                if (mouseX >= rightX + (i * 22) && mouseX <= rightX + (i * 22) + 18 && mouseY >= rightY && mouseY <= rightY + 18) { PotionHUD.nameColor = colorPresets[i]; return; }
            }
            rightY += 44;
            for (int i = 0; i < colorPresets.length; i++) {
                if (mouseX >= rightX + (i * 22) && mouseX <= rightX + (i * 22) + 18 && mouseY >= rightY && mouseY <= rightY + 18) { PotionHUD.durationColor = colorPresets[i]; return; }
            }
        } else if (selectedTab == 1) { 
            if (mouseX >= rightX + 45 && mouseX <= rightX + 125 && mouseY >= rightY && mouseY <= rightY + 14) { ArmorHUD.instance.isHorizontal = !ArmorHUD.instance.isHorizontal; return; }
        } else if (selectedTab == 2) { 
            if (mouseX >= rightX + 45 && mouseX <= rightX + 125 && mouseY >= rightY && mouseY <= rightY + 14) { CoordsHUD.instance.isHorizontal = !CoordsHUD.instance.isHorizontal; return; }
            rightY += 44;
            for (int i = 0; i < colorPresets.length; i++) {
                if (mouseX >= rightX + (i * 22) && mouseX <= rightX + (i * 22) + 18 && mouseY >= rightY && mouseY <= rightY + 18) { CoordsHUD.axisColor = colorPresets[i]; return; }
            }
            rightY += 44;
            for (int i = 0; i < colorPresets.length; i++) {
                if (mouseX >= rightX + (i * 22) && mouseX <= rightX + (i * 22) + 18 && mouseY >= rightY && mouseY <= rightY + 18) { CoordsHUD.numberColor = colorPresets[i]; return; }
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingSlider) {
            int panelW = 450; int panelX = (this.width - panelW) / 2; int rightX = panelX + 145;
            float percent = (float)(mouseX - (rightX + 45)) / 110f;
            percent = Math.max(0, Math.min(1, percent));
            float newVal = 0.5f + (1.0f * percent);
            if (selectedTab == 0) PotionHUD.instance.scale = newVal;
            else if (selectedTab == 1) ArmorHUD.instance.scale = newVal;
            else if (selectedTab == 2) CoordsHUD.instance.scale = newVal;
            else if (selectedTab == 3) EnemyHUD.instance.scale = newVal;
            else if (selectedTab == 4) NickedHUD.instance.scale = newVal;
            else if (selectedTab == 5) FriendsHUD.instance.scale = newVal;
            else if (selectedTab == 6) SessionStatsHUD.instance.scale = newVal;
            else if (selectedTab == 7) EventHUD.instance.scale = newVal;
            else if (selectedTab == 8) RegHUD.instance.scale = newVal;
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) { draggingSlider = false; ConfigHandler.saveConfig(); }
    @Override
    public void onGuiClosed() { Keyboard.enableRepeatEvents(false); ConfigHandler.saveConfig(); }
    @Override
    public boolean doesGuiPauseGame() { return false; }

    private float getScaleForTab() {
        if (selectedTab == 0) return PotionHUD.instance.scale;
        if (selectedTab == 1) return ArmorHUD.instance.scale;
        if (selectedTab == 2) return CoordsHUD.instance.scale;
        return 1.0f;
    }

    private void resetScaleForTab() {
        if (selectedTab == 0) PotionHUD.instance.scale = 1.0f;
        else if (selectedTab == 1) ArmorHUD.instance.scale = 1.0f;
        else if (selectedTab == 2) CoordsHUD.instance.scale = 1.0f;
    }
}