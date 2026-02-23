package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Handler.ConfigHandler;
import com.linexstudios.foxtrot.Handler.TelemetryManager;
import com.linexstudios.foxtrot.Denick.AutoDenick;
import com.linexstudios.foxtrot.Combat.AutoClicker;
import com.linexstudios.foxtrot.Render.PitESP;
import com.linexstudios.foxtrot.Enemy.EnemyESP;
import com.linexstudios.foxtrot.Render.FriendsESP;
import com.linexstudios.foxtrot.Render.NickedRender;
import com.linexstudios.foxtrot.Render.LowLifeMystic;
import com.linexstudios.foxtrot.Misc.AutoPantSwap;
import com.linexstudios.foxtrot.Misc.AutoGhead;
import com.linexstudios.foxtrot.Misc.AutoQuickMath;
import com.linexstudios.foxtrot.Misc.AutoBulletTime;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public class EditHUDGui extends GuiScreen {
    public static int collapsedX = -1, collapsedY = -1;
    public static boolean panelCollapsed = true;
    
    private int mainPanelX, mainPanelY;
    private final int panelW = 315; 
    private final int panelH = 220; 

    private int selectedTab = 0; 
    
    private boolean draggingPanel = false;
    private int lastX, lastY;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    
    private int activeSlider = 0; 
    public static boolean randomDropdownExpanded = false;

    private DraggableHUD draggingModule = null;
    private DraggableHUD resizingModule = null;
    private int resizingCorner = 0;
    private long lastClickTime = 0;
    private DraggableHUD lastClickedHUD = null;

    // ADDED "Telemetry" TO THE TABS ARRAY
    private String[] tabs = {"Combat", "Render", "Denick", "HUD", "Misc", "Telemetry"};
    private GuiTextField whitelistField;
    private String currentTooltip = null;

    // --- GUIDELINE STATE ---
    private float guideAlphaX = 0.0f;
    private float guideAlphaY = 0.0f;
    private boolean isSnappedX = false;
    private boolean isSnappedY = false;

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
        currentTooltip = null; 

        // ============================================
        //     DYNAMIC PROXIMITY GUIDELINES
        // ============================================
        float targetAlphaX = 0.0f;
        float targetAlphaY = 0.0f;
        int revealRadius = 15;

        if (draggingModule != null) {
            int scaledW = (int) (draggingModule.width * draggingModule.scale);
            int scaledH = (int) (draggingModule.height * draggingModule.scale);
            int modCenterX = draggingModule.x + scaledW / 2;
            int modCenterY = draggingModule.y + scaledH / 2;
            int screenCenterX = this.width / 2;
            int screenCenterY = this.height / 2;

            if (Math.abs(modCenterX - screenCenterX) <= revealRadius) targetAlphaX = 0.4f;
            if (Math.abs(modCenterY - screenCenterY) <= revealRadius) targetAlphaY = 0.4f;
        }

        guideAlphaX += (targetAlphaX - guideAlphaX) * 0.2f;
        guideAlphaY += (targetAlphaY - guideAlphaY) * 0.2f;

        if (guideAlphaX > 0.01f) {
            float vR = isSnappedX ? 1.0f : 0.33f;
            float vG = isSnappedX ? 0.33f : 1.0f;
            drawGuideLine(this.width / 2.0f - 0.5f, 0, 1.0f, this.height, vR, vG, 1.0f, guideAlphaX);
        }
        if (guideAlphaY > 0.01f) {
            float hR = isSnappedY ? 1.0f : 0.33f;
            float hG = isSnappedY ? 0.33f : 1.0f;
            drawGuideLine(0, this.height / 2.0f - 0.5f, this.width, 1.0f, hR, hG, 1.0f, guideAlphaY);
        }
        
        GlStateManager.pushMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.disableStandardItemLighting();

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
            this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + "Foxtrot Settings", collapsedX + 8, collapsedY + 5, -1);
            this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.WHITE + "+", collapsedX + 102, collapsedY + 5, -1);
        } else {
            drawGradientRoundedRect(mainPanelX, mainPanelY, panelW, panelH, 3.0f, 0xFA1E1E1E, 0xFA141414);
            drawSolidRect(mainPanelX + 80, mainPanelY + 15, 1, panelH - 30, 0x1AFFFFFF); 

            this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + "Foxtrot", mainPanelX + 8, mainPanelY + 8, -1);
            this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.WHITE + "-", mainPanelX + 300, mainPanelY + 8, -1);

            int tY = mainPanelY + 25;
            for (int i = 0; i < tabs.length; i++) {
                boolean hovered = isInside(mouseX, mouseY, mainPanelX + 6, tY, 70, 16);
                if (selectedTab == i) {
                    drawNeonGlow(mainPanelX + 6, tY, 70, 16, 3, 10.0f, 0x1AFF1111); 
                    drawGradientRoundedRect(mainPanelX + 6, tY, 70, 16, 3, 0xFFFF4444, 0xFFE53935); 
                    this.fontRendererObj.drawStringWithShadow(tabs[i], mainPanelX + 10, tY + 4, -1);
                } else {
                    if (hovered) drawGradientRoundedRect(mainPanelX + 6, tY, 70, 16, 3, 0xFF2A2A2A, 0xFF202020);
                    this.fontRendererObj.drawStringWithShadow(tabs[i], mainPanelX + 10, tY + 4, 0xAAAAAA);
                }
                tY += 20; 
            }

            int c1 = mainPanelX + 90; 
            int c2 = mainPanelX + 205; 
            int rY = mainPanelY + 10;

            this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + tabs[selectedTab], c1, rY, -1);
            rY += 15;

            if (selectedTab == 0) { 
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
                    whitelistField.xPosition = c1 + 5; whitelistField.yPosition = y1 + 2;
                    whitelistField.setVisible(true); GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
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
            else if (selectedTab == 1) { 
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

                drawSettingsCard(c2, y2, 100, 108);
                this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + "Pit Misc", c2 + 5, y2 + 5, -1); y2 += 18;
                drawIOSToggle(c2 + 5, y2, 100, "Sewer Chests", PitESP.espChests, mouseX, mouseY); y2 += 18;
                drawIOSToggle(c2 + 5, y2, 100, "Dragon Eggs", PitESP.espDragonEggs, mouseX, mouseY); y2 += 18;
                drawIOSToggle(c2 + 5, y2, 100, "Raffle Tickets", PitESP.espRaffleTickets, mouseX, mouseY); y2 += 18;
                drawIOSToggle(c2 + 5, y2, 100, "Mystic Drops", PitESP.espMystics, mouseX, mouseY); y2 += 18;
                drawIOSToggle(c2 + 5, y2, 100, "Low Life Mystic", LowLifeMystic.enabled, mouseX, mouseY); 
            }
            else if (selectedTab == 2) { 
                if (whitelistField != null) whitelistField.setVisible(false);
                int y1 = rY;
                drawSettingsCard(c1, y1, 105, 50);
                drawIOSToggle(c1 + 5, y1 + 6, 105, "Auto Denick", AutoDenick.enabled, mouseX, mouseY); 
                drawIOSToggle(c1 + 5, y1 + 24, 105, "Nicked Tags", NickedRender.enabled, mouseX, mouseY);
            }
            else if (selectedTab == 3) { 
                if (whitelistField != null) whitelistField.setVisible(false);
                int y1 = rY; int y2 = rY;
                drawSettingsCard(c1, y1, 105, 162);
                drawIOSToggle(c1 + 5, y1 + 6, 105, "Enemy HUD", EnemyHUD.enabled, mouseX, mouseY); y1 += 18;
                drawIOSToggle(c1 + 5, y1 + 6, 105, "Nicked HUD", NickedHUD.enabled, mouseX, mouseY); y1 += 18;
                drawIOSToggle(c1 + 5, y1 + 6, 105, "Friends HUD", FriendsHUD.enabled, mouseX, mouseY); y1 += 18;
                drawIOSToggle(c1 + 5, y1 + 6, 105, "Session Stats", SessionStatsHUD.enabled, mouseX, mouseY); y1 += 18;
                drawIOSToggle(c1 + 5, y1 + 6, 105, "Event Tracker", EventHUD.enabled, mouseX, mouseY); y1 += 18;
                drawIOSToggle(c1 + 5, y1 + 6, 105, "Boss Bar", BossBarModule.enabled, mouseX, mouseY); y1 += 18;
                drawIOSToggle(c1 + 5, y1 + 6, 105, "CPS HUD", CPSModule.enabled, mouseX, mouseY); y1 += 18;
                drawIOSToggle(c1 + 5, y1 + 6, 105, "FPS HUD", FPSModule.enabled, mouseX, mouseY);

                drawSettingsCard(c2, y2, 100, 108); 
                drawIOSToggle(c2 + 5, y2 + 6, 100, "Reg HUD", RegHUD.enabled, mouseX, mouseY); y2 += 18;
                drawIOSToggle(c2 + 5, y2 + 6, 100, "Darks HUD", DarksHUD.enabled, mouseX, mouseY); y2 += 18;
                drawIOSToggle(c2 + 5, y2 + 6, 100, "Potion HUD", PotionHUD.enabled, mouseX, mouseY); y2 += 18;
                drawIOSToggle(c2 + 5, y2 + 6, 100, "Armor HUD", ArmorHUD.enabled, mouseX, mouseY); y2 += 18;
                drawIOSToggle(c2 + 5, y2 + 6, 100, "Coords HUD", CoordsHUD.enabled, mouseX, mouseY); y2 += 28;
                drawIOSButton(c2, y2 + 4, 100, 14, "HUD Customization", mouseX, mouseY);
            }
            else if (selectedTab == 4) {
                if (whitelistField != null) whitelistField.setVisible(false);
                int y1 = rY + 18; int y2 = rY + 18;
                drawSettingsCard(c1, y1 - 18, 105, 126); 
                this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + "Auto Use", c1 + 5, y1 - 13, -1); 
                
                drawIOSToggle(c1 + 5, y1, 105, "Pant Swap", AutoPantSwap.pantSwapEnabled, mouseX, mouseY); 
                if (isInside(mouseX, mouseY, c1 + 5, y1, 75, 12)) currentTooltip = EnumChatFormatting.RED + "\u26A0 " + EnumChatFormatting.YELLOW + "USE AT YOUR OWN RISK." + EnumChatFormatting.GRAY + " - " + EnumChatFormatting.GRAY + "Auto Swap / Hold right-click while holding over pants in your inventory to instantly equip it.";
                y1 += 18;
                
                drawIOSToggle(c1 + 5, y1, 105, "Venom Swap", AutoPantSwap.venomSwapEnabled, mouseX, mouseY); 
                if (isInside(mouseX, mouseY, c1 + 5, y1, 75, 12)) currentTooltip = EnumChatFormatting.RED + "\u26A0 " + EnumChatFormatting.YELLOW + "USE AT YOUR OWN RISK." + EnumChatFormatting.GRAY + " - " + EnumChatFormatting.GRAY + "Automatically swaps to diamond pants if you get venomed.";
                y1 += 18;
                
                drawIOSToggle(c1 + 5, y1, 105, "Auto Heal", AutoGhead.enabled, mouseX, mouseY); 
                if (isInside(mouseX, mouseY, c1 + 5, y1, 75, 12)) currentTooltip = EnumChatFormatting.RED + "\u26A0 " + EnumChatFormatting.YELLOW + "USE AT YOUR OWN RISK." + EnumChatFormatting.GRAY + " - " + EnumChatFormatting.GRAY + "Automatically use Ghead or First Aid Egg.";
                y1 += 18;
                
                drawIOSToggle(c1 + 5, y1, 105, "Auto Pod", AutoPantSwap.autoPodEnabled, mouseX, mouseY); 
                if (isInside(mouseX, mouseY, c1 + 5, y1, 75, 12)) currentTooltip = EnumChatFormatting.RED + "\u26A0 " + EnumChatFormatting.YELLOW + "USE AT YOUR OWN RISK." + EnumChatFormatting.GRAY + " - " + EnumChatFormatting.GRAY + "Automatically use Escape Pods when you are at low health";
                y1 += 18;
                
                drawIOSToggle(c1 + 5, y1, 105, "Auto Bullet Time", AutoBulletTime.enabled, mouseX, mouseY); 
                if (isInside(mouseX, mouseY, c1 + 5, y1, 75, 12)) currentTooltip = EnumChatFormatting.YELLOW + "\u26A0 " + EnumChatFormatting.RED + "USE AT YOUR OWN RISK, POSSIBLE BAN." + EnumChatFormatting.GRAY + " - " + EnumChatFormatting.GRAY + "Automatically switch to Bullet Time when you right click on any sword.";
                y1 += 18;

                drawSettingsCard(c2, y2 - 18, 100, 36);
                this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + "Chat", c2 + 5, y2 - 13, -1);
                drawIOSToggle(c2 + 5, y2, 100, "Auto Quick Math", AutoQuickMath.enabled, mouseX, mouseY); 
            }
            // ============================================
            //     NEW: TELEMETRY TAB
            // ============================================
            else if (selectedTab == 5) {
                if (whitelistField != null) whitelistField.setVisible(false);
                int y1 = rY;
                
                drawSettingsCard(c1, y1, 215, 40);
                drawIOSToggle(c1 + 5, y1 + 6, 205, "Enable Telemetry Stats", ConfigHandler.telemetryEnabled, mouseX, mouseY);
                this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.GRAY + "Help us improve Foxtrot PIT!", c1 + 5, y1 + 22, -1);
                
                y1 += 45;
                
                // Privacy Policy Description (Height increased to 125 to fit the new line)
                drawSettingsCard(c1, y1, 215, 125);
                this.fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "Privacy & Anonymity", c1 + 5, y1 + 5, -1);
                
                int textY = y1 + 20;
                this.fontRendererObj.drawString(EnumChatFormatting.GRAY + "All telemetry data is " + EnumChatFormatting.WHITE + "100% Anonymous" + EnumChatFormatting.GRAY + ".", c1 + 5, textY, -1); textY += 10;
                this.fontRendererObj.drawString(EnumChatFormatting.GRAY + "We CANNOT track or collect:", c1 + 5, textY, -1); textY += 12;
                
                this.fontRendererObj.drawString(EnumChatFormatting.RED + "\u2718 " + EnumChatFormatting.GRAY + "Your Minecraft Name or UUID", c1 + 10, textY, -1); textY += 10;
                this.fontRendererObj.drawString(EnumChatFormatting.RED + "\u2718 " + EnumChatFormatting.GRAY + "Your Session Token or Passwords", c1 + 10, textY, -1); textY += 10;
                this.fontRendererObj.drawString(EnumChatFormatting.RED + "\u2718 " + EnumChatFormatting.GRAY + "Your IP Address or Location", c1 + 10, textY, -1); textY += 10;
                this.fontRendererObj.drawString(EnumChatFormatting.RED + "\u2718 " + EnumChatFormatting.GRAY + "Your Chat Logs or Inventories", c1 + 10, textY, -1); textY += 15;
                
                this.fontRendererObj.drawString(EnumChatFormatting.GRAY + "We only track active player counts to", c1 + 5, textY, -1); textY += 10;
                this.fontRendererObj.drawString(EnumChatFormatting.GRAY + "display live stats on https://linex-studios.github.io", c1 + 5, textY, -1);
            }
        }
        
        if (currentTooltip != null) {
            drawTooltip(currentTooltip, mouseX, mouseY);
        }
    }

    private void drawGuideLine(float x, float y, float w, float h, float r, float g, float b, float alpha) {
        setupSmoothRender(false);
        GlStateManager.color(r, g, b, alpha); 
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y + h);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x + w, y);
        GL11.glEnd();
        endSmoothRender();
    }
    
    private void drawTooltip(String text, int x, int y) {
        int stringWidth = this.fontRendererObj.getStringWidth(text);
        int pad = 4;
        int drawX = x + 10;
        int drawY = y - 10;
        if (drawX + stringWidth + (pad*2) > this.width) {
            drawX = this.width - stringWidth - (pad*2);
        }
        drawRoundedRect(drawX, drawY, stringWidth + (pad*2), this.fontRendererObj.FONT_HEIGHT + (pad*2), 3, 0xF9111111);
        drawRoundedOutline(drawX, drawY, stringWidth + (pad*2), this.fontRendererObj.FONT_HEIGHT + (pad*2), 3, 1.0f, 0x55FFFFFF);
        this.fontRendererObj.drawStringWithShadow(text, drawX + pad, drawY + pad, -1);
    }

    private void updateEditSlider(int slider, int mouseX, int startX) {
        float pct = (mouseX - startX) / 90f; 
        pct = Math.max(0, Math.min(1, pct));
        if (slider == 1) AutoClicker.minCps = 1.0f + (pct * 19.0f);
        if (slider == 2) AutoClicker.maxCps = 1.0f + (pct * 19.0f);
        if (slider == 3) AutoClicker.inventoryFillCps = 5.0f + (pct * 15.0f);
    }

    private void drawSettingsCard(int x, int y, int w, int h) { drawRoundedRect(x, y, w, h, 3.0f, 0x11FFFFFF); }
    private void setupSmoothRender(boolean isGradient) { GlStateManager.pushMatrix(); GlStateManager.enableBlend(); GlStateManager.disableTexture2D(); GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0); GL11.glDisable(GL11.GL_LINE_SMOOTH); GL11.glDisable(GL11.GL_POLYGON_SMOOTH); GL11.glDisable(GL11.GL_CULL_FACE); if (isGradient) GlStateManager.shadeModel(GL11.GL_SMOOTH); }
    private void endSmoothRender() { GlStateManager.shadeModel(GL11.GL_FLAT); GL11.glEnable(GL11.GL_CULL_FACE); GlStateManager.enableTexture2D(); GlStateManager.disableBlend(); GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F); GlStateManager.popMatrix(); }
    private void setColor(int color) { float a = (color >> 24 & 0xFF) / 255.0F; float r = (color >> 16 & 0xFF) / 255.0F; float g = (color >> 8 & 0xFF) / 255.0F; float b = (color & 0xFF) / 255.0F; GlStateManager.color(r, g, b, a); }
    private void drawNeonGlow(float x, float y, float width, float height, float radius, float spread, int color) { float a = (color >> 24 & 0xFF) / 255.0F; for (float s = spread; s > 0; s -= 1.0f) { float currentAlpha = a * (1.0f - (s / spread)); int c = ((int)(currentAlpha * 255) << 24) | (color & 0x00FFFFFF); drawRoundedOutline(x - s, y - s, width + (s*2), height + (s*2), radius + s, 1.0f, c); } }
    private void drawGradientRoundedRect(float x, float y, float width, float height, float radius, int topColor, int bottomColor) { setupSmoothRender(true); float x1 = x + width; float y1 = y + height; GL11.glBegin(GL11.GL_POLYGON); setColor(topColor); for (int i = 180; i <= 270; i += 5) GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(i)) * radius), (float)(y + radius + Math.sin(Math.toRadians(i)) * radius)); for (int i = 270; i <= 360; i += 5) GL11.glVertex2f((float)(x1 - radius + Math.cos(Math.toRadians(i)) * radius), (float)(y + radius + Math.sin(Math.toRadians(i)) * radius)); setColor(bottomColor); for (int i = 0; i <= 90; i += 5) GL11.glVertex2f((float)(x1 - radius + Math.cos(Math.toRadians(i)) * radius), (float)(y1 - radius + Math.sin(Math.toRadians(i)) * radius)); for (int i = 90; i <= 180; i += 5) GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(i)) * radius), (float)(y1 - radius + Math.sin(Math.toRadians(i)) * radius)); GL11.glEnd(); endSmoothRender(); }
    private void drawRoundedRect(float x, float y, float width, float height, float radius, int color) { setupSmoothRender(false); setColor(color); float x1 = x + width; float y1 = y + height; GL11.glBegin(GL11.GL_POLYGON); for (int i = 180; i <= 270; i += 5) GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(i)) * radius), (float)(y + radius + Math.sin(Math.toRadians(i)) * radius)); for (int i = 270; i <= 360; i += 5) GL11.glVertex2f((float)(x1 - radius + Math.cos(Math.toRadians(i)) * radius), (float)(y + radius + Math.sin(Math.toRadians(i)) * radius)); for (int i = 0; i <= 90; i += 5) GL11.glVertex2f((float)(x1 - radius + Math.cos(Math.toRadians(i)) * radius), (float)(y1 - radius + Math.sin(Math.toRadians(i)) * radius)); for (int i = 90; i <= 180; i += 5) GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(i)) * radius), (float)(y1 - radius + Math.sin(Math.toRadians(i)) * radius)); GL11.glEnd(); endSmoothRender(); }
    private void drawRoundedOutline(float x, float y, float width, float height, float radius, float lineWidth, int color) { setupSmoothRender(false); setColor(color); GL11.glLineWidth(lineWidth); float x1 = x + width; float y1 = y + height; GL11.glBegin(GL11.GL_LINE_LOOP); for (int i = 180; i <= 270; i += 5) GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(i)) * radius), (float)(y + radius + Math.sin(Math.toRadians(i)) * radius)); for (int i = 270; i <= 360; i += 5) GL11.glVertex2f((float)(x1 - radius + Math.cos(Math.toRadians(i)) * radius), (float)(y + radius + Math.sin(Math.toRadians(i)) * radius)); for (int i = 0; i <= 90; i += 5) GL11.glVertex2f((float)(x1 - radius + Math.cos(Math.toRadians(i)) * radius), (float)(y1 - radius + Math.sin(Math.toRadians(i)) * radius)); for (int i = 90; i <= 180; i += 5) GL11.glVertex2f((float)(x + radius + Math.cos(Math.toRadians(i)) * radius), (float)(y1 - radius + Math.sin(Math.toRadians(i)) * radius)); GL11.glEnd(); endSmoothRender(); }
    private void drawSolidRect(float x, float y, float w, float h, int color) { drawRoundedRect(x, y, w, h, 0, color); }
    private void drawCircle(float cx, float cy, float radius, int color) { setupSmoothRender(false); setColor(color); GL11.glBegin(GL11.GL_POLYGON); for (int i = 0; i <= 360; i += 5) GL11.glVertex2f((float)(cx + Math.cos(Math.toRadians(i)) * radius), (float)(cy + Math.sin(Math.toRadians(i)) * radius)); GL11.glEnd(); endSmoothRender(); }
    private void drawCircleOutline(float cx, float cy, float radius, float lineWidth, int color) { setupSmoothRender(false); setColor(color); GL11.glLineWidth(lineWidth); GL11.glBegin(GL11.GL_LINE_LOOP); for (int i = 0; i <= 360; i += 5) GL11.glVertex2f((float)(cx + Math.cos(Math.toRadians(i)) * radius), (float)(cy + Math.sin(Math.toRadians(i)) * radius)); GL11.glEnd(); endSmoothRender(); }

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
        boolean hovered = isInside(mx, my, swX, swY, swW, swH);
        if (isOn) {
            drawNeonGlow(swX, swY, swW, swH, swH / 2, 6.0f, 0x1AFF3333); 
            drawRoundedRect(swX, swY, swW, swH, swH / 2, 0xFFE53935); 
        } else {
            drawRoundedRect(swX, swY, swW, swH, swH / 2, hovered ? 0xFF555555 : 0xFF444444); 
        }
        float rad = swH / 2 - 1.0f; float cx = isOn ? swX + swW - rad - 1.0f : swX + rad + 1.0f;
        drawCircle(cx, swY + swH / 2, rad, 0xFFFFFFFF); 
    }

    private void drawIOSButton(float x, float y, float w, float h, String text, int mx, int my) {
        boolean hovered = isInside(mx, my, x, y, w, h);
        int topColor = hovered ? 0xFF3D3D3D : 0xFF2F2F2F;
        int botColor = hovered ? 0xFF2A2A2A : 0xFF1C1C1C;
        drawGradientRoundedRect(x, y, w, h, 3, topColor, botColor); 
        this.fontRendererObj.drawStringWithShadow(text, x + (w - this.fontRendererObj.getStringWidth(text)) / 2, y + (h - 8) / 2, -1);
    }

    private boolean isInside(float mx, float my, float x, float y, float w, float h) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) return;

        if (panelCollapsed) {
            if (isInside(mouseX, mouseY, collapsedX + 115 - 20, collapsedY, 20, 18)) {
                panelCollapsed = false; return;
            }
            if (isInside(mouseX, mouseY, collapsedX, collapsedY, 115 - 20, 18)) {
                draggingPanel = true; lastX = mouseX; lastY = mouseY; return;
            }
        } else {
            if (isInside(mouseX, mouseY, mainPanelX + panelW - 25, mainPanelY, 25, 20)) {
                panelCollapsed = true; return;
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
                    if (isInside(mouseX, mouseY, c2 + 5, y2 + 66, 90, 14)) { randomDropdownExpanded = !randomDropdownExpanded; } 
                    else if (!randomDropdownExpanded) {
                        if (isInside(mouseX, mouseY, c2 + 5, y2 + 5, 90, 15)) { activeSlider = 1; updateEditSlider(1, mouseX, c2 + 5); }
                        else if (isInside(mouseX, mouseY, c2 + 5, y2 + 25, 90, 15)) { activeSlider = 2; updateEditSlider(2, mouseX, c2 + 5); }
                        else if (isInside(mouseX, mouseY, c2 + 5, y2 + 45, 90, 15)) { activeSlider = 3; updateEditSlider(3, mouseX, c2 + 5); }
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
                if (isInside(mouseX, mouseY, c2 + 5, y2, 100, 12)) PitESP.espMystics = !PitESP.espMystics; y2 += 18;
                if (isInside(mouseX, mouseY, c2 + 5, y2, 100, 12)) LowLifeMystic.enabled = !LowLifeMystic.enabled; 
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
                if (isInside(mouseX, mouseY, c1 + 5, y1 + 6, 100, 12)) EventHUD.enabled = !EventHUD.enabled; y1 += 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1 + 6, 100, 12)) BossBarModule.enabled = !BossBarModule.enabled; y1 += 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1 + 6, 100, 12)) CPSModule.enabled = !CPSModule.enabled; y1 += 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1 + 6, 100, 12)) FPSModule.enabled = !FPSModule.enabled;
                
                if (isInside(mouseX, mouseY, c2 + 5, y2 + 6, 100, 12)) RegHUD.enabled = !RegHUD.enabled; y2 += 18;
                if (isInside(mouseX, mouseY, c2 + 5, y2 + 6, 100, 12)) DarksHUD.enabled = !DarksHUD.enabled; y2 += 18;
                if (isInside(mouseX, mouseY, c2 + 5, y2 + 6, 100, 12)) PotionHUD.enabled = !PotionHUD.enabled; y2 += 18;
                if (isInside(mouseX, mouseY, c2 + 5, y2 + 6, 100, 12)) ArmorHUD.enabled = !ArmorHUD.enabled; y2 += 18;
                if (isInside(mouseX, mouseY, c2 + 5, y2 + 6, 100, 12)) CoordsHUD.enabled = !CoordsHUD.enabled; y2 += 28;

                if (isInside(mouseX, mouseY, c2, y2 + 4, 100, 14)) { 
                    this.mc.displayGuiScreen(new HUDSettingsGui(this)); return; 
                }
            }
            else if (selectedTab == 4) {
                int y1 = rY + 18; int y2 = rY + 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1, 100, 12)) AutoPantSwap.pantSwapEnabled = !AutoPantSwap.pantSwapEnabled; y1 += 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1, 100, 12)) AutoPantSwap.venomSwapEnabled = !AutoPantSwap.venomSwapEnabled; y1 += 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1, 100, 12)) AutoGhead.enabled = !AutoGhead.enabled; y1 += 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1, 100, 12)) AutoPantSwap.autoPodEnabled = !AutoPantSwap.autoPodEnabled; y1 += 18;
                if (isInside(mouseX, mouseY, c1 + 5, y1, 100, 12)) AutoBulletTime.enabled = !AutoBulletTime.enabled; 
                
                if (isInside(mouseX, mouseY, c2 + 5, y2, 100, 12)) AutoQuickMath.enabled = !AutoQuickMath.enabled;
            }
            // ============================================
            //     NEW: TELEMETRY CLICK LOGIC
            // ============================================
            else if (selectedTab == 5) {
                int y1 = rY;
                if (isInside(mouseX, mouseY, c1 + 5, y1 + 6, 205, 12)) {
                    ConfigHandler.telemetryEnabled = !ConfigHandler.telemetryEnabled;
                    if (ConfigHandler.telemetryEnabled) {
                        TelemetryManager.initialize(); // Boot it up instantly!
                    }
                }
            }
            ConfigHandler.saveConfig();
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (DraggableHUD hud : DraggableHUD.getRegistry()) {
            if (!hud.isEnabled()) continue;
            
            if (mouseButton == 2 && hud.isHovered(mouseX, mouseY)) { 
                hud.scale = 1.0f; ConfigHandler.saveConfig(); return; 
            }
            
            if (mouseButton == 0 && hud.isHovered(mouseX, mouseY)) {
                long currentTime = System.currentTimeMillis();
                if (hud == lastClickedHUD && (currentTime - lastClickTime < 300)) { 
                    this.mc.displayGuiScreen(new HUDSettingsGui(this, getTabIndexFromName(hud.name))); return; 
                }
                lastClickTime = currentTime; 
                lastClickedHUD = hud;
                int corner = hud.getHoveredCorner(mouseX, mouseY);
                if (corner != 0) { resizingModule = hud; resizingCorner = corner; lastX = mouseX; lastY = mouseY; return; }
                
                draggingModule = hud; 
                dragOffsetX = mouseX - hud.x;
                dragOffsetY = mouseY - hud.y;
                return;
            }
        }
    }

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
        if (lower.contains("dark")) return 9; 
        if (lower.contains("sprint")) return 10;
        if (lower.contains("cps")) return 11; 
        if (lower.contains("fps")) return 12; 
        if (lower.contains("boss")) return 13;
        return 0; 
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
        } else if (resizingModule != null) {
            resizingModule.handleResize(deltaX, deltaY, resizingCorner);
        } else if (draggingModule != null) {
            
            int targetX = mouseX - dragOffsetX;
            int targetY = mouseY - dragOffsetY;
            int scaledW = (int) (draggingModule.width * draggingModule.scale);
            int scaledH = (int) (draggingModule.height * draggingModule.scale);
            
            int modCenterX = targetX + scaledW / 2;
            int modCenterY = targetY + scaledH / 2;
            int screenCenterX = this.width / 2;
            int screenCenterY = this.height / 2;
            
            int snapRadius = 6; 
            isSnappedX = false;
            isSnappedY = false;
            
            if (Math.abs(modCenterX - screenCenterX) <= snapRadius) {
                draggingModule.x = screenCenterX - scaledW / 2;
                isSnappedX = true;
            } else {
                draggingModule.x = targetX;
            }

            if (Math.abs(modCenterY - screenCenterY) <= snapRadius) {
                draggingModule.y = screenCenterY - scaledH / 2;
                isSnappedY = true;
            } else {
                draggingModule.y = targetY;
            }

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
        isSnappedX = false; isSnappedY = false;
        ConfigHandler.saveConfig();
    }

    @Override
    public void onGuiClosed() { Keyboard.enableRepeatEvents(false); ConfigHandler.saveConfig(); }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}