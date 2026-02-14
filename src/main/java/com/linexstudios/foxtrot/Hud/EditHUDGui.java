package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Handler.ConfigHandler;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;
import java.io.IOException;

public class EditHUDGui extends GuiScreen {
    private boolean draggingEnemy = false;
    private boolean draggingNicked = false;
    private int lastX, lastY;

@Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground(); 
        
        String title = EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "Foxtrot HUD Editor";
        this.fontRendererObj.drawStringWithShadow(title, this.width / 2 - 55, 20, 0xFFFFFF);
        
        String subtitle = EnumChatFormatting.GRAY + "Click and drag the HUD Elements. Press ESC to save.";
        this.fontRendererObj.drawStringWithShadow(subtitle, this.width / 2 - 110, 35, 0xFFFFFF);

        if (EnemyHUD.enabled) EnemyHUD.instance.render(true);
        if (NickedHUD.enabled) NickedHUD.instance.render(true);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
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
        if (draggingEnemy) {
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
    }

    // THIS TRIGGERS THE SAVE WHEN YOU HIT ESC
    @Override
    public void onGuiClosed() {
        ConfigHandler.saveConfig();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}