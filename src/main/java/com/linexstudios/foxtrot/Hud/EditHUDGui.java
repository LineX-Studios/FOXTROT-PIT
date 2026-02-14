package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Handler.ConfigHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;

import java.io.IOException;

public class EditHUDGui extends GuiScreen {
    private boolean draggingEnemy = false;
    private boolean draggingNicked = false;
    
    // Panel dragging variables
    private boolean draggingPanel = false;
    public static int panelX = -1; 
    public static int panelY = -1;
    public static boolean panelCollapsed = false;
    
    // Tracks if the NameTags dropdown is open
    public static boolean nameTagsExpanded = false; 
    
    private int lastX, lastY;

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear(); 
        
        if (panelX == -1 && panelY == -1) {
            panelX = this.width - 165;
            panelY = this.height - 120;
        }
        
        String on = EnumChatFormatting.GREEN + "ON";
        String off = EnumChatFormatting.RED + "OFF";
        
        // 1. The Main NameTags Button
        this.buttonList.add(new ModernGUI(1, panelX + 5, panelY + 22, 150, 18, "NameTags ESP: " + (NameTags.enabled ? on : off)));
        
        // 2. The Sub-Settings (Indented width of 140)
        this.buttonList.add(new ModernGUI(2, panelX + 10, panelY + 44, 140, 18, "Show Health: " + (NameTags.showHealth ? on : off)));
        this.buttonList.add(new ModernGUI(3, panelX + 10, panelY + 66, 140, 18, "Armor & Sword: " + (NameTags.showItems ? on : off)));
        this.buttonList.add(new ModernGUI(4, panelX + 10, panelY + 88, 140, 18, "Items Thru Walls: " + (NameTags.itemsThroughWalls ? on : off)));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground(); 
        
        String title = EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "Foxtrot HUD Editor";
        this.fontRendererObj.drawStringWithShadow(title, this.width / 2 - 55, 20, 0xFFFFFF);
        String subtitle = EnumChatFormatting.GRAY + "Click and drag the HUD Elements. Press ESC to save.";
        this.fontRendererObj.drawStringWithShadow(subtitle, this.width / 2 - 110, 35, 0xFFFFFF);

        if (EnemyHUD.enabled) EnemyHUD.instance.render(true);
        if (NickedHUD.enabled) NickedHUD.instance.render(true);

        // --- 1. Draw Panel Header ---
        Gui.drawRect(panelX, panelY, panelX + 160, panelY + 18, 0xFF121212);
        
        // Panel Title set to RED to match the theme
        this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + "Render Settings", panelX + 5, panelY + 5, 0xFFFFFF);
        this.fontRendererObj.drawStringWithShadow(panelCollapsed ? EnumChatFormatting.GRAY + "+" : EnumChatFormatting.GRAY + "-", panelX + 145, panelY + 5, 0xFFFFFF);

        // --- 2. Draw Panel Body & Buttons ---
        if (!panelCollapsed) {
            int visibleCount = nameTagsExpanded ? 4 : 1;
            int bodyHeight = (visibleCount * 22) + 4;
            
            Gui.drawRect(panelX, panelY + 18, panelX + 160, panelY + 18 + bodyHeight, 0xDD121212);
            
            int btnY = panelY + 22;
            
            for (GuiButton btn : this.buttonList) {
                if (btn.id == 1) {
                    btn.xPosition = panelX + 5;
                    btn.yPosition = btnY;
                    btn.visible = true;
                    btnY += 22;
                } else if (btn.id >= 2 && btn.id <= 4) {
                    if (nameTagsExpanded) {
                        btn.xPosition = panelX + 10; 
                        btn.yPosition = btnY;
                        btn.visible = true;
                        btnY += 22;
                    } else {
                        btn.visible = false;
                    }
                }
            }
            
            super.drawScreen(mouseX, mouseY, partialTicks);
            
            // Draw the "..." dots on top of the NameTags button
            for (GuiButton btn : this.buttonList) {
                if (btn.id == 1 && btn.visible) {
                    String icon = nameTagsExpanded ? "v" : "...";
                    this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.GRAY + icon, panelX + 140, btn.yPosition + (nameTagsExpanded ? 5 : 3), 0xFFFFFF);
                }
            }
        } else {
            for (GuiButton btn : this.buttonList) btn.visible = false;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) NameTags.enabled = !NameTags.enabled;
        if (button.id == 2) NameTags.showHealth = !NameTags.showHealth;
        if (button.id == 3) NameTags.showItems = !NameTags.showItems;
        if (button.id == 4) NameTags.itemsThroughWalls = !NameTags.itemsThroughWalls;
        
        ConfigHandler.saveConfig();
        this.initGui(); 
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        
        // 1. Intercept Header Clicks
        if (mouseX >= panelX && mouseX <= panelX + 160 && mouseY >= panelY && mouseY <= panelY + 18) {
            if (mouseX >= panelX + 140 && mouseButton == 0) {
                panelCollapsed = !panelCollapsed;
            } else if (mouseButton == 0) {
                draggingPanel = true;
                lastX = mouseX; lastY = mouseY;
            }
            return; 
        }

        // 2. Intercept Dropdown Clicks
        if (!panelCollapsed) {
            for (GuiButton btn : this.buttonList) {
                if (btn.id == 1 && btn.visible) {
                    boolean hoveringDots = mouseX >= panelX + 130 && mouseX <= panelX + 155 && mouseY >= btn.yPosition && mouseY <= btn.yPosition + 18;
                    boolean hoveringBtn = mouseX >= btn.xPosition && mouseX <= btn.xPosition + btn.width && mouseY >= btn.yPosition && mouseY <= btn.yPosition + 18;
                    
                    if ((hoveringDots && mouseButton == 0) || (hoveringBtn && mouseButton == 1)) {
                        nameTagsExpanded = !nameTagsExpanded;
                        return; 
                    }
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton); 

        // 3. Standard HUD Dragging Logic
        if (mouseButton == 0) {
            if (EnemyHUD.enabled && EnemyHUD.instance.isHovered(mouseX, mouseY)) {
                draggingEnemy = true;
                lastX = mouseX; lastY = mouseY;
            } else if (NickedHUD.enabled && NickedHUD.instance.isHovered(mouseX, mouseY)) {
                draggingNicked = true;
                lastX = mouseX; lastY = mouseY;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (draggingPanel) {
            panelX += (mouseX - lastX);
            panelY += (mouseY - lastY);
            lastX = mouseX; lastY = mouseY;
        } else if (draggingEnemy) {
            EnemyHUD.hudX += (mouseX - lastX);
            EnemyHUD.hudY += (mouseY - lastY);
            lastX = mouseX; lastY = mouseY;
        } else if (draggingNicked) {
            NickedHUD.hudX += (mouseX - lastX);
            NickedHUD.hudY += (mouseY - lastY);
            lastX = mouseX; lastY = mouseY;
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        draggingEnemy = false;
        draggingNicked = false;
        draggingPanel = false;
    }

    @Override
    public void onGuiClosed() {
        ConfigHandler.saveConfig();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}