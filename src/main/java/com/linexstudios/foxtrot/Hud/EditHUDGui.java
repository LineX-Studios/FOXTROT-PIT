package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Handler.ConfigHandler;
import com.linexstudios.foxtrot.Denick.AutoDenick;
import com.linexstudios.foxtrot.Combat.AutoClicker;
import com.linexstudios.foxtrot.Render.ChestESP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class EditHUDGui extends GuiScreen {
    private boolean draggingEnemy = false, draggingNicked = false, draggingPanel = false;
    public static int panelX = -1, panelY = -1;
    public static boolean panelCollapsed = false;
    private int lastX, lastY;

    public static boolean combatExpanded = false, renderExpanded = false, denickExpanded = false, hudExpanded = false;

    // These variables perfectly track where the headers are drawn so clicks never miss
    private int catCombatY = -1, catRenderY = -1, catDenickY = -1, catHudY = -1;

    private ModernTextField whitelistField;

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear(); 
        Keyboard.enableRepeatEvents(true);
        if (this.width <= 0) return;
        if (panelX == -1) { panelX = this.width - 150; panelY = 20; }
        
        String on = EnumChatFormatting.GREEN + "ON";
        String off = EnumChatFormatting.RED + "OFF";
        int btnW = 125; 

        // Combat
        this.buttonList.add(new ModernGUI(40, 0, 0, btnW, 16, "AutoClicker: " + (AutoClicker.enabled ? on : off)));
        this.buttonList.add(new ModernGUI(41, 0, 0, btnW - 10, 16, "Hold to Click: " + (AutoClicker.holdToClick ? on : off)));
        this.buttonList.add(new ModernGUI(42, 0, 0, btnW - 10, 16, "Inventory Fill: " + (AutoClicker.inventoryFill ? on : off)));
        this.buttonList.add(new ModernSlider(43, 0, 0, btnW - 10, 16, "Fill CPS", AutoClicker.inventoryFillCps, 5.0F, 20.0F));
        this.buttonList.add(new ModernGUI(44, 0, 0, btnW - 10, 16, "Break Blocks: " + (AutoClicker.breakBlocks ? on : off)));
        this.buttonList.add(new ModernSlider(45, 0, 0, btnW - 10, 16, "Min CPS", AutoClicker.minCps, 1.0F, 20.0F));
        this.buttonList.add(new ModernSlider(46, 0, 0, btnW - 10, 16, "Max CPS", AutoClicker.maxCps, 1.0F, 20.0F));
        this.buttonList.add(new ModernGUI(47, 0, 0, btnW - 10, 16, "Limit Items: " + (AutoClicker.limitItems ? on : off)));

        // Ensure the text field isn't destroyed when clicking other buttons
        if (whitelistField == null) {
            whitelistField = new ModernTextField(100, 0, 0, btnW - 10, 14);
            whitelistField.setText(String.join(", ", AutoClicker.itemWhitelist));
        }

        // Render & HUD Buttons...
        this.buttonList.add(new ModernGUI(10, 0, 0, btnW, 16, "NameTags: " + (NameTags.enabled ? on : off)));
        this.buttonList.add(new ModernGUI(11, 0, 0, btnW - 10, 16, "Show Health: " + (NameTags.showHealth ? on : off)));
        this.buttonList.add(new ModernGUI(12, 0, 0, btnW - 10, 16, "Show Armorstatus: " + (NameTags.showItems ? on : off)));
        this.buttonList.add(new ModernGUI(20, 0, 0, btnW, 16, "Auto Denick: " + (AutoDenick.enabled ? on : off)));
        this.buttonList.add(new ModernGUI(30, 0, 0, btnW, 16, "Enemy HUD: " + (EnemyHUD.enabled ? on : off)));
        this.buttonList.add(new ModernGUI(31, 0, 0, btnW, 16, "Nicked HUD: " + (NickedHUD.enabled ? on : off)));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        if (EnemyHUD.enabled) EnemyHUD.instance.render(true);
        if (NickedHUD.enabled) NickedHUD.instance.render(true);

        // Header
        Gui.drawRect(panelX, panelY, panelX + 135, panelY + 18, 0xFF121212);
        this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + "Foxtrot Settings", panelX + 5, panelY + 5, -1);
        this.fontRendererObj.drawStringWithShadow(panelCollapsed ? "+" : "-", panelX + 122, panelY + 5, -1);

        if (!panelCollapsed) {
            // 1. CALCULATE BACKGROUND HEIGHT FIRST
            int bgHeight = 4;
            bgHeight += 12 + (combatExpanded ? (countButtons(40, 47) * 18) + 28 : 0) + 4;
            bgHeight += 12 + (renderExpanded ? (countButtons(10, 12) * 18) : 0) + 4;
            bgHeight += 12 + (denickExpanded ? (countButtons(20, 20) * 18) : 0) + 4;
            bgHeight += 12 + (hudExpanded ? (countButtons(30, 31) * 18) : 0) + 4;

            // 2. DRAW BACKGROUND BEHIND TEXT
            Gui.drawRect(panelX, panelY + 18, panelX + 135, panelY + 18 + bgHeight, 0xDD121212);

            // 3. DRAW HEADERS & BUTTONS ON TOP (Fixes the dark text issue)
            int currentY = panelY + 22;
            currentY = drawCategory(currentY, "Combat", combatExpanded, 40, 47, mouseX, mouseY);
            currentY = drawCategory(currentY, "Render", renderExpanded, 10, 12, mouseX, mouseY);
            currentY = drawCategory(currentY, "Denick", denickExpanded, 20, 20, mouseX, mouseY);
            currentY = drawCategory(currentY, "HUD", hudExpanded, 30, 31, mouseX, mouseY);

            super.drawScreen(mouseX, mouseY, partialTicks);
        }
    }

    private int drawCategory(int y, String name, boolean expanded, int startId, int endId, int mouseX, int mouseY) {
        // Save exact Y coordinate so mouseClicked NEVER misses
        if (name.equals("Combat")) catCombatY = y;
        if (name.equals("Render")) catRenderY = y;
        if (name.equals("Denick")) catDenickY = y;
        if (name.equals("HUD")) catHudY = y;

        boolean isHovered = mouseX >= panelX && mouseX <= panelX + 135 && mouseY >= y && mouseY <= y + 12;
        String colorPrefix = isHovered ? EnumChatFormatting.RED.toString() : EnumChatFormatting.WHITE.toString();
        
        this.fontRendererObj.drawStringWithShadow(colorPrefix + name, panelX + 5, y, -1);
        this.fontRendererObj.drawStringWithShadow(expanded ? EnumChatFormatting.GRAY + "v" : EnumChatFormatting.GRAY + ">", panelX + 120, y, -1);
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

        if (name.equals("Combat") && expanded && whitelistField != null) {
            this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.GRAY + "Allowed Items:", panelX + 5, y, -1);
            y += 10;
            whitelistField.xPosition = panelX + 5;
            whitelistField.yPosition = y;
            whitelistField.setVisible(true);
            whitelistField.drawTextBox();
            y += 18;
        } else if (name.equals("Combat") && !expanded && whitelistField != null) {
            whitelistField.setVisible(false);
        }

        return y + 4;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (combatExpanded && whitelistField != null && whitelistField.getVisible() && whitelistField.isFocused()) {
            whitelistField.textboxKeyTyped(typedChar, keyCode);
            updateWhitelist();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void updateWhitelist() {
        AutoClicker.itemWhitelist.clear();
        String[] items = whitelistField.getText().split(",");
        for (String item : items) {
            if (!item.trim().isEmpty()) AutoClicker.itemWhitelist.add(item.trim().toLowerCase());
        }
        ConfigHandler.saveConfig();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 40) AutoClicker.enabled = !AutoClicker.enabled;
        if (button.id == 41) AutoClicker.holdToClick = !AutoClicker.holdToClick;
        if (button.id == 42) AutoClicker.inventoryFill = !AutoClicker.inventoryFill;
        if (button.id == 44) AutoClicker.breakBlocks = !AutoClicker.breakBlocks;
        if (button.id == 47) AutoClicker.limitItems = !AutoClicker.limitItems;
        
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

        if (combatExpanded && whitelistField != null && whitelistField.getVisible()) {
            whitelistField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        if (!panelCollapsed) {
            // Uses the perfectly tracked Y coordinates to guarantee clicks register
            if (catCombatY != -1 && mouseY >= catCombatY && mouseY <= catCombatY + 12 && mouseX >= panelX && mouseX <= panelX + 135) {
                combatExpanded = !combatExpanded; ConfigHandler.saveConfig(); this.initGui(); return;
            }
            if (catRenderY != -1 && mouseY >= catRenderY && mouseY <= catRenderY + 12 && mouseX >= panelX && mouseX <= panelX + 135) {
                renderExpanded = !renderExpanded; ConfigHandler.saveConfig(); this.initGui(); return;
            }
            if (catDenickY != -1 && mouseY >= catDenickY && mouseY <= catDenickY + 12 && mouseX >= panelX && mouseX <= panelX + 135) {
                denickExpanded = !denickExpanded; ConfigHandler.saveConfig(); this.initGui(); return;
            }
            if (catHudY != -1 && mouseY >= catHudY && mouseY <= catHudY + 12 && mouseX >= panelX && mouseX <= panelX + 135) {
                hudExpanded = !hudExpanded; ConfigHandler.saveConfig(); this.initGui(); return;
            }
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

    private int countButtons(int start, int end) {
        int count = 0;
        for (GuiButton btn : this.buttonList) if (btn.id >= start && btn.id <= end) count++;
        return count;
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
        
        for (GuiButton btn : this.buttonList) {
            if (btn instanceof ModernSlider) {
                ModernSlider slider = (ModernSlider) btn;
                slider.mouseReleased(mouseX, mouseY);
                if (slider.id == 43) AutoClicker.inventoryFillCps = slider.getValue();
                if (slider.id == 45) AutoClicker.minCps = slider.getValue();
                if (slider.id == 46) AutoClicker.maxCps = slider.getValue();
                ConfigHandler.saveConfig();
            }
        }
    }

    @Override
    public void onGuiClosed() { 
        Keyboard.enableRepeatEvents(false); 
        ConfigHandler.saveConfig(); 
    }
    
    @Override
    public boolean doesGuiPauseGame() { return false; }
}