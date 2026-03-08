package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Handler.*;
import com.linexstudios.foxtrot.Denick.*;
import com.linexstudios.foxtrot.Combat.AutoClicker;
import com.linexstudios.foxtrot.Render.*;
import com.linexstudios.foxtrot.Misc.*;
import com.linexstudios.foxtrot.Enemy.EnemyESP;
import com.linexstudios.foxtrot.Update.FoxtrotTweaker;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class EditHUDGui extends GuiScreen {
    public static int collapsedX = -1, collapsedY = -1;
    public static boolean panelCollapsed = true, randomDropdownExpanded = false;
    private int mainPanelX, mainPanelY, selectedTab = 0, lastX, lastY, dragOffsetX = 0, dragOffsetY = 0, activeSlider = 0, resizingCorner = 0;
    private final int panelW = 315, panelH = 240;
    private boolean draggingPanel = false, isSnappedX = false, isSnappedY = false;
    private DraggableHUD draggingModule = null, resizingModule = null, lastClickedHUD = null;
    private long lastClickTime = 0;
    private String[] tabs = {"Combat", "Render", "Denick", "HUD", "Misc", "Telemetry", "Updates"};
    private GuiTextField whitelistField;
    private String currentTooltip = null;
    private float guideAlphaX = 0.0f, guideAlphaY = 0.0f;

    @Override public void initGui() {
        super.initGui(); Keyboard.enableRepeatEvents(true); if (this.width <= 0) return;
        mainPanelX = (this.width - panelW) / 2; mainPanelY = (this.height - panelH) / 2;
        if (collapsedX == -1) { collapsedX = mainPanelX + panelW - 115; collapsedY = mainPanelY - 20; }
        if (whitelistField == null) { whitelistField = new GuiTextField(100, this.fontRendererObj, 0, 0, 95, 12); whitelistField.setMaxStringLength(256); whitelistField.setText(String.join(", ", AutoClicker.itemWhitelist)); whitelistField.setVisible(false); }
    }

    @Override public void drawScreen(int mX, int mY, float pT) {
        this.drawDefaultBackground(); currentTooltip = null;
        float tAx = 0, tAy = 0; int cx = this.width / 2, cy = this.height / 2;
        if (draggingModule != null) { int mCx = draggingModule.x + (int)(draggingModule.width * draggingModule.scale) / 2, mCy = draggingModule.y + (int)(draggingModule.height * draggingModule.scale) / 2; if (Math.abs(mCx - cx) <= 15) tAx = 0.4f; if (Math.abs(mCy - cy) <= 15) tAy = 0.4f; }
        guideAlphaX += (tAx - guideAlphaX) * 0.2f; guideAlphaY += (tAy - guideAlphaY) * 0.2f;
        if (guideAlphaX > 0.01f) RenderUtils.drawGuideLine(cx - 0.5f, 0, 1.0f, this.height, isSnappedX ? 1.0f : 0.33f, isSnappedX ? 0.33f : 1.0f, 1.0f, guideAlphaX);
        if (guideAlphaY > 0.01f) RenderUtils.drawGuideLine(0, cy - 0.5f, this.width, 1.0f, isSnappedY ? 1.0f : 0.33f, isSnappedY ? 0.33f : 1.0f, 1.0f, guideAlphaY);

        GlStateManager.pushMatrix(); GlStateManager.enableTexture2D(); GlStateManager.enableAlpha(); GlStateManager.enableBlend(); GlStateManager.color(1, 1, 1, 1); RenderHelper.disableStandardItemLighting();
        for (DraggableHUD h : DraggableHUD.getRegistry()) if (h.isEnabled()) h.render(true, mX, mY);
        GlStateManager.popMatrix(); GlStateManager.disableLighting(); GlStateManager.enableBlend();

        if (panelCollapsed) {
            RenderUtils.drawGradientRoundedRect(collapsedX, collapsedY, 115, 18, 3, 0xFA1E1E1E, 0xFA141414);
            fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + "Foxtrot Settings", collapsedX + 8, collapsedY + 5, -1);
            fontRendererObj.drawStringWithShadow(EnumChatFormatting.WHITE + "+", collapsedX + 102, collapsedY + 5, -1);
        } else {
            RenderUtils.drawGradientRoundedRect(mainPanelX, mainPanelY, panelW, panelH, 3, 0xFA1E1E1E, 0xFA141414); RenderUtils.drawSolidRect(mainPanelX + 80, mainPanelY + 15, 1, panelH - 30, 0x1AFFFFFF);
            fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + "Foxtrot", mainPanelX + 8, mainPanelY + 8, -1); fontRendererObj.drawStringWithShadow(EnumChatFormatting.WHITE + "-", mainPanelX + 300, mainPanelY + 8, -1);

            for (int i = 0, y = mainPanelY + 25; i < tabs.length; i++, y += 20) {
                if (selectedTab == i) { RenderUtils.drawNeonGlow(mainPanelX + 6, y, 70, 16, 3, 10, 0x1AFF1111); RenderUtils.drawGradientRoundedRect(mainPanelX + 6, y, 70, 16, 3, 0xFFFF4444, 0xFFE53935); fontRendererObj.drawStringWithShadow(tabs[i], mainPanelX + 10, y + 4, -1); } 
                else { if (isInside(mX, mY, mainPanelX + 6, y, 70, 16)) RenderUtils.drawGradientRoundedRect(mainPanelX + 6, y, 70, 16, 3, 0xFF2A2A2A, 0xFF202020); fontRendererObj.drawStringWithShadow(tabs[i], mainPanelX + 10, y + 4, 0xAAAAAA); }
                if (tabs[i].equals("Updates") && !ConfigHandler.autoUpdateEnabled && FoxtrotTweaker.UPDATE_AVAILABLE) fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + "\u2022", mainPanelX + 12 + fontRendererObj.getStringWidth(tabs[i]), y + 4, -1);
            }

            int c1 = mainPanelX + 90, c2 = mainPanelX + 205, y1 = mainPanelY + 25, y2 = y1;
            fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + tabs[selectedTab], c1, mainPanelY + 10, -1);
            if (whitelistField != null) whitelistField.setVisible(false);

            if (selectedTab == 0) {
                drawSettingsCard(c1, y1, 105, AutoClicker.limitItems ? 165 : 145); fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + "Autoclicker", c1 + 5, y1 + 5, -1); y1 += 16;
                drawIOSToggle(c1 + 5, y1, 105, "Enabled", AutoClicker.enabled, mX, mY); y1 += 18; drawIOSToggle(c1 + 5, y1, 105, "Left Click", AutoClicker.leftClick, mX, mY); y1 += 18; drawIOSToggle(c1 + 5, y1, 105, "Hold Click", AutoClicker.holdToClick, mX, mY); y1 += 18; drawIOSToggle(c1 + 5, y1, 105, "Fast Place", AutoClicker.fastPlaceEnabled, mX, mY); y1 += 18; drawIOSToggle(c1 + 5, y1, 105, "Break Blocks", AutoClicker.breakBlocks, mX, mY); y1 += 18; drawIOSToggle(c1 + 5, y1, 105, "Inventory Fill", AutoClicker.inventoryFill, mX, mY); y1 += 18; drawIOSToggle(c1 + 5, y1, 105, "Limit Items", AutoClicker.limitItems, mX, mY); y1 += 18;
                if (AutoClicker.limitItems) { whitelistField.xPosition = c1 + 5; whitelistField.yPosition = y1 + 2; whitelistField.setVisible(true); GlStateManager.color(1, 1, 1, 1); whitelistField.drawTextBox(); }
                drawSettingsCard(c2, y2, 100, randomDropdownExpanded ? 135 : 90); drawIOSSlider(c2 + 5, y2 + 5, "Min CPS", AutoClicker.minCps, 1, 20, 90); y2 += 20; drawIOSSlider(c2 + 5, y2 + 5, "Max CPS", AutoClicker.maxCps, 1, 20, 90); y2 += 20; drawIOSSlider(c2 + 5, y2 + 5, "Fill CPS", AutoClicker.inventoryFillCps, 5, 20, 90); y2 += 26;
                drawIOSButton(c2 + 5, y2, 90, 14, "Mode: " + (AutoClicker.randomMode == 0 ? "Normal" : AutoClicker.randomMode == 1 ? "Extra" : "Extra+"), mX, mY); y2 += 16;
                if (randomDropdownExpanded) { drawIOSButton(c2 + 5, y2, 90, 12, (AutoClicker.randomMode == 0 ? EnumChatFormatting.RED : EnumChatFormatting.GRAY) + "Normal", mX, mY); y2 += 14; drawIOSButton(c2 + 5, y2, 90, 12, (AutoClicker.randomMode == 1 ? EnumChatFormatting.RED : EnumChatFormatting.GRAY) + "Extra", mX, mY); y2 += 14; drawIOSButton(c2 + 5, y2, 90, 12, (AutoClicker.randomMode == 2 ? EnumChatFormatting.RED : EnumChatFormatting.GRAY) + "Extra+", mX, mY); }
            } else if (selectedTab == 1) {
                drawSettingsCard(c1, y1, 105, 70); fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + "NameTags", c1 + 5, y1 + 5, -1); y1 += 18; drawIOSToggle(c1 + 5, y1, 105, "Enabled", NameTags.enabled, mX, mY); y1 += 18; drawIOSToggle(c1 + 5, y1, 105, "Show Health", NameTags.showHealth, mX, mY); y1 += 18; drawIOSToggle(c1 + 5, y1, 105, "Show Items", NameTags.showItems, mX, mY); y1 += 22;
                drawSettingsCard(c1, y1, 105, 52); fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + "Player ESP", c1 + 5, y1 + 5, -1); y1 += 18; drawIOSToggle(c1 + 5, y1, 105, "Enemy ESP", EnemyESP.enabled, mX, mY); y1 += 18; drawIOSToggle(c1 + 5, y1, 105, "Friends ESP", FriendsESP.enabled, mX, mY);
                drawSettingsCard(c2, y2, 100, 108); fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + "Pit Misc", c2 + 5, y2 + 5, -1); y2 += 18; drawIOSToggle(c2 + 5, y2, 100, "Sewer Chests", PitESP.espChests, mX, mY); y2 += 18; drawIOSToggle(c2 + 5, y2, 100, "Dragon Eggs", PitESP.espDragonEggs, mX, mY); y2 += 18; drawIOSToggle(c2 + 5, y2, 100, "Raffle Tickets", PitESP.espRaffleTickets, mX, mY); y2 += 18; drawIOSToggle(c2 + 5, y2, 100, "Mystic Drops", PitESP.espMystics, mX, mY); y2 += 18; drawIOSToggle(c2 + 5, y2, 100, "Low Life Mystic", LowLifeMystic.enabled, mX, mY);
            } else if (selectedTab == 2) {
                drawSettingsCard(c1, y1, 105, 50); drawIOSToggle(c1 + 5, y1 + 6, 105, "Auto Denick", AutoDenick.enabled, mX, mY); drawIOSToggle(c1 + 5, y1 + 24, 105, "Nicked Tags", NickedRender.enabled, mX, mY);
            } else if (selectedTab == 3) {
                drawSettingsCard(c1, y1, 105, 162); drawIOSToggle(c1 + 5, y1 += 6, 105, "Enemy HUD", EnemyHUD.enabled, mX, mY); drawIOSToggle(c1 + 5, y1 += 18, 105, "Nicked HUD", NickedHUD.enabled, mX, mY); drawIOSToggle(c1 + 5, y1 += 18, 105, "Friends HUD", FriendsHUD.enabled, mX, mY); drawIOSToggle(c1 + 5, y1 += 18, 105, "Session Stats", SessionStatsHUD.enabled, mX, mY); drawIOSToggle(c1 + 5, y1 += 18, 105, "Event Tracker", EventHUD.enabled, mX, mY); drawIOSToggle(c1 + 5, y1 += 18, 105, "Boss Bar", BossBarModule.enabled, mX, mY); drawIOSToggle(c1 + 5, y1 += 18, 105, "CPS HUD", CPSModule.enabled, mX, mY); drawIOSToggle(c1 + 5, y1 += 18, 105, "FPS HUD", FPSModule.enabled, mX, mY);
                drawSettingsCard(c2, y2, 100, 162); drawIOSToggle(c2 + 5, y2 += 6, 100, "Reg HUD", RegHUD.enabled, mX, mY); drawIOSToggle(c2 + 5, y2 += 18, 100, "Darks HUD", DarksHUD.enabled, mX, mY); drawIOSToggle(c2 + 5, y2 += 18, 100, "Potion HUD", PotionHUD.enabled, mX, mY); drawIOSToggle(c2 + 5, y2 += 18, 100, "Armor HUD", ArmorHUD.enabled, mX, mY); drawIOSToggle(c2 + 5, y2 += 18, 100, "Coords HUD", CoordsHUD.enabled, mX, mY); drawIOSToggle(c2 + 5, y2 += 18, 100, "Telebow Timer", TelebowHUD.enabled, mX, mY); drawIOSToggle(c2 + 5, y2 += 18, 100, "Player Counter", PlayerCounterHUD.enabled, mX, mY); drawIOSToggle(c2 + 5, y2 += 18, 100, "Venom Timer", VenomTimer.enabled, mX, mY);
                drawIOSButton(c1 + 52, Math.max(y1, y2) + 28, 110, 16, "HUD Customization", mX, mY);
            } else if (selectedTab == 4) {
                drawSettingsCard(c1, y1, 105, 126); fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + "Auto Use", c1 + 5, y1 + 5, -1);
                drawIOSToggle(c1 + 5, y1 += 18, 105, "Pant Swap", AutoPantSwap.pantSwapEnabled, mX, mY); if (isInside(mX, mY, c1 + 5, y1, 75, 12)) currentTooltip = EnumChatFormatting.RED + "\u26A0 " + EnumChatFormatting.YELLOW + "USE AT YOUR OWN RISK." + EnumChatFormatting.GRAY + " - Auto Swap / Hold right-click while holding over pants in your inventory to instantly equip it.";
                drawIOSToggle(c1 + 5, y1 += 18, 105, "Venom Swap", AutoPantSwap.venomSwapEnabled, mX, mY); if (isInside(mX, mY, c1 + 5, y1, 75, 12)) currentTooltip = EnumChatFormatting.RED + "\u26A0 " + EnumChatFormatting.YELLOW + "USE AT YOUR OWN RISK." + EnumChatFormatting.GRAY + " - Automatically swaps to diamond pants if you get venomed.";
                drawIOSToggle(c1 + 5, y1 += 18, 105, "Auto Heal", AutoGhead.enabled, mX, mY); if (isInside(mX, mY, c1 + 5, y1, 75, 12)) currentTooltip = EnumChatFormatting.RED + "\u26A0 " + EnumChatFormatting.YELLOW + "USE AT YOUR OWN RISK." + EnumChatFormatting.GRAY + " - Automatically use Ghead or First Aid Egg.";
                drawIOSToggle(c1 + 5, y1 += 18, 105, "Auto Pod", AutoPantSwap.autoPodEnabled, mX, mY); if (isInside(mX, mY, c1 + 5, y1, 75, 12)) currentTooltip = EnumChatFormatting.RED + "\u26A0 " + EnumChatFormatting.YELLOW + "USE AT YOUR OWN RISK." + EnumChatFormatting.GRAY + " - Automatically use Escape Pods when you are at low health";
                drawIOSToggle(c1 + 5, y1 += 18, 105, "Auto Bullet Time", AutoBulletTime.enabled, mX, mY); if (isInside(mX, mY, c1 + 5, y1, 75, 12)) currentTooltip = EnumChatFormatting.YELLOW + "\u26A0 " + EnumChatFormatting.RED + "USE AT YOUR OWN RISK, POSSIBLE BAN." + EnumChatFormatting.GRAY + " - Automatically switch to Bullet Time when you right click on any sword.";
                drawSettingsCard(c2, y2, 100, 36); fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + "Chat", c2 + 5, y2 + 5, -1); drawIOSToggle(c2 + 5, y2 += 18, 100, "Auto Quick Math", AutoQuickMath.enabled, mX, mY);
            } else if (selectedTab == 5) {
                drawSettingsCard(c1, y1, 215, 40); drawIOSToggle(c1 + 5, y1 + 6, 205, "Enable Telemetry Stats", ConfigHandler.telemetryEnabled, mX, mY); fontRendererObj.drawStringWithShadow(EnumChatFormatting.GRAY + "Help us improve Foxtrot PIT!", c1 + 5, y1 + 22, -1);
                drawSettingsCard(c1, y1 += 45, 215, 125); fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "Privacy & Anonymity", c1 + 5, y1 += 5, -1);
                fontRendererObj.drawString(EnumChatFormatting.GRAY + "All telemetry data is " + EnumChatFormatting.WHITE + "100% Anonymous.", c1 + 5, y1 += 15, -1); fontRendererObj.drawString(EnumChatFormatting.GRAY + "We CANNOT track or collect:", c1 + 5, y1 += 12, -1); fontRendererObj.drawString(EnumChatFormatting.RED + "\u2718 " + EnumChatFormatting.GRAY + "Your Minecraft Name or UUID", c1 + 10, y1 += 10, -1); fontRendererObj.drawString(EnumChatFormatting.RED + "\u2718 " + EnumChatFormatting.GRAY + "Your Session Token or Passwords", c1 + 10, y1 += 10, -1); fontRendererObj.drawString(EnumChatFormatting.RED + "\u2718 " + EnumChatFormatting.GRAY + "Your IP Address or Location", c1 + 10, y1 += 10, -1); fontRendererObj.drawString(EnumChatFormatting.RED + "\u2718 " + EnumChatFormatting.GRAY + "Your Chat Logs or Inventories", c1 + 10, y1 += 10, -1); fontRendererObj.drawString(EnumChatFormatting.GRAY + "We only track active player counts to", c1 + 5, y1 += 15, -1); fontRendererObj.drawString(EnumChatFormatting.GRAY + "display live stats on linex-studios.github.io", c1 + 5, y1 += 10, -1);
            } else if (selectedTab == 6) {
                drawSettingsCard(c1, y1, 215, FoxtrotTweaker.UPDATE_AVAILABLE && !ConfigHandler.autoUpdateEnabled ? 65 : 40); fontRendererObj.drawStringWithShadow(EnumChatFormatting.BOLD + "Mod Updates", c1 + 5, y1 + 5, -1); drawIOSToggle(c1 + 5, y1 + 20, 205, "Auto Updates", ConfigHandler.autoUpdateEnabled, mX, mY);
                if (!ConfigHandler.autoUpdateEnabled && FoxtrotTweaker.UPDATE_AVAILABLE) drawIOSButton(c1 + 5, y1 + 40, 205, 16, EnumChatFormatting.GREEN + "Install Update (v" + FoxtrotTweaker.LATEST_VERSION + ")", mX, mY);
            }
            ConfigHandler.saveConfig();
        }
        if (currentTooltip != null) { int sw = fontRendererObj.getStringWidth(currentTooltip), dx = mX + 10, dy = mY - 10; if (dx + sw + 8 > this.width) dx = this.width - sw - 8; RenderUtils.drawRoundedRect(dx, dy, sw + 8, fontRendererObj.FONT_HEIGHT + 8, 3, 0xF9111111); RenderUtils.drawRoundedOutline(dx, dy, sw + 8, fontRendererObj.FONT_HEIGHT + 8, 3, 1, 0x55FFFFFF); fontRendererObj.drawStringWithShadow(currentTooltip, dx + 4, dy + 4, -1); }
    }

    private void updateEditSlider(int s, int mX, int sX) { float p = Math.max(0, Math.min(1, (mX - sX) / 90f)); if (s == 1) AutoClicker.minCps = 1 + (p * 19); if (s == 2) AutoClicker.maxCps = 1 + (p * 19); if (s == 3) AutoClicker.inventoryFillCps = 5 + (p * 15); }
    private void drawSettingsCard(int x, int y, int w, int h) { RenderUtils.drawRoundedRect(x, y, w, h, 3, 0x11FFFFFF); }
    private void drawIOSSlider(float x, float y, String l, float v, float min, float max, float tW) { fontRendererObj.drawStringWithShadow(l, x, y, 0xDDDDDD); String vT = String.format("%.1f", v); fontRendererObj.drawStringWithShadow(vT, x+tW-fontRendererObj.getStringWidth(vT), y, 0xAAAAAA); float tY = y+10; RenderUtils.drawRoundedRect(x, tY, tW, 2, 1, 0xFF444444); float fW = ((v-min)/(max-min))*tW; if(fW>2) RenderUtils.drawRoundedRect(x, tY, fW, 2, 1, 0xFFE53935); float kX = x+fW, kY = tY+1; RenderUtils.drawCircleOutline(kX, kY, 4, 1, 0x88000000); RenderUtils.drawCircle(kX, kY, 3, 0xFFFFFFFF); }
    private void drawIOSToggle(float x, float y, float cW, String l, boolean o, int mX, int mY) { fontRendererObj.drawStringWithShadow(l, x, y+1, 0xDDDDDD); float sX = x+cW-20, sY = y+1; if(o){ RenderUtils.drawNeonGlow(sX, sY, 16, 8, 4, 6, 0x1AFF3333); RenderUtils.drawRoundedRect(sX, sY, 16, 8, 4, 0xFFE53935); } else RenderUtils.drawRoundedRect(sX, sY, 16, 8, 4, isInside(mX, mY, sX, sY, 16, 8) ? 0xFF555555 : 0xFF444444); RenderUtils.drawCircle(o ? sX+12 : sX+4, sY+4, 3, 0xFFFFFFFF); }
    private void drawIOSButton(float x, float y, float w, float h, String t, int mX, int mY) { boolean hov = isInside(mX, mY, x, y, w, h); RenderUtils.drawGradientRoundedRect(x, y, w, h, 3, hov ? 0xFF3D3D3D : 0xFF2F2F2F, hov ? 0xFF2A2A2A : 0xFF1C1C1C); fontRendererObj.drawStringWithShadow(t, x+(w-fontRendererObj.getStringWidth(t))/2, y+(h-8)/2, -1); }
    private boolean isInside(float mX, float mY, float x, float y, float w, float h) { return mX>=x && mX<=x+w && mY>=y && mY<=y+h; }

    @Override protected void mouseClicked(int mX, int mY, int b) throws IOException {
        if(b!=0 && b!=2) return; 
        
        if (b == 0) {
            if(panelCollapsed) { 
                if(isInside(mX, mY, collapsedX+95, collapsedY, 20, 18)) { panelCollapsed=false; return; } 
                if(isInside(mX, mY, collapsedX, collapsedY, 95, 18)) { draggingPanel=true; lastX=mX; lastY=mY; return; } 
            } else {
                if(isInside(mX, mY, mainPanelX+panelW-25, mainPanelY, 25, 20)) { panelCollapsed=true; return; }
                for(int i=0, y=mainPanelY+25; i<tabs.length; i++, y+=20) if(isInside(mX, mY, mainPanelX+6, y, 70, 16)) { selectedTab=i; return; }
                int c1=mainPanelX+90, c2=mainPanelX+205, y1=mainPanelY+25, y2=y1;
                
                if(selectedTab==0) {
                    if(AutoClicker.limitItems && whitelistField!=null) whitelistField.mouseClicked(mX, mY, b);
                    if(isInside(mX, mY, c1+5, y1+=16, 100, 12)) AutoClicker.enabled=!AutoClicker.enabled; if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) AutoClicker.leftClick=!AutoClicker.leftClick; if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) AutoClicker.holdToClick=!AutoClicker.holdToClick; if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) AutoClicker.fastPlaceEnabled=!AutoClicker.fastPlaceEnabled; if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) AutoClicker.breakBlocks=!AutoClicker.breakBlocks; if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) AutoClicker.inventoryFill=!AutoClicker.inventoryFill; if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) AutoClicker.limitItems=!AutoClicker.limitItems;
                    boolean cD=false; if(randomDropdownExpanded){ if(isInside(mX, mY, c2+5, y2+82, 90, 12)){ AutoClicker.randomMode=0; randomDropdownExpanded=false; cD=true; } else if(isInside(mX, mY, c2+5, y2+96, 90, 12)){ AutoClicker.randomMode=1; randomDropdownExpanded=false; cD=true; } else if(isInside(mX, mY, c2+5, y2+110, 90, 12)){ AutoClicker.randomMode=2; randomDropdownExpanded=false; cD=true; } }
                    if(!cD){ if(isInside(mX, mY, c2+5, y2+66, 90, 14)) randomDropdownExpanded=!randomDropdownExpanded; else if(!randomDropdownExpanded){ if(isInside(mX, mY, c2+5, y2+5, 90, 15)){ activeSlider=1; updateEditSlider(1, mX, c2+5); } else if(isInside(mX, mY, c2+5, y2+25, 90, 15)){ activeSlider=2; updateEditSlider(2, mX, c2+5); } else if(isInside(mX, mY, c2+5, y2+45, 90, 15)){ activeSlider=3; updateEditSlider(3, mX, c2+5); } } }
                } else if(selectedTab==1) {
                    if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) NameTags.enabled=!NameTags.enabled; if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) NameTags.showHealth=!NameTags.showHealth; if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) NameTags.showItems=!NameTags.showItems;
                    if(isInside(mX, mY, c1+5, y1+=36, 100, 12)) EnemyESP.enabled=!EnemyESP.enabled; if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) FriendsESP.enabled=!FriendsESP.enabled;
                    if(isInside(mX, mY, c2+5, y2+=18, 100, 12)) PitESP.espChests=!PitESP.espChests; if(isInside(mX, mY, c2+5, y2+=18, 100, 12)) PitESP.espDragonEggs=!PitESP.espDragonEggs; if(isInside(mX, mY, c2+5, y2+=18, 100, 12)) PitESP.espRaffleTickets=!PitESP.espRaffleTickets; if(isInside(mX, mY, c2+5, y2+=18, 100, 12)) PitESP.espMystics=!PitESP.espMystics; if(isInside(mX, mY, c2+5, y2+=18, 100, 12)) LowLifeMystic.enabled=!LowLifeMystic.enabled;
                } else if(selectedTab==2) { if(isInside(mX, mY, c1+5, y1+6, 100, 12)) AutoDenick.enabled=!AutoDenick.enabled; if(isInside(mX, mY, c1+5, y1+24, 100, 12)) NickedRender.enabled=!NickedRender.enabled;
                } else if(selectedTab==3) {
                    if(isInside(mX, mY, c1+5, y1+=6, 100, 12)) EnemyHUD.enabled=!EnemyHUD.enabled; if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) NickedHUD.enabled=!NickedHUD.enabled; if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) FriendsHUD.enabled=!FriendsHUD.enabled; if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) SessionStatsHUD.enabled=!SessionStatsHUD.enabled; if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) EventHUD.enabled=!EventHUD.enabled; if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) BossBarModule.enabled=!BossBarModule.enabled; if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) CPSModule.enabled=!CPSModule.enabled; if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) FPSModule.enabled=!FPSModule.enabled;
                    if(isInside(mX, mY, c2+5, y2+=6, 100, 12)) RegHUD.enabled=!RegHUD.enabled; if(isInside(mX, mY, c2+5, y2+=18, 100, 12)) DarksHUD.enabled=!DarksHUD.enabled; if(isInside(mX, mY, c2+5, y2+=18, 100, 12)) PotionHUD.enabled=!PotionHUD.enabled; if(isInside(mX, mY, c2+5, y2+=18, 100, 12)) ArmorHUD.enabled=!ArmorHUD.enabled; if(isInside(mX, mY, c2+5, y2+=18, 100, 12)) CoordsHUD.enabled=!CoordsHUD.enabled; if(isInside(mX, mY, c2+5, y2+=18, 100, 12)) TelebowHUD.enabled=!TelebowHUD.enabled; if(isInside(mX, mY, c2+5, y2+=18, 100, 12)) PlayerCounterHUD.enabled=!PlayerCounterHUD.enabled; if(isInside(mX, mY, c2+5, y2+=18, 100, 12)) VenomTimer.enabled=!VenomTimer.enabled;
                    if(isInside(mX, mY, c1+52, Math.max(y1, y2)+28, 110, 16)) mc.displayGuiScreen(new HUDSettingsGui(this));
                } else if(selectedTab==4) {
                    if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) AutoPantSwap.pantSwapEnabled=!AutoPantSwap.pantSwapEnabled; if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) AutoPantSwap.venomSwapEnabled=!AutoPantSwap.venomSwapEnabled; if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) AutoGhead.enabled=!AutoGhead.enabled; if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) AutoPantSwap.autoPodEnabled=!AutoPantSwap.autoPodEnabled; if(isInside(mX, mY, c1+5, y1+=18, 100, 12)) AutoBulletTime.enabled=!AutoBulletTime.enabled;
                    if(isInside(mX, mY, c2+5, y2+=18, 100, 12)) AutoQuickMath.enabled=!AutoQuickMath.enabled;
                } else if(selectedTab==5) { if(isInside(mX, mY, c1+5, y1+6, 205, 12)) { ConfigHandler.telemetryEnabled=!ConfigHandler.telemetryEnabled; if(ConfigHandler.telemetryEnabled) TelemetryManager.initialize(); }
                } else if(selectedTab==6) { if(isInside(mX, mY, c1+5, y1+20, 205, 12)) ConfigHandler.autoUpdateEnabled=!ConfigHandler.autoUpdateEnabled; if(!ConfigHandler.autoUpdateEnabled && FoxtrotTweaker.UPDATE_AVAILABLE && isInside(mX, mY, c1+5, y1+40, 205, 16)) FoxtrotTweaker.triggerManualUpdate(); }
                ConfigHandler.saveConfig(); 
            }
        }
        
        super.mouseClicked(mX, mY, b);
        
        if (!panelCollapsed && isInside(mX, mY, mainPanelX, mainPanelY, panelW, panelH)) return; 
        
        for(DraggableHUD h : DraggableHUD.getRegistry()) {
            if(!h.isEnabled()) continue;
            if(b==2 && h.isHovered(mX, mY)) { h.scale=1; ConfigHandler.saveConfig(); return; }
            if(b==0 && h.isHovered(mX, mY)) {
                long now = System.currentTimeMillis(); if(h==lastClickedHUD && (now-lastClickTime<300)){ mc.displayGuiScreen(new HUDSettingsGui(this, getIdx(h.name))); return; }
                lastClickTime=now; lastClickedHUD=h; int cr=h.getHoveredCorner(mX, mY);
                if(cr!=0){ resizingModule=h; resizingCorner=cr; lastX=mX; lastY=mY; return; }
                draggingModule=h; dragOffsetX=mX-h.x; dragOffsetY=mY-h.y; return;
            }
        }
    }

    private int getIdx(String n) { String l = n.toLowerCase(); return l.contains("potion")?0 : l.contains("armor")?1 : l.contains("coord")?2 : l.contains("enemy")?3 : l.contains("nick")?4 : l.contains("friend")?5 : l.contains("session")?6 : l.contains("event")?7 : l.contains("reg")?8 : l.contains("dark")?9 : l.contains("sprint")?10 : l.contains("cps")?11 : l.contains("fps")?12 : l.contains("boss")?13 : l.contains("telebow")?14 : l.contains("counter")?15 : l.contains("venom")?16 : 0; }

    @Override protected void mouseClickMove(int mX, int mY, int b, long t) {
        int dX=mX-lastX, dY=mY-lastY;
        if(draggingPanel && panelCollapsed){ collapsedX=Math.max(0, Math.min(width-115, collapsedX+dX)); collapsedY=Math.max(0, Math.min(height-18, collapsedY+dY)); }
        else if(activeSlider!=0 && !panelCollapsed) updateEditSlider(activeSlider, mX, mainPanelX+210);
        else if(resizingModule!=null) resizingModule.handleResize(dX, dY, resizingCorner);
        else if(draggingModule!=null) {
            int tX=mX-dragOffsetX, tY=mY-dragOffsetY, sW=(int)(draggingModule.width*draggingModule.scale), sH=(int)(draggingModule.height*draggingModule.scale), mCx=tX+sW/2, mCy=tY+sH/2, cX=width/2, cY=height/2;
            isSnappedX=Math.abs(mCx-cX)<=6; draggingModule.x = isSnappedX ? cX-sW/2 : Math.max(0, Math.min(width-sW, tX));
            isSnappedY=Math.abs(mCy-cY)<=6; draggingModule.y = isSnappedY ? cY-sH/2 : Math.max(0, Math.min(height-sH, tY));
        } lastX=mX; lastY=mY;
    }

    @Override protected void mouseReleased(int mX, int mY, int s) { draggingPanel=false; activeSlider=0; draggingModule=null; resizingModule=null; resizingCorner=0; isSnappedX=isSnappedY=false; ConfigHandler.saveConfig(); }
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); ConfigHandler.saveConfig(); }
    @Override public boolean doesGuiPauseGame() { return false; }
}