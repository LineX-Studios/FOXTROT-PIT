package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Handler.ConfigHandler;
import com.linexstudios.foxtrot.Denick.AutoDenick;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;

import java.io.IOException;

public class EditHUDGui extends GuiScreen {
    private boolean draggingEnemy = false;
    private boolean draggingNicked = false;
    private boolean draggingPanel = false;
    
    public static int panelX = -1, panelY = -1;
    public static boolean panelCollapsed = false;
    private int lastX, lastY;

    // Categories
    public static boolean renderExpanded = false;
    public static boolean denickExpanded = false;
    public static boolean hudExpanded = false;

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear(); 
        
        // Safety check to prevent startup crashes if width isn't set yet
        if (this.width <= 0) return;

        if (panelX == -1 || panelX > this.width) { 
            panelX = this.width - 145; 
            panelY = 20; 
        }
        
        String on = EnumChatFormatting.GREEN + "ON";
        String off = EnumChatFormatting.RED + "OFF";
        int btnW = 125; 

        // RENDER SECTION
        this.buttonList.add(new ModernGUI(10, 0, 0, btnW, 16, "NameTags: " + (NameTags.enabled ? on : off)));
        this.buttonList.add(new ModernGUI(11, 0, 0, btnW - 10, 16, "Show Health: " + (NameTags.showHealth ? on : off)));
        this.buttonList.add(new ModernGUI(12, 0, 0, btnW - 10, 16, "Show Armorstatus: " + (NameTags.showItems ? on : off)));
        
        // DENICK SECTION
        this.buttonList.add(new ModernGUI(20, 0, 0, btnW, 16, "Auto Denick: " + (AutoDenick.enabled ? on : off)));
        
        // HUD SECTION
        this.buttonList.add(new ModernGUI(30, 0, 0, btnW, 16, "Enemy HUD: " + (EnemyHUD.enabled ? on : off)));
        this.buttonList.add(new ModernGUI(31, 0, 0, btnW, 16, "Nicked HUD: " + (NickedHUD.enabled ? on : off)));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        
        if (EnemyHUD.enabled) EnemyHUD.instance.render(true);
        if (NickedHUD.enabled) NickedHUD.instance.render(true);

        // Header - Red & Gray Theme
        Gui.drawRect(panelX, panelY, panelX + 135, panelY + 18, 0xFF121212);
        this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + "Foxtrot Settings", panelX + 5, panelY + 5, -1);
        this.fontRendererObj.drawStringWithShadow(panelCollapsed ? "+" : "-", panelX + 122, panelY + 5, -1);

        if (!panelCollapsed) {
            int currentY = panelY + 22;
            
            currentY = drawCategory(currentY, "Render", renderExpanded, 10, 12);
            currentY = drawCategory(currentY, "Denick", denickExpanded, 20, 20);
            currentY = drawCategory(currentY, "HUD", hudExpanded, 30, 31);

            Gui.drawRect(panelX, panelY + 18, panelX + 135, currentY, 0xDD121212);
            super.drawScreen(mouseX, mouseY, partialTicks);
        }
    }

    private int drawCategory(int y, String name, boolean expanded, int startId, int endId) {
        this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.GRAY + name, panelX + 5, y, -1);
        this.fontRendererObj.drawStringWithShadow(expanded ? "v" : ">", panelX + 120, y, -1);
        y += 12;

        for (GuiButton btn : this.buttonList) {
            if (btn.id >= startId && btn.id <= endId) {
                if (expanded) {
                    btn.visible = true;
                    btn.xPosition = panelX + 5 + (btn.width < 125 ? 5 : 0);
                    btn.yPosition = y;
                    y += 18;
                } else {
                    btn.visible = false;
                }
            }
        }
        return y + 4;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 10) NameTags.enabled = !NameTags.enabled;
        if (button.id == 11) NameTags.showHealth = !NameTags.showHealth;
        if (button.id == 12) NameTags.showItems = !NameTags.showItems;
        if (button.id == 20) AutoDenick.enabled = !AutoDenick.enabled;
        if (button.id == 30) EnemyHUD.enabled = !EnemyHUD.enabled;
        if (button.id == 31) NickedHUD.enabled = !NickedHUD.enabled;
        
        ConfigHandler.saveConfig();
        this.initGui();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseX >= panelX && mouseX <= panelX + 135 && mouseY >= panelY && mouseY <= panelY + 18) {
            if (mouseButton == 0) {
                if (mouseX >= panelX + 115) panelCollapsed = !panelCollapsed;
                else draggingPanel = true;
                lastX = mouseX; lastY = mouseY;
            }
            return;
        }

        // Expanded logic for header clicks
        if (!panelCollapsed && mouseButton == 1) {
             if (mouseY > panelY + 22 && mouseY < panelY + 34) renderExpanded = !renderExpanded;
             this.initGui();
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        if (mouseButton == 0) {
            if (EnemyHUD.enabled && EnemyHUD.instance.isHovered(mouseX, mouseY)) {
                draggingEnemy = true; lastX = mouseX; lastY = mouseY;
            } else if (NickedHUD.enabled && NickedHUD.instance.isHovered(mouseX, mouseY)) {
                draggingNicked = true; lastX = mouseX; lastY = mouseY;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (draggingPanel) { panelX += (mouseX - lastX); panelY += (mouseY - lastY); lastX = mouseX; lastY = mouseY; }
        else if (draggingEnemy) { EnemyHUD.hudX += (mouseX - lastX); EnemyHUD.hudY += (mouseY - lastY); lastX = mouseX; lastY = mouseY; }
        else if (draggingNicked) { NickedHUD.hudX += (mouseX - lastX); NickedHUD.hudY += (mouseY - lastY); lastX = mouseX; lastY = mouseY; }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        draggingEnemy = false; draggingNicked = false; draggingPanel = false;
    }

    @Override
    public void onGuiClosed() { ConfigHandler.saveConfig(); }
    @Override
    public boolean doesGuiPauseGame() { return false; }
}