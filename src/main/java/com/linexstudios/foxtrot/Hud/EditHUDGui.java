package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Handler.ConfigHandler;
import com.linexstudios.foxtrot.Denick.AutoDenick;
import com.linexstudios.foxtrot.Combat.AutoClicker;
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

    public static boolean combatExpanded = false;
    public static boolean renderExpanded = false;
    public static boolean denickExpanded = false;
    public static boolean hudExpanded = false;

    // --- The Whitelist Text Box ---
    private ModernTextField whitelistField;

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear(); 
        Keyboard.enableRepeatEvents(true); // Allows holding backspace to delete text
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

        // Initialize the Whitelist Box
        whitelistField = new ModernTextField(100, 0, 0, btnW - 10, 14);
        whitelistField.setText(String.join(", ", AutoClicker.itemWhitelist));

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

        Gui.drawRect(panelX, panelY, panelX + 135, panelY + 18, 0xFF121212);
        this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + "Foxtrot Settings", panelX + 5, panelY + 5, -1);
        this.fontRendererObj.drawStringWithShadow(panelCollapsed ? "+" : "-", panelX + 122, panelY + 5, -1);

        if (!panelCollapsed) {
            int currentY = panelY + 22;
            
            currentY = drawCategory(currentY, "Combat", combatExpanded, 40, 47, mouseX, mouseY);
            currentY = drawCategory(currentY, "Render", renderExpanded, 10, 12, mouseX, mouseY);
            currentY = drawCategory(currentY, "Denick", denickExpanded, 20, 20, mouseX, mouseY);
            currentY = drawCategory(currentY, "HUD", hudExpanded, 30, 31, mouseX, mouseY);

            Gui.drawRect(panelX, panelY + 18, panelX + 135, currentY, 0xDD121212);
            super.drawScreen(mouseX, mouseY, partialTicks);
        }
    }

    private int drawCategory(int y, String name, boolean expanded, int startId, int endId, int mouseX, int mouseY) {
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

        // --- BAKE THE WHITELIST TEXT BOX INTO COMBAT ---
        if (name.equals("Combat") && expanded) {
            this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.GRAY + "Allowed Items:", panelX + 5, y, -1);
            y += 10;
            whitelistField.xPosition = panelX + 5;
            whitelistField.yPosition = y;
            whitelistField.setVisible(true);
            whitelistField.drawTextBox();
            y += 18;
        } else if (name.equals("Combat") && !expanded) {
            whitelistField.setVisible(false);
        }

        return y + 4;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // Intercept typing so you can type in the box without triggering Minecraft menus
        if (combatExpanded && whitelistField.getVisible() && whitelistField.isFocused()) {
            whitelistField.textboxKeyTyped(typedChar, keyCode);
            updateWhitelist();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void updateWhitelist() {
        // Instantly update the AutoClicker engine when you type
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
        // Other toggles remain the same...
        ConfigHandler.saveConfig();
        this.initGui();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // Pass mouse clicks to the text box
        if (combatExpanded && whitelistField.getVisible()) {
            whitelistField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        // Keep your existing dragging/collapsing logic here...
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
    
    @Override
    public void onGuiClosed() { 
        Keyboard.enableRepeatEvents(false); 
        ConfigHandler.saveConfig(); 
    }
    
    // Ensure all your other methods (mouseClickMove, mouseReleased, etc.) stay intact here
}