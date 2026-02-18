package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Handler.ConfigHandler;
import com.linexstudios.foxtrot.Denick.AutoDenick;
import com.linexstudios.foxtrot.Combat.AutoClicker;
import com.linexstudios.foxtrot.Render.PitESP;
import com.linexstudios.foxtrot.Enemy.EnemyESP;
import com.linexstudios.foxtrot.Render.FriendsESP;
import com.linexstudios.foxtrot.Render.NickedRender;
import com.linexstudios.foxtrot.Foxtrot;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class EditHUDGui extends GuiScreen {
    private boolean draggingPanel = false;
    
    // NEW UNIFIED DRAGGING SYSTEM
    private DraggableHUD draggingModule = null;
    private DraggableHUD resizingModule = null;
    private int resizingCorner = 0;

    public static int panelX = -1, panelY = -1;
    public static boolean panelCollapsed = false;
    private int lastX, lastY;

    public static boolean combatExpanded = false, renderExpanded = false, denickExpanded = false, hudExpanded = false;
    public static boolean autoClickerDropdownExpanded = false;
    public static boolean randomDropdownExpanded = false;
    public static boolean nameTagsDropdownExpanded = false;
    public static boolean pitEspDropdownExpanded = false;

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

        // --- COMBAT ---
        this.buttonList.add(new ModernGUI(40, 0, 0, btnW, 16, "AutoClicker" + EnumChatFormatting.GRAY + (autoClickerDropdownExpanded ? " ^" : " v")));
        this.buttonList.add(new ModernGUI(41, 0, 0, btnW - 10, 16, " > Enabled: " + (AutoClicker.enabled ? on : off)));
        String bindName = (Foxtrot.toggleCombatKey != null) ? Keyboard.getKeyName(Foxtrot.toggleCombatKey.getKeyCode()) : "NONE";
        this.buttonList.add(new ModernGUI(42, 0, 0, btnW - 10, 16, " > Bind: " + bindName));
        this.buttonList.add(new ModernGUI(43, 0, 0, btnW - 10, 16, " > Left Click: " + (AutoClicker.leftClick ? on : off)));
        this.buttonList.add(new ModernGUI(44, 0, 0, btnW - 10, 16, " > Fast Place: " + (AutoClicker.fastPlaceEnabled ? on : off)));
        this.buttonList.add(new ModernGUI(45, 0, 0, btnW - 10, 16, " > Hold to Click: " + (AutoClicker.holdToClick ? on : off)));
        this.buttonList.add(new ModernGUI(46, 0, 0, btnW - 10, 16, " > Inventory Fill: " + (AutoClicker.inventoryFill ? on : off)));
        this.buttonList.add(new ModernSlider(47, 0, 0, btnW - 10, 16, " > Fill CPS", AutoClicker.inventoryFillCps, 5.0F, 20.0F));
        this.buttonList.add(new ModernGUI(48, 0, 0, btnW - 10, 16, " > Break Blocks: " + (AutoClicker.breakBlocks ? on : off)));
        this.buttonList.add(new ModernSlider(49, 0, 0, btnW - 10, 16, " > Min CPS", AutoClicker.minCps, 1.0F, 20.0F));
        this.buttonList.add(new ModernSlider(50, 0, 0, btnW - 10, 16, " > Max CPS", AutoClicker.maxCps, 1.0F, 20.0F));

        String randStr = AutoClicker.randomMode == 0 ? "Normal" : (AutoClicker.randomMode == 1 ? "Extra" : "Extra+");
        this.buttonList.add(new ModernGUI(51, 0, 0, btnW - 10, 16, " > Random: " + randStr + EnumChatFormatting.GRAY + (randomDropdownExpanded ? " ^" : " v")));
        this.buttonList.add(new ModernGUI(52, 0, 0, btnW - 15, 16, (AutoClicker.randomMode == 0 ? EnumChatFormatting.RED : "") + " >> Normal"));
        this.buttonList.add(new ModernGUI(53, 0, 0, btnW - 15, 16, (AutoClicker.randomMode == 1 ? EnumChatFormatting.RED : "") + " >> Extra"));
        this.buttonList.add(new ModernGUI(54, 0, 0, btnW - 15, 16, (AutoClicker.randomMode == 2 ? EnumChatFormatting.RED : "") + " >> Extra+"));
        this.buttonList.add(new ModernGUI(55, 0, 0, btnW - 10, 16, " > Limit Items: " + (AutoClicker.limitItems ? on : off)));

        if (whitelistField == null) {
            whitelistField = new ModernTextField(100, 0, 0, btnW - 10, 14);
            whitelistField.setText(String.join(", ", AutoClicker.itemWhitelist));
        }

        // --- RENDER ---
        this.buttonList.add(new ModernGUI(60, 0, 0, btnW, 16, "NameTags" + EnumChatFormatting.GRAY + (nameTagsDropdownExpanded ? " ^" : " v")));
        this.buttonList.add(new ModernGUI(61, 0, 0, btnW - 10, 16, " > Enabled: " + (NameTags.enabled ? on : off)));
        this.buttonList.add(new ModernGUI(62, 0, 0, btnW - 10, 16, " > Show Health: " + (NameTags.showHealth ? on : off)));
        this.buttonList.add(new ModernGUI(63, 0, 0, btnW - 10, 16, " > Show Armorstatus: " + (NameTags.showItems ? on : off)));
        
        this.buttonList.add(new ModernGUI(64, 0, 0, btnW, 16, "PIT ESP" + EnumChatFormatting.GRAY + (pitEspDropdownExpanded ? " ^" : " v")));
        this.buttonList.add(new ModernGUI(65, 0, 0, btnW - 10, 16, " > Sewer Chests: " + (PitESP.espChests ? on : off)));
        this.buttonList.add(new ModernGUI(66, 0, 0, btnW - 10, 16, " > Dragon Eggs: " + (PitESP.espDragonEggs ? on : off)));
        this.buttonList.add(new ModernGUI(67, 0, 0, btnW - 10, 16, " > Raffle Tickets: " + (PitESP.espRaffleTickets ? on : off)));
        this.buttonList.add(new ModernGUI(68, 0, 0, btnW - 10, 16, " > Mystic Drops: " + (PitESP.espMystics ? on : off)));

        this.buttonList.add(new ModernGUI(69, 0, 0, btnW, 16, "Enemy ESP: " + (EnemyESP.enabled ? on : off)));
        this.buttonList.add(new ModernGUI(70, 0, 0, btnW, 16, "Friends ESP: " + (FriendsESP.enabled ? on : off)));

        // --- DENICK & HUD ---
        this.buttonList.add(new ModernGUI(75, 0, 0, btnW, 16, "Auto Denick: " + (AutoDenick.enabled ? on : off)));
        this.buttonList.add(new ModernGUI(76, 0, 0, btnW, 16, "Nicked Tags: " + (NickedRender.enabled ? on : off)));

        this.buttonList.add(new ModernGUI(80, 0, 0, btnW, 16, "Enemy HUD: " + (EnemyHUD.enabled ? on : off)));
        this.buttonList.add(new ModernGUI(81, 0, 0, btnW, 16, "Nicked HUD: " + (NickedHUD.enabled ? on : off)));
        this.buttonList.add(new ModernGUI(82, 0, 0, btnW, 16, "Friends HUD: " + (FriendsHUD.enabled ? on : off)));
        this.buttonList.add(new ModernGUI(85, 0, 0, btnW, 16, "Session Stats: " + (SessionStatsHUD.enabled ? on : off)));
        this.buttonList.add(new ModernGUI(86, 0, 0, btnW, 16, "Event Tracker: " + (EventHUD.enabled ? on : off)));
        this.buttonList.add(new ModernGUI(87, 0, 0, btnW, 16, "Reg HUD: " + (RegHUD.enabled ? on : off)));
        
        // --- NEW HUD TOGGLES ---
        this.buttonList.add(new ModernGUI(88, 0, 0, btnW, 16, "Potion HUD: " + (PotionHUD.enabled ? on : off)));
        this.buttonList.add(new ModernGUI(89, 0, 0, btnW, 16, "Armor HUD: " + (ArmorHUD.enabled ? on : off)));
        this.buttonList.add(new ModernGUI(90, 0, 0, btnW, 16, "Coords HUD: " + (CoordsHUD.enabled ? on : off)));
        
        // --- MASTER CUSTOMIZE BUTTON ---
        this.buttonList.add(new ModernGUI(500, 0, 0, btnW, 16, EnumChatFormatting.YELLOW + "Customize HUDs"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        
        // --- UNIFIED HUD RENDERING (CHEATBREAKER VISUALS ENABLED) ---
        if (EnemyHUD.enabled) EnemyHUD.instance.render(true, mouseX, mouseY);
        if (NickedHUD.enabled) NickedHUD.instance.render(true, mouseX, mouseY);
        if (FriendsHUD.enabled) FriendsHUD.instance.render(true, mouseX, mouseY);
        if (SessionStatsHUD.enabled) SessionStatsHUD.instance.render(true, mouseX, mouseY);
        if (EventHUD.enabled) EventHUD.instance.render(true, mouseX, mouseY);
        if (RegHUD.enabled) RegHUD.instance.render(true, mouseX, mouseY); 
        if (PotionHUD.enabled) PotionHUD.instance.render(true, mouseX, mouseY);
        if (ArmorHUD.enabled) ArmorHUD.instance.render(true, mouseX, mouseY);
        if (CoordsHUD.enabled) CoordsHUD.instance.render(true, mouseX, mouseY);

        // --- RENDER SIDEBAR PANEL ---
        Gui.drawRect(panelX, panelY, panelX + 135, panelY + 18, 0xFF121212);
        this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + "Foxtrot Settings", panelX + 5, panelY + 5, -1);
        this.fontRendererObj.drawStringWithShadow(panelCollapsed ? "+" : "-", panelX + 122, panelY + 5, -1);

        if (!panelCollapsed) {
            int bgHeight = 4;

            int combatCount = 1;
            if (autoClickerDropdownExpanded) {
                combatCount += 12;
                if (randomDropdownExpanded) combatCount += 3;
            }
            bgHeight += 12 + (combatExpanded ? (combatCount * 18) + (autoClickerDropdownExpanded ? 28 : 0) : 0) + 4;

            int renderCount = 4;
            if (nameTagsDropdownExpanded) renderCount += 3;
            if (pitEspDropdownExpanded) renderCount += 4;
            bgHeight += 12 + (renderExpanded ? (renderCount * 18) : 0) + 4;

            bgHeight += 12 + (denickExpanded ? (2 * 18) : 0) + 4;

            bgHeight += 12 + (hudExpanded ? (10 * 18) : 0) + 4;

            Gui.drawRect(panelX, panelY + 18, panelX + 135, panelY + 18 + bgHeight, 0xDD121212);

            int currentY = panelY + 22;
            currentY = drawCategory(currentY, "Combat", combatExpanded, 40, 59, mouseX, mouseY);
            currentY = drawCategory(currentY, "Render", renderExpanded, 60, 70, mouseX, mouseY);
            currentY = drawCategory(currentY, "Denick", denickExpanded, 75, 79, mouseX, mouseY);
            drawCategory(currentY, "HUD", hudExpanded, 80, 500, mouseX, mouseY);

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

                if (btn.id >= 41 && btn.id <= 55) {
                    if (expanded && autoClickerDropdownExpanded) {
                        if (btn.id >= 52 && btn.id <= 54) {
                            if (randomDropdownExpanded) {
                                btn.visible = true; btn.xPosition = panelX + 15; btn.yPosition = y; y += 18;
                            } else btn.visible = false;
                            continue;
                        }
                        btn.visible = true; btn.xPosition = panelX + 5; btn.yPosition = y; y += 18;
                    } else btn.visible = false;
                    continue;
                }

                if (btn.id >= 61 && btn.id <= 63) {
                    if (expanded && nameTagsDropdownExpanded) {
                        btn.visible = true; btn.xPosition = panelX + 5; btn.yPosition = y; y += 18;
                    } else btn.visible = false;
                    continue;
                }

                if (btn.id >= 65 && btn.id <= 68) {
                    if (expanded && pitEspDropdownExpanded) {
                        btn.visible = true; btn.xPosition = panelX + 5; btn.yPosition = y; y += 18;
                    } else btn.visible = false;
                    continue;
                }

                if (expanded) {
                    btn.visible = true; btn.xPosition = panelX + 5; btn.yPosition = y; y += 18;
                } else btn.visible = false;
            }
        }

        if (name.equals("Combat") && expanded && autoClickerDropdownExpanded && whitelistField != null) {
            this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.GRAY + "  Allowed Items:", panelX + 5, y, -1);
            y += 10;
            whitelistField.xPosition = panelX + 5;
            whitelistField.yPosition = y;
            whitelistField.setVisible(true);
            whitelistField.drawTextBox();
            y += 18;
        } else if (name.equals("Combat") && whitelistField != null) {
            whitelistField.setVisible(false);
        }

        return y + 4;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (combatExpanded && autoClickerDropdownExpanded && whitelistField != null && whitelistField.getVisible() && whitelistField.isFocused()) {
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
        if (button instanceof ModernSlider) return;

        if (button.id == 40) autoClickerDropdownExpanded = !autoClickerDropdownExpanded;
        if (button.id == 41) AutoClicker.enabled = !AutoClicker.enabled;
        if (button.id == 43) AutoClicker.leftClick = !AutoClicker.leftClick;
        if (button.id == 44) AutoClicker.fastPlaceEnabled = !AutoClicker.fastPlaceEnabled;
        if (button.id == 45) AutoClicker.holdToClick = !AutoClicker.holdToClick;
        if (button.id == 46) AutoClicker.inventoryFill = !AutoClicker.inventoryFill;
        if (button.id == 48) AutoClicker.breakBlocks = !AutoClicker.breakBlocks;
        if (button.id == 51) randomDropdownExpanded = !randomDropdownExpanded;
        if (button.id >= 52 && button.id <= 54) { AutoClicker.randomMode = button.id - 52; randomDropdownExpanded = false; }
        if (button.id == 55) AutoClicker.limitItems = !AutoClicker.limitItems;

        if (button.id == 60) nameTagsDropdownExpanded = !nameTagsDropdownExpanded;
        if (button.id == 61) NameTags.enabled = !NameTags.enabled;
        if (button.id == 62) NameTags.showHealth = !NameTags.showHealth;
        if (button.id == 63) NameTags.showItems = !NameTags.showItems;
        
        if (button.id == 64) pitEspDropdownExpanded = !pitEspDropdownExpanded;
        if (button.id == 65) PitESP.espChests = !PitESP.espChests;
        if (button.id == 66) PitESP.espDragonEggs = !PitESP.espDragonEggs;
        if (button.id == 67) PitESP.espRaffleTickets = !PitESP.espRaffleTickets;
        if (button.id == 68) PitESP.espMystics = !PitESP.espMystics;

        if (button.id == 69) EnemyESP.enabled = !EnemyESP.enabled;
        if (button.id == 70) FriendsESP.enabled = !FriendsESP.enabled;

        if (button.id == 75) AutoDenick.enabled = !AutoDenick.enabled;
        if (button.id == 76) NickedRender.enabled = !NickedRender.enabled;
        
        if (button.id == 80) EnemyHUD.enabled = !EnemyHUD.enabled;
        if (button.id == 81) NickedHUD.enabled = !NickedHUD.enabled;
        if (button.id == 82) FriendsHUD.enabled = !FriendsHUD.enabled;
        if (button.id == 85) SessionStatsHUD.enabled = !SessionStatsHUD.enabled;
        if (button.id == 86) EventHUD.enabled = !EventHUD.enabled;
        if (button.id == 87) RegHUD.enabled = !RegHUD.enabled; 
        
        if (button.id == 88) PotionHUD.enabled = !PotionHUD.enabled;
        if (button.id == 89) ArmorHUD.enabled = !ArmorHUD.enabled;
        if (button.id == 90) CoordsHUD.enabled = !CoordsHUD.enabled;

        // SWITCH TO THE MODERN HUD SETTINGS GUI
        if (button.id == 500) {
            this.mc.displayGuiScreen(new HUDSettingsGui(this));
            return;
        }

        ConfigHandler.saveConfig();
        this.initGui();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // Handle Settings Sidebar Dragging
        if (mouseX >= panelX && mouseX <= panelX + 135 && mouseY >= panelY && mouseY <= panelY + 18) {
            if (mouseButton == 0) {
                if (mouseX >= panelX + 115) panelCollapsed = !panelCollapsed;
                else draggingPanel = true;
                lastX = mouseX; lastY = mouseY;
            }
            return;
        }

        if (combatExpanded && autoClickerDropdownExpanded && whitelistField != null && whitelistField.getVisible()) {
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

        // --- UNIFIED HUD DRAG & DROP LOGIC ---
        DraggableHUD[] activeHuds = new DraggableHUD[]{
            EnemyHUD.enabled ? EnemyHUD.instance : null,
            NickedHUD.enabled ? NickedHUD.instance : null,
            FriendsHUD.enabled ? FriendsHUD.instance : null,
            SessionStatsHUD.enabled ? SessionStatsHUD.instance : null,
            EventHUD.enabled ? EventHUD.instance : null,
            RegHUD.enabled ? RegHUD.instance : null,
            PotionHUD.enabled ? PotionHUD.instance : null,
            ArmorHUD.enabled ? ArmorHUD.instance : null,
            CoordsHUD.enabled ? CoordsHUD.instance : null
        };

        for (DraggableHUD hud : activeHuds) {
            if (hud == null) continue;

            // Cheatbreaker Feature: Middle click instantly resets scale
            if (mouseButton == 2 && hud.isHovered(mouseX, mouseY)) {
                hud.scale = 1.0f;
                ConfigHandler.saveConfig();
                return;
            }

            if (mouseButton == 0) {
                // Check corners first (Scaling)
                int corner = hud.getHoveredCorner(mouseX, mouseY);
                if (corner != 0) {
                    resizingModule = hud;
                    resizingCorner = corner;
                    lastX = mouseX;
                    lastY = mouseY;
                    return;
                }

                // Check main body (Dragging)
                if (hud.isHovered(mouseX, mouseY)) {
                    draggingModule = hud;
                    lastX = mouseX;
                    lastY = mouseY;
                    return;
                }
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);

        int deltaX = mouseX - lastX;
        int deltaY = mouseY - lastY;

        if (draggingPanel) {
            panelX = Math.max(0, Math.min(this.width - 135, panelX + deltaX));
            panelY = Math.max(0, Math.min(this.height - 30, panelY + deltaY));
        }
        else if (resizingModule != null) {
            resizingModule.handleResize(deltaX, deltaY, resizingCorner);
        }
        else if (draggingModule != null) {
            draggingModule.x += deltaX;
            draggingModule.y += deltaY;

            // SCREEN CLAMPING
            int scaledW = (int) (draggingModule.width * draggingModule.scale);
            int scaledH = (int) (draggingModule.height * draggingModule.scale);

            if (draggingModule.x < 0) draggingModule.x = 0;
            if (draggingModule.x > this.width - scaledW) draggingModule.x = this.width - scaledW;
            if (draggingModule.y < 0) draggingModule.y = 0;
            if (draggingModule.y > this.height - scaledH) draggingModule.y = this.height - scaledH;
        }

        lastX = mouseX;
        lastY = mouseY;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        draggingPanel = false;
        draggingModule = null;
        resizingModule = null;
        resizingCorner = 0;

        for (GuiButton btn : this.buttonList) {
            if (btn instanceof ModernSlider) {
                ModernSlider slider = (ModernSlider) btn;
                slider.mouseReleased(mouseX, mouseY);
                if (slider.id == 47) AutoClicker.inventoryFillCps = slider.getValue();
                if (slider.id == 49) AutoClicker.minCps = slider.getValue();
                if (slider.id == 50) AutoClicker.maxCps = slider.getValue();
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