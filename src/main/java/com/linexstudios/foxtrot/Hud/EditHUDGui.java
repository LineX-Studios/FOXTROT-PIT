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

    // Category States
    public static boolean combatExpanded = false, renderExpanded = false, denickExpanded = false, hudExpanded = false;
    
    // Dropdown States
    public static boolean randomDropdownExpanded = false;
    public static boolean nameTagsDropdownExpanded = false; // NEW

    // Hotkey State
    public static boolean bindingAutoClicker = false;

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

        // --- COMBAT (IDs 40-59) ---
        this.buttonList.add(new ModernGUI(40, 0, 0, btnW, 16, "AutoClicker: " + (AutoClicker.enabled ? on : off)));
        
        // The Bind Button
        String bindText = bindingAutoClicker ? "[Press Any Key]" : "Bind: " + (AutoClicker.bind == Keyboard.KEY_NONE ? "NONE" : Keyboard.getKeyName(AutoClicker.bind));
        this.buttonList.add(new ModernGUI(54, 0, 0, btnW - 10, 16, bindText));
        
        this.buttonList.add(new ModernGUI(41, 0, 0, btnW - 10, 16, "Left Click: " + (AutoClicker.leftClick ? on : off)));
        this.buttonList.add(new ModernGUI(42, 0, 0, btnW - 10, 16, "Right Click: " + (AutoClicker.rightClick ? on : off)));
        this.buttonList.add(new ModernGUI(43, 0, 0, btnW - 10, 16, "Hold to Click: " + (AutoClicker.holdToClick ? on : off)));
        this.buttonList.add(new ModernGUI(44, 0, 0, btnW - 10, 16, "Inventory Fill: " + (AutoClicker.inventoryFill ? on : off)));
        this.buttonList.add(new ModernSlider(45, 0, 0, btnW - 10, 16, "Fill CPS", AutoClicker.inventoryFillCps, 5.0F, 20.0F));
        this.buttonList.add(new ModernGUI(46, 0, 0, btnW - 10, 16, "Break Blocks: " + (AutoClicker.breakBlocks ? on : off)));
        this.buttonList.add(new ModernSlider(47, 0, 0, btnW - 10, 16, "Min CPS", AutoClicker.minCps, 1.0F, 20.0F));
        this.buttonList.add(new ModernSlider(48, 0, 0, btnW - 10, 16, "Max CPS", AutoClicker.maxCps, 1.0F, 20.0F));

        // Randomization Dropdown
        String randStr = AutoClicker.randomMode == 0 ? "Normal" : (AutoClicker.randomMode == 1 ? "Extra" : "Extra+");
        this.buttonList.add(new ModernGUI(49, 0, 0, btnW - 10, 16, "Random: " + randStr + EnumChatFormatting.GRAY + (randomDropdownExpanded ? " ^" : " v")));
        this.buttonList.add(new ModernGUI(50, 0, 0, btnW - 15, 16, (AutoClicker.randomMode == 0 ? EnumChatFormatting.RED : "") + " > Normal"));
        this.buttonList.add(new ModernGUI(51, 0, 0, btnW - 15, 16, (AutoClicker.randomMode == 1 ? EnumChatFormatting.RED : "") + " > Extra"));
        this.buttonList.add(new ModernGUI(52, 0, 0, btnW - 15, 16, (AutoClicker.randomMode == 2 ? EnumChatFormatting.RED : "") + " > Extra+"));

        this.buttonList.add(new ModernGUI(53, 0, 0, btnW - 10, 16, "Limit Items: " + (AutoClicker.limitItems ? on : off)));

        if (whitelistField == null) {
            whitelistField = new ModernTextField(100, 0, 0, btnW - 10, 14);
            whitelistField.setText(String.join(", ", AutoClicker.itemWhitelist));
        }

        // --- RENDER (IDs 60-69) ---
        this.buttonList.add(new ModernGUI(60, 0, 0, btnW, 16, "NameTags: " + (NameTags.enabled ? on : off)));
        this.buttonList.add(new ModernGUI(61, 0, 0, btnW - 10, 16, "Settings: " + EnumChatFormatting.GRAY + (nameTagsDropdownExpanded ? " ^" : " v")));
        this.buttonList.add(new ModernGUI(62, 0, 0, btnW - 15, 16, " > Show Health: " + (NameTags.showHealth ? on : off)));
        this.buttonList.add(new ModernGUI(63, 0, 0, btnW - 15, 16, " > Show Armor: " + (NameTags.showItems ? on : off)));
        this.buttonList.add(new ModernGUI(64, 0, 0, btnW - 10, 16, "Chest ESP: " + (ChestESP.enabled ? on : off)));
        
        // --- DENICK (IDs 70-79) ---
        this.buttonList.add(new ModernGUI(70, 0, 0, btnW, 16, "Auto Denick: " + (AutoDenick.enabled ? on : off)));
        
        // --- HUD (IDs 80-89) ---
        this.buttonList.add(new ModernGUI(80, 0, 0, btnW, 16, "Enemy HUD: " + (EnemyHUD.enabled ? on : off)));
        this.buttonList.add(new ModernGUI(81, 0, 0, btnW, 16, "Nicked HUD: " + (NickedHUD.enabled ? on : off)));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        if (EnemyHUD.enabled) EnemyHUD.instance.render(true);
        if (NickedHUD.enabled) NickedHUD.instance.render(true);

        Gui.drawRect(panelX, panelY, panelX + 135, panelY + 18, 0xFF121212);
        this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + "Foxtrot Settings", panelX + 5, panelY + 5, -1);
        this.fontRendererObj.drawStringWithShadow(panelCollapsed ? "+" : "-", panelX + 122, panelY + 5, -1);

        if (!panelCollapsed) {
            int bgHeight = 4;
            
            // Dynamic Height Logic
            int combatCount = 12; // Base buttons
            if (randomDropdownExpanded) combatCount += 3;
            bgHeight += 12 + (combatExpanded ? (combatCount * 18) + 28 : 0) + 4;
            
            int renderCount = 3; // Base buttons (NameTags, Settings Header, ChestESP)
            if (nameTagsDropdownExpanded) renderCount += 2; // Health and Armor
            bgHeight += 12 + (renderExpanded ? (renderCount * 18) : 0) + 4;
            
            bgHeight += 12 + (denickExpanded ? (1 * 18) : 0) + 4;
            bgHeight += 12 + (hudExpanded ? (2 * 18) : 0) + 4;

            Gui.drawRect(panelX, panelY + 18, panelX + 135, panelY + 18 + bgHeight, 0xDD121212);

            int currentY = panelY + 22;
            currentY = drawCategory(currentY, "Combat", combatExpanded, 40, 59, mouseX, mouseY);
            currentY = drawCategory(currentY, "Render", renderExpanded, 60, 69, mouseX, mouseY);
            currentY = drawCategory(currentY, "Denick", denickExpanded, 70, 79, mouseX, mouseY);
            currentY = drawCategory(currentY, "HUD", hudExpanded, 80, 89, mouseX, mouseY);

            super.drawScreen(mouseX, mouseY, partialTicks);
        }
    }

    private int drawCategory(int y, String name, boolean expanded, int startId, int endId, int mouseX, int mouseY) {
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
                
                // Combat Nested Dropdown
                if (btn.id >= 50 && btn.id <= 52) {
                    if (expanded && randomDropdownExpanded) {
                        btn.visible = true;
                        btn.xPosition = panelX + 15;
                        btn.yPosition = y;
                        y += 18;
                    } else btn.visible = false;
                    continue;
                }

                // Render Nested Dropdown (NameTags)
                if (btn.id >= 62 && btn.id <= 63) {
                    if (expanded && nameTagsDropdownExpanded) {
                        btn.visible = true;
                        btn.xPosition = panelX + 15; // Indent
                        btn.yPosition = y;
                        y += 18;
                    } else btn.visible = false;
                    continue;
                }

                // Standard Buttons
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
        // --- CUSTOM HOTKEY LISTENER ---
        if (bindingAutoClicker) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                AutoClicker.bind = Keyboard.KEY_NONE;
            } else {
                AutoClicker.bind = keyCode;
            }
            bindingAutoClicker = false;
            ConfigHandler.saveConfig();
            this.initGui();
            return;
        }

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
        if (button instanceof ModernSlider) return; // Fixes Slider Bug

        // Combat
        if (button.id == 40) AutoClicker.enabled = !AutoClicker.enabled;
        if (button.id == 41) AutoClicker.leftClick = !AutoClicker.leftClick;
        if (button.id == 42) AutoClicker.rightClick = !AutoClicker.rightClick;
        if (button.id == 43) AutoClicker.holdToClick = !AutoClicker.holdToClick;
        if (button.id == 44) AutoClicker.inventoryFill = !AutoClicker.inventoryFill;
        if (button.id == 46) AutoClicker.breakBlocks = !AutoClicker.breakBlocks;
        if (button.id == 49) randomDropdownExpanded = !randomDropdownExpanded;
        if (button.id == 50) { AutoClicker.randomMode = 0; randomDropdownExpanded = false; }
        if (button.id == 51) { AutoClicker.randomMode = 1; randomDropdownExpanded = false; }
        if (button.id == 52) { AutoClicker.randomMode = 2; randomDropdownExpanded = false; }
        if (button.id == 53) AutoClicker.limitItems = !AutoClicker.limitItems;
        if (button.id == 54) { bindingAutoClicker = true; this.initGui(); return; } // Trigger Bind
        
        // Render
        if (button.id == 60) NameTags.enabled = !NameTags.enabled;
        if (button.id == 61) nameTagsDropdownExpanded = !nameTagsDropdownExpanded; // Toggle Dropdown
        if (button.id == 62) NameTags.showHealth = !NameTags.showHealth;
        if (button.id == 63) NameTags.showItems = !NameTags.showItems;
        if (button.id == 64) ChestESP.enabled = !ChestESP.enabled;
        
        // Denick & HUD
        if (button.id == 70) AutoDenick.enabled = !AutoDenick.enabled;
        if (button.id == 80) EnemyHUD.enabled = !EnemyHUD.enabled;
        if (button.id == 81) NickedHUD.enabled = !NickedHUD.enabled;
        
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
                if (slider.id == 45) AutoClicker.inventoryFillCps = slider.getValue();
                if (slider.id == 47) AutoClicker.minCps = slider.getValue();
                if (slider.id == 48) AutoClicker.maxCps = slider.getValue();
                ConfigHandler.saveConfig();
            }
        }
    }

    @Override
    public void onGuiClosed() { 
        Keyboard.enableRepeatEvents(false); 
        bindingAutoClicker = false; // Reset binding state if you close GUI mid-bind
        ConfigHandler.saveConfig(); 
    }
    
    @Override
    public boolean doesGuiPauseGame() { return false; }
}