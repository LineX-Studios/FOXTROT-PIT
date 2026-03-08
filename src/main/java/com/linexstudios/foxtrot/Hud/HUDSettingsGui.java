package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Handler.ConfigHandler;
import com.linexstudios.foxtrot.Render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import java.awt.Color;
import java.io.IOException;

public class HUDSettingsGui extends GuiScreen {
    private final GuiScreen previousScreen;
    private String[] modules = {"Potion Status", "Armor Status", "Coordinates", "Enemy List", "Nicked List", "Friend List", "Session Stats", "Event Tracker", "Regularity List", "Darks List", "Toggle Sprint", "CPS", "FPS", "Boss Bar", "Telebow Timer", "Player Counter"};
    private boolean inSettingsMenu = false, draggingSlider = false, draggingFlySlider = false, draggingColorBox = false, draggingHueBar = false;
    private int selectedModule = -1, maxScroll = 0, activeCustomColorTarget = -1, pickerX = 0, pickerY = 0;
    private float scrollY = 0, targetScrollY = 0, currentHue = 0f, currentSat = 1f, currentBri = 1f;
    private int[] palette = {0xFFFFFF, 0xAAAAAA, 0x555555, 0xFF5555, 0x55FF55, 0x5555FF, 0xFFFF55, 0x55FFFF, 0xFFAA00, 0xFF55FF, 0x000000};
    private final int COLOR_ENABLED = 0xFF28A061, COLOR_DISABLED = 0xFFB82C35, COLOR_TEXT_SECONDARY = 0xFFAAAAAA, COLOR_SEPARATOR = 0x44FFFFFF, COLOR_CARD_BG = 0x44000000, COLOR_CARD_BG_HOVER = 0x66000000, COLOR_BTN_HOVER_OVERLAY = 0x22FFFFFF;

    public HUDSettingsGui(GuiScreen prev) { this.previousScreen = prev; }
    public HUDSettingsGui(GuiScreen prev, int def) { this.previousScreen = prev; this.selectedModule = def; this.inSettingsMenu = true; }

    @Override public void initGui() { super.initGui(); Keyboard.enableRepeatEvents(true); }

    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput(); int dW = Mouse.getEventDWheel();
        if (dW != 0 && !inSettingsMenu) { if (dW > 0) targetScrollY -= 45; if (dW < 0) targetScrollY += 45; }
    }

    @Override public void drawScreen(int mX, int mY, float pT) {
        RenderUtils.drawSolidRect(0, 0, this.width, this.height, 0x99000000);
        GlStateManager.pushMatrix(); GlStateManager.enableTexture2D(); GlStateManager.enableAlpha(); GlStateManager.enableBlend(); GlStateManager.color(1, 1, 1, 1);
        DraggableHUD[] huds = {PotionHUD.instance, ArmorHUD.instance, CoordsHUD.instance, EnemyHUD.instance, NickedHUD.instance, FriendsHUD.instance, SessionStatsHUD.instance, EventHUD.instance, RegHUD.instance, DarksHUD.instance, ToggleSprintModule.instance, CPSModule.instance, FPSModule.instance, BossBarModule.instance, TelebowHUD.instance, PlayerCounterHUD.instance};
        for(int i=0; i<huds.length; i++) if(isModuleEnabled(i)) huds[i].render(true, mX, mY);
        GlStateManager.popMatrix(); GlStateManager.disableLighting(); GlStateManager.enableBlend();

        int pW = 400, pH = 300, pX = (this.width - pW) / 2, pY = (this.height - pH) / 2;
        RenderUtils.drawGradientRoundedRect(pX, pY, pW, pH, 6, 0xCC1E1E1E, 0xCC141414); RenderUtils.drawRoundedOutline(pX, pY, pW, pH, 6, 1.0f, 0x44FF0000);

        if (!inSettingsMenu) {
            fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + "HUD Customization", pX + 15, pY + 14, -1);
            drawCross(pX + pW - 17, pY + 17, 3.5f, 1.5f, isInside(mX, mY, pX + pW - 25, pY + 10, 15, 15) ? COLOR_DISABLED : COLOR_TEXT_SECONDARY);
            RenderUtils.drawSolidRect(pX + 15, pY + 30, pW - 30, 1, 0x1AFFFFFF);
            
            int cW = 115, cH = 110, sX = pX + (pW - (3 * cW + 24)) / 2, sY = pY + 40, vH = pH - 50;
            maxScroll = Math.max(0, (((modules.length + 2) / 3) * (cH + 12)) - vH + 15);
            targetScrollY = Math.max(0, Math.min(maxScroll, targetScrollY)); scrollY += (targetScrollY - scrollY) * 0.2f;

            doGlScissor(pX, sY, pW, vH); GL11.glEnable(GL11.GL_SCISSOR_TEST);
            for (int i = 0; i < modules.length; i++) {
                int cx = sX + (i % 3) * (cW + 12), cy = sY + (i / 3) * (cH + 12) - (int)scrollY, btnX = cx + 6, bW = cW - 12;
                if (cy + cH < sY || cy > sY + vH) continue;
                boolean en = isModuleEnabled(i), hC = isInside(mX, mY, cx, cy, cW, cH) && mY >= sY && mY <= sY + vH, hO = isInside(mX, mY, btnX, cy+62, bW, 20) && mY >= sY && mY <= sY + vH, hT = isInside(mX, mY, btnX, cy+86, bW, 20) && mY >= sY && mY <= sY + vH;
                
                RenderUtils.drawRoundedRect(cx, cy, cW, cH, 4, hC ? COLOR_CARD_BG_HOVER : COLOR_CARD_BG); RenderUtils.drawRoundedOutline(cx, cy, cW, cH, 4, 1f, 0x22FFFFFF);
                fontRendererObj.drawStringWithShadow(modules[i], cx + (cW - fontRendererObj.getStringWidth(modules[i])) / 2f, cy + 27, en ? -1 : COLOR_TEXT_SECONDARY);
                drawInnerRoundedRect(btnX, cy+62, bW, 20, 3, hO ? 0x55FFFFFF : 0x33FFFFFF, hO);
                fontRendererObj.drawStringWithShadow("OPTIONS", btnX + (bW - fontRendererObj.getStringWidth("OPTIONS")) / 2f, cy+62 + 6, -1);
                RenderUtils.drawSolidRect(cx + cW - 28, cy + 66, 1, 12, COLOR_SEPARATOR); drawMathGear(cx + cW - 17, cy + 72, 4.5f, 0xFFFFFFFF, 0f);
                drawInnerRoundedRect(btnX, cy+86, bW, 20, 3, en ? COLOR_ENABLED : COLOR_DISABLED, hT);
                String tT = en ? "ENABLED" : "DISABLED"; fontRendererObj.drawStringWithShadow(tT, btnX + (bW - fontRendererObj.getStringWidth(tT)) / 2f, cy+86 + 6, -1);
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            if (maxScroll > 0) RenderUtils.drawRoundedRect(pX + pW - 8, sY + (scrollY / maxScroll) * (vH - Math.max(20, vH * (vH / (float)(((modules.length + 2) / 3) * (cH + 12))))), 4, Math.max(20, vH * (vH / (float)(((modules.length + 2) / 3) * (cH + 12)))), 2, 0x55FFFFFF);
        } else {
            drawInnerRoundedRect(pX + 10, pY + 10, 60, 18, 3, 0x33000000, isInside(mX, mY, pX + 10, pY + 10, 60, 18));
            fontRendererObj.drawStringWithShadow("< Back", pX + 22, pY + 15, -1); fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + modules[selectedModule] + " Settings", pX + 85, pY + 15, -1);
            RenderUtils.drawSolidRect(pX + 15, pY + 35, pW - 30, 1, 0x1AFFFFFF);
            int rX = pX + 20, rY = pY + 50;

            drawSettingsCard(rX, rY, 360, 40); fontRendererObj.drawStringWithShadow("HUD Scale", rX + 10, rY + 16, 0xDDDDDD);
            drawIOSSlider(rX + 80, rY + 14, getScaleForTab(selectedModule), 0.5f, 1.5f, 210); drawSettingsButton(rX + 300, rY + 12, 50, 16, "Reset", mX, mY); rY += 50;

            switch(selectedModule) {
                case 0: drawSettingsCard(rX, rY, 360, 85); fontRendererObj.drawStringWithShadow("Name Color", rX+10, rY+10, 0xDDDDDD); drawPalette(rX+10, rY+23, PotionHUD.nameColor, mX, mY, 1); fontRendererObj.drawStringWithShadow("Duration Color", rX+10, rY+50, 0xDDDDDD); drawPalette(rX+10, rY+63, PotionHUD.durationColor, mX, mY, 2); break;
                case 1: drawSettingsCard(rX, rY, 360, 75); drawIOSToggle(rX+10, rY+10, 340, "Horizontal Layout", ArmorHUD.instance.isHorizontal, mX, mY); RenderUtils.drawSolidRect(rX+10, rY+32, 340, 1, 0x11FFFFFF); fontRendererObj.drawStringWithShadow("Durability Color", rX+10, rY+42, 0xDDDDDD); drawPalette(rX+10, rY+55, ArmorHUD.durabilityColor, mX, mY, 3); break;
                case 2: drawSettingsCard(rX, rY, 360, 135); drawIOSToggle(rX+10, rY+10, 340, "Horizontal Layout", CoordsHUD.instance.isHorizontal, mX, mY); RenderUtils.drawSolidRect(rX+10, rY+30, 340, 1, 0x11FFFFFF); fontRendererObj.drawStringWithShadow("Axis Color", rX+10, rY+40, 0xDDDDDD); drawPalette(rX+10, rY+52, CoordsHUD.axisColor, mX, mY, 4); fontRendererObj.drawStringWithShadow("Value Color", rX+10, rY+72, 0xDDDDDD); drawPalette(rX+10, rY+84, CoordsHUD.numberColor, mX, mY, 5); fontRendererObj.drawStringWithShadow("Direction Color", rX+10, rY+104, 0xDDDDDD); drawPalette(rX+10, rY+116, CoordsHUD.directionColor, mX, mY, 6); break;
                case 8: drawSettingsCard(rX, rY, 360, 30); fontRendererObj.drawStringWithShadow(EnumChatFormatting.GRAY+"Use scale to resize Reg List.", rX+10, rY+11, -1); break;
                case 9: drawSettingsCard(rX, rY, 360, 30); fontRendererObj.drawStringWithShadow(EnumChatFormatting.GRAY+"Use scale to resize Darks List.", rX+10, rY+11, -1); break;
                case 10: drawSettingsCard(rX, rY, 360, 160); drawIOSToggle(rX+10, rY+10, 340, "Enable Module", ToggleSprintModule.instance.enabled, mX, mY); drawIOSToggle(rX+10, rY+30, 340, "Toggle Sprint", ToggleSprintModule.instance.toggleSprint, mX, mY); drawIOSToggle(rX+10, rY+50, 340, "Toggle Sneak", ToggleSprintModule.instance.toggleSneak, mX, mY); drawIOSToggle(rX+10, rY+70, 340, "W-Tap", ToggleSprintModule.instance.wTapFix, mX, mY); drawIOSToggle(rX+10, rY+90, 340, "Fly Boost", ToggleSprintModule.instance.flyBoost, mX, mY); RenderUtils.drawSolidRect(rX+10, rY+110, 340, 1, 0x11FFFFFF); fontRendererObj.drawStringWithShadow("Fly Boost", rX+10, rY+118, 0xDDDDDD); drawIOSSlider(rX+80, rY+116, ToggleSprintModule.instance.flyBoostAmount, 1, 10, 250); fontRendererObj.drawStringWithShadow("Text Color", rX+10, rY+140, 0xDDDDDD); drawPalette(rX+80, rY+135, ToggleSprintModule.instance.textColor, mX, mY, 7); break;
                case 11: drawSettingsCard(rX, rY, 360, 70); drawIOSToggle(rX+10, rY+8, 340, "Show Background", CPSModule.showBackground, mX, mY); RenderUtils.drawSolidRect(rX+10, rY+28, 340, 1, 0x11FFFFFF); fontRendererObj.drawStringWithShadow("Text Color", rX+10, rY+36, 0xDDDDDD); drawPalette(rX+10, rY+49, CPSModule.textColor, mX, mY, 8); break;
                case 12: drawSettingsCard(rX, rY, 360, 70); drawIOSToggle(rX+10, rY+8, 340, "Show Background", FPSModule.showBackground, mX, mY); RenderUtils.drawSolidRect(rX+10, rY+28, 340, 1, 0x11FFFFFF); fontRendererObj.drawStringWithShadow("Text Color", rX+10, rY+36, 0xDDDDDD); drawPalette(rX+10, rY+49, FPSModule.textColor, mX, mY, 10); break;
                case 13: drawSettingsCard(rX, rY, 360, 30); fontRendererObj.drawStringWithShadow(EnumChatFormatting.GRAY+"Use scale to resize Boss Bar.", rX+10, rY+11, -1); break;
                case 14: drawSettingsCard(rX, rY, 360, 30); fontRendererObj.drawStringWithShadow(EnumChatFormatting.GRAY+"Use scale to resize Telebow Timer.", rX+10, rY+11, -1); break;
                case 15: drawSettingsCard(rX, rY, 360, 85); fontRendererObj.drawStringWithShadow("Prefix Color", rX+10, rY+10, 0xDDDDDD); drawPalette(rX+10, rY+23, PlayerCounterHUD.prefixColor, mX, mY, 11); fontRendererObj.drawStringWithShadow("Number Color", rX+10, rY+50, 0xDDDDDD); drawPalette(rX+10, rY+63, PlayerCounterHUD.countColor, mX, mY, 12); break;
            }
            if (activeCustomColorTarget != -1) drawColorPickerPopup(pickerX, pickerY, mX, mY);
        } super.drawScreen(mX, mY, pT);
    }

    private boolean isModuleEnabled(int i) { boolean[] st = {PotionHUD.enabled, ArmorHUD.enabled, CoordsHUD.enabled, EnemyHUD.enabled, NickedHUD.enabled, FriendsHUD.enabled, SessionStatsHUD.enabled, EventHUD.enabled, RegHUD.enabled, DarksHUD.enabled, ToggleSprintModule.instance.enabled, CPSModule.enabled, FPSModule.enabled, BossBarModule.enabled, TelebowHUD.enabled, PlayerCounterHUD.enabled}; return i >= 0 && i < st.length ? st[i] : false; }
    private void toggleModule(int i) { if(i==0)PotionHUD.enabled=!PotionHUD.enabled; else if(i==1)ArmorHUD.enabled=!ArmorHUD.enabled; else if(i==2)CoordsHUD.enabled=!CoordsHUD.enabled; else if(i==3)EnemyHUD.enabled=!EnemyHUD.enabled; else if(i==4)NickedHUD.enabled=!NickedHUD.enabled; else if(i==5)FriendsHUD.enabled=!FriendsHUD.enabled; else if(i==6)SessionStatsHUD.enabled=!SessionStatsHUD.enabled; else if(i==7)EventHUD.enabled=!EventHUD.enabled; else if(i==8)RegHUD.enabled=!RegHUD.enabled; else if(i==9)DarksHUD.enabled=!DarksHUD.enabled; else if(i==10)ToggleSprintModule.instance.enabled=!ToggleSprintModule.instance.enabled; else if(i==11)CPSModule.enabled=!CPSModule.enabled; else if(i==12)FPSModule.enabled=!FPSModule.enabled; else if(i==13)BossBarModule.enabled=!BossBarModule.enabled; else if(i==14)TelebowHUD.enabled=!TelebowHUD.enabled; else if(i==15)PlayerCounterHUD.enabled=!PlayerCounterHUD.enabled; }
    private void assignScale(float v) { DraggableHUD[] h = {PotionHUD.instance, ArmorHUD.instance, CoordsHUD.instance, EnemyHUD.instance, NickedHUD.instance, FriendsHUD.instance, SessionStatsHUD.instance, EventHUD.instance, RegHUD.instance, DarksHUD.instance, ToggleSprintModule.instance, CPSModule.instance, FPSModule.instance, BossBarModule.instance, TelebowHUD.instance, PlayerCounterHUD.instance}; if(selectedModule>=0 && selectedModule<h.length) h[selectedModule].scale = v; }
    private float getScaleForTab(int t) { DraggableHUD[] h = {PotionHUD.instance, ArmorHUD.instance, CoordsHUD.instance, EnemyHUD.instance, NickedHUD.instance, FriendsHUD.instance, SessionStatsHUD.instance, EventHUD.instance, RegHUD.instance, DarksHUD.instance, ToggleSprintModule.instance, CPSModule.instance, FPSModule.instance, BossBarModule.instance, TelebowHUD.instance, PlayerCounterHUD.instance}; return (t>=0 && t<h.length) ? h[t].scale : 1f; }

    @Override protected void mouseClicked(int mX, int mY, int b) throws IOException {
        if(b!=0) return; int pW=400, pH=300, pX=(this.width-pW)/2, pY=(this.height-pH)/2;
        if (!inSettingsMenu) {
            if(isInside(mX, mY, pX+pW-25, pY+10, 15, 15)) { ConfigHandler.saveConfig(); mc.displayGuiScreen(previousScreen); return; }
            for (int i=0, sX=pX+(pW-369)/2, sY=pY+40; i<modules.length; i++) {
                int cx=sX+(i%3)*127, cy=sY+(i/3)*122-(int)scrollY; if(cy+110<sY || cy>sY+pH-50) continue;
                if(isInside(mX, mY, cx+6, cy+86, 103, 20) && mY>=sY && mY<=sY+pH-50) { toggleModule(i); mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1F)); return; }
                if((isInside(mX, mY, cx+6, cy+62, 103, 20) || isInside(mX, mY, cx, cy, 115, 110)) && mY>=sY && mY<=sY+pH-50) { selectedModule=i; inSettingsMenu=true; mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1F)); return; }
            }
        } else {
            if(activeCustomColorTarget!=-1) { if(isInside(mX, mY, pickerX, pickerY, 100, 100)) { draggingColorBox=true; return; } if(isInside(mX, mY, pickerX+110, pickerY, 10, 100)) { draggingHueBar=true; return; } activeCustomColorTarget=-1; }
            if(isInside(mX, mY, pX+10, pY+10, 60, 18)) { inSettingsMenu=false; mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1F)); return; }
            int rX=pX+20, rY=pY+50; if(isInside(mX, mY, rX+300, rY+12, 50, 16)) { assignScale(1f); return; } if(isInside(mX, mY, rX+80, rY+14, 210, 15)) { draggingSlider=true; assignScale(0.5f+(Math.max(0, Math.min(1, (mX-(rX+80))/210f)))); return; }
            rY+=50;
            switch(selectedModule) {
                case 0: for(int i=0;i<palette.length;i++) { if(isInside(mX,mY,rX+10+(i*22),rY+23,12,12)){if(i==10)openCustomColorPicker(1,rX+10+(i*22),rY+23,PotionHUD.nameColor);else PotionHUD.nameColor=palette[i];return;} if(isInside(mX,mY,rX+10+(i*22),rY+63,12,12)){if(i==10)openCustomColorPicker(2,rX+10+(i*22),rY+63,PotionHUD.durationColor);else PotionHUD.durationColor=palette[i];return;} } break;
                case 1: if(isInside(mX,mY,rX+326,rY+10,24,12)) ArmorHUD.instance.isHorizontal=!ArmorHUD.instance.isHorizontal; for(int i=0;i<11;i++) if(isInside(mX,mY,rX+10+(i*22),rY+55,12,12)){if(i==10)openCustomColorPicker(3,rX+10+(i*22),rY+55,ArmorHUD.durabilityColor);else ArmorHUD.durabilityColor=palette[i];return;} break;
                case 2: if(isInside(mX,mY,rX+326,rY+10,24,12)) CoordsHUD.instance.isHorizontal=!CoordsHUD.instance.isHorizontal; for(int i=0;i<11;i++) { if(isInside(mX,mY,rX+10+(i*22),rY+52,12,12)){if(i==10)openCustomColorPicker(4,rX+10+(i*22),rY+52,CoordsHUD.axisColor);else CoordsHUD.axisColor=palette[i];return;} if(isInside(mX,mY,rX+10+(i*22),rY+84,12,12)){if(i==10)openCustomColorPicker(5,rX+10+(i*22),rY+84,CoordsHUD.numberColor);else CoordsHUD.numberColor=palette[i];return;} if(isInside(mX,mY,rX+10+(i*22),rY+116,12,12)){if(i==10)openCustomColorPicker(6,rX+10+(i*22),rY+116,CoordsHUD.directionColor);else CoordsHUD.directionColor=palette[i];return;} } break;
                case 10: if(isInside(mX,mY,rX+326,rY+10,24,12)) ToggleSprintModule.instance.enabled=!ToggleSprintModule.instance.enabled; if(isInside(mX,mY,rX+326,rY+30,24,12)) ToggleSprintModule.instance.toggleSprint=!ToggleSprintModule.instance.toggleSprint; if(isInside(mX,mY,rX+326,rY+50,24,12)) ToggleSprintModule.instance.toggleSneak=!ToggleSprintModule.instance.toggleSneak; if(isInside(mX,mY,rX+326,rY+70,24,12)) ToggleSprintModule.instance.wTapFix=!ToggleSprintModule.instance.wTapFix; if(isInside(mX,mY,rX+326,rY+90,24,12)) ToggleSprintModule.instance.flyBoost=!ToggleSprintModule.instance.flyBoost; if(isInside(mX,mY,rX+80,rY+116,250,15)) { draggingFlySlider=true; ToggleSprintModule.instance.flyBoostAmount=1+(9*Math.max(0,Math.min(1,(mX-(rX+80))/250f))); return; } for(int i=0;i<11;i++) if(isInside(mX,mY,rX+80+(i*22),rY+135,12,12)){if(i==10)openCustomColorPicker(7,rX+80+(i*22),rY+135,ToggleSprintModule.instance.textColor);else ToggleSprintModule.instance.textColor=palette[i];return;} break;
                case 11: if(isInside(mX,mY,rX+326,rY+8,24,12)) CPSModule.showBackground=!CPSModule.showBackground; for(int i=0;i<11;i++) if(isInside(mX,mY,rX+10+(i*22),rY+49,12,12)){if(i==10)openCustomColorPicker(8,rX+10+(i*22),rY+49,CPSModule.textColor);else CPSModule.textColor=palette[i];return;} break;
                case 12: if(isInside(mX,mY,rX+326,rY+8,24,12)) FPSModule.showBackground=!FPSModule.showBackground; for(int i=0;i<11;i++) if(isInside(mX,mY,rX+10+(i*22),rY+49,12,12)){if(i==10)openCustomColorPicker(10,rX+10+(i*22),rY+49,FPSModule.textColor);else FPSModule.textColor=palette[i];return;} break;
                case 15: for(int i=0;i<11;i++){ if(isInside(mX,mY,rX+10+(i*22),rY+23,12,12)){if(i==10)openCustomColorPicker(11,rX+10+(i*22),rY+23,PlayerCounterHUD.prefixColor);else PlayerCounterHUD.prefixColor=palette[i];return;} if(isInside(mX,mY,rX+10+(i*22),rY+63,12,12)){if(i==10)openCustomColorPicker(12,rX+10+(i*22),rY+63,PlayerCounterHUD.countColor);else PlayerCounterHUD.countColor=palette[i];return;} } break;
            }
        }
    }

    @Override protected void mouseClickMove(int mX, int mY, int b, long t) { if (!inSettingsMenu) return; if(draggingColorBox) { currentSat=Math.max(0, Math.min(1, (mX-pickerX)/100f)); currentBri=Math.max(0, Math.min(1, 1f-((mY-pickerY)/100f))); int c=Color.HSBtoRGB(currentHue, currentSat, currentBri)&0xFFFFFF; if(activeCustomColorTarget==1)PotionHUD.nameColor=c; else if(activeCustomColorTarget==2)PotionHUD.durationColor=c; else if(activeCustomColorTarget==3)ArmorHUD.durabilityColor=c; else if(activeCustomColorTarget==4)CoordsHUD.axisColor=c; else if(activeCustomColorTarget==5)CoordsHUD.numberColor=c; else if(activeCustomColorTarget==6)CoordsHUD.directionColor=c; else if(activeCustomColorTarget==7)ToggleSprintModule.instance.textColor=c; else if(activeCustomColorTarget==8)CPSModule.textColor=c; else if(activeCustomColorTarget==10)FPSModule.textColor=c; else if(activeCustomColorTarget==11)PlayerCounterHUD.prefixColor=c; else if(activeCustomColorTarget==12)PlayerCounterHUD.countColor=c; } else if(draggingHueBar) { currentHue=Math.max(0, Math.min(1, (mY-pickerY)/100f)); int c=Color.HSBtoRGB(currentHue, currentSat, currentBri)&0xFFFFFF; if(activeCustomColorTarget==1)PotionHUD.nameColor=c; else if(activeCustomColorTarget==2)PotionHUD.durationColor=c; else if(activeCustomColorTarget==3)ArmorHUD.durabilityColor=c; else if(activeCustomColorTarget==4)CoordsHUD.axisColor=c; else if(activeCustomColorTarget==5)CoordsHUD.numberColor=c; else if(activeCustomColorTarget==6)CoordsHUD.directionColor=c; else if(activeCustomColorTarget==7)ToggleSprintModule.instance.textColor=c; else if(activeCustomColorTarget==8)CPSModule.textColor=c; else if(activeCustomColorTarget==10)FPSModule.textColor=c; else if(activeCustomColorTarget==11)PlayerCounterHUD.prefixColor=c; else if(activeCustomColorTarget==12)PlayerCounterHUD.countColor=c; } else if(draggingSlider) { assignScale(0.5f+(Math.max(0, Math.min(1, (mX-(((this.width-400)/2)+100))/210f)))); } else if(draggingFlySlider) { ToggleSprintModule.instance.flyBoostAmount=1+(9*Math.max(0, Math.min(1, (mX-(((this.width-400)/2)+100))/250f))); } }
    @Override protected void mouseReleased(int mX, int mY, int s) { draggingSlider=false; draggingColorBox=false; draggingHueBar=false; draggingFlySlider=false; ConfigHandler.saveConfig(); }

    private void drawColorPickerPopup(int x, int y, int mX, int mY) {
        RenderUtils.drawRoundedRect(x - 5, y - 5, 135, 110, 4, 0xFA1E1E1E);
        RenderUtils.setupSmoothRender(true);
        GL11.glBegin(GL11.GL_QUADS);
        RenderUtils.setColor(0xFFFFFFFF); GL11.glVertex2f(x, y); 
        RenderUtils.setColor(0xFF000000); GL11.glVertex2f(x, y + 100); 
        RenderUtils.setColor(0xFF000000); GL11.glVertex2f(x + 100, y + 100); 
        RenderUtils.setColor(Color.HSBtoRGB(currentHue, 1.0f, 1.0f) | 0xFF000000); GL11.glVertex2f(x + 100, y); 
        GL11.glEnd();
        RenderUtils.endSmoothRender();
        float dotX = x + (currentSat * 100), dotY = y + ((1f - currentBri) * 100);
        RenderUtils.drawCircle(dotX, dotY, 4.0f, 0xFF000000); RenderUtils.drawCircle(dotX, dotY, 3.0f, 0xFFFFFFFF);
        RenderUtils.setupSmoothRender(true);
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i <= 20; i++) {
            float hueStep = i / 20f;
            RenderUtils.setColor(Color.HSBtoRGB(hueStep, 1.0f, 1.0f) | 0xFF000000);
            GL11.glVertex2f(x + 110, y + (hueStep * 100)); GL11.glVertex2f(x + 120, y + (hueStep * 100));
        }
        GL11.glEnd();
        RenderUtils.endSmoothRender();
        float hueY = y + (currentHue * 100);
        RenderUtils.drawSolidRect(x + 108, hueY - 1, 14, 3, 0xFFFFFFFF); RenderUtils.drawSolidRect(x + 109, hueY, 12, 1, 0xFF000000);
    }

    private void openCustomColorPicker(int id, int x, int y, int c) { activeCustomColorTarget=id; pickerX=x+15; pickerY=y-45; float[] hsb=Color.RGBtoHSB((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF, null); currentHue=hsb[0]; currentSat=hsb[1]; currentBri=hsb[2]; }
    public void drawMathGear(float x, float y, float r, int c, float rt) { GL11.glPushMatrix(); GL11.glTranslatef(x, y, 0); float s=r/3.5f; GL11.glScalef(s, s, 1); GlStateManager.enableTexture2D(); GlStateManager.enableBlend(); fontRendererObj.drawStringWithShadow("\u2630", -fontRendererObj.getStringWidth("\u2630")/2f, -fontRendererObj.FONT_HEIGHT/2f+1, c); GlStateManager.color(1, 1, 1, 1); GL11.glPopMatrix(); }

    private void drawCross(float cX, float cY, float s, float t, int c) {
        RenderUtils.setupSmoothRender(false);
        GL11.glEnable(GL11.GL_LINE_SMOOTH); GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        RenderUtils.setColor(c); GL11.glLineWidth(t);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(cX - s, cY - s); GL11.glVertex2f(cX + s, cY + s);
        GL11.glVertex2f(cX + s, cY - s); GL11.glVertex2f(cX - s, cY + s);
        GL11.glEnd(); 
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderUtils.endSmoothRender();
    }

    private void doGlScissor(int x, int y, int w, int h) { Minecraft mc=Minecraft.getMinecraft(); int sf=1, k=mc.gameSettings.guiScale; if(k==0) k=1000; while(sf<k && mc.displayWidth/(sf+1)>=320 && mc.displayHeight/(sf+1)>=240) ++sf; GL11.glScissor(x*sf, mc.displayHeight-(y+h)*sf, w*sf, h*sf); }
    private void drawInnerRoundedRect(float x, float y, float w, float h, float r, int c, boolean hv) { RenderUtils.drawRoundedRect(x, y, w, h, r, hv?c|COLOR_BTN_HOVER_OVERLAY:c); RenderUtils.drawRoundedOutline(x, y, w, h, r, 1f, 0x55FFFFFF); }
    private void drawIOSToggle(float x, float y, float cW, String l, boolean o, int mX, int mY) { fontRendererObj.drawStringWithShadow(l, x, y+2, 0xDDDDDD); float sX=x+cW-28, sY=y+1; RenderUtils.drawRoundedRect(sX, sY, 24, 12, 6, o?COLOR_ENABLED:(isInside(mX,mY,sX,sY,24,12)?0xFF555555:0xFF444444)); RenderUtils.drawCircle(o?sX+18:sX+6, sY+6, 5, 0xFFFFFFFF); }
    private void drawSettingsButton(float x, float y, float w, float h, String t, int mX, int mY) { drawInnerRoundedRect(x, y, w, h, 3, 0x33FFFFFF, isInside(mX, mY, x, y, w, h)); fontRendererObj.drawStringWithShadow(t, x+(w-fontRendererObj.getStringWidth(t))/2f, y+(h-8)/2f, -1); }
    private void drawSettingsCard(int x, int y, int w, int h) { RenderUtils.drawRoundedRect(x, y, w, h, 4, 0x33000000); }
    private void drawIOSSlider(float x, float y, float v, float mn, float mx, float tW) { String vT=String.format("%.1f", v); fontRendererObj.drawStringWithShadow(vT, x+tW-fontRendererObj.getStringWidth(vT), y-6, 0xAAAAAA); RenderUtils.drawRoundedRect(x, y+4, tW, 4, 2, 0xFF333333); float fW=((v-mn)/(mx-mn))*tW; if(fW>4) RenderUtils.drawRoundedRect(x, y+4, fW, 4, 2, COLOR_DISABLED); RenderUtils.drawCircleOutline(x+fW, y+6, 5, 1, 0x88000000); RenderUtils.drawCircle(x+fW, y+6, 4, 0xFFFFFFFF); }
    private void drawPalette(float x, float y, int cr, int mX, int mY, int tI) { for(int i=0;i<palette.length;i++){ float cX=x+(i*22)+6, cY=y+6; boolean hv=isInside(mX,mY,x+(i*22),y,12,12); if(i==10){ if(activeCustomColorTarget==tI) RenderUtils.drawCircle(cX, cY, 7.5f, COLOR_DISABLED); else if(hv) RenderUtils.drawCircle(cX, cY, 7.5f, 0x55FFFFFF); RenderUtils.drawCircle(cX, cY, 6.5f, 0xFF222222); RenderUtils.drawCircle(cX, cY, 5.5f, 0xFF111111); fontRendererObj.drawStringWithShadow("+", cX-2.5f, cY-3.5f, 0xAAAAAA); } else { if(hv && palette[i]!=cr) RenderUtils.drawCircle(cX, cY, 7.5f, 0x55FFFFFF); if((palette[i]&0xFFFFFF)==(cr&0xFFFFFF)) RenderUtils.drawCircle(cX, cY, 7.5f, 0xFFFFFFFF); RenderUtils.drawCircle(cX, cY, 5.5f, palette[i]|0xFF000000); } } }
    private boolean isInside(float mX, float mY, float x, float y, float w, float h) { return mX>=x && mX<=x+w && mY>=y && mY<=y+h; }
    
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); ConfigHandler.saveConfig(); }
    @Override public boolean doesGuiPauseGame() { return false; }
}