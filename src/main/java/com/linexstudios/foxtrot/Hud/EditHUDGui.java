package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.gui.GuiScreen;
import java.io.IOException;

public class EditHUDGui extends GuiScreen {
    private boolean draggingEnemy = false;
    private boolean draggingNicked = false;
    private int lastX, lastY;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // This natively grays out the background!
        this.drawDefaultBackground(); 

        this.fontRendererObj.drawStringWithShadow("§6§lFoxtrot HUD Editor", this.width / 2 - 55, 20, 0xFFFFFF);
        this.fontRendererObj.drawStringWithShadow("§7Click and drag the boxes. Press ESC to save.", this.width / 2 - 110, 35, 0xFFFFFF);

        // Tell the HUDs to render in "Edit Mode" so you can see their hitboxes
        if (EnemyHUD.enabled) HUDController.enemyHUD.render(true);
        if (NickedHUD.enabled) HUDController.nickedHUD.render(true);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) { // Left Click
            if (EnemyHUD.enabled && HUDController.enemyHUD.isHovered(mouseX, mouseY)) {
                draggingEnemy = true;
                lastX = mouseX; lastY = mouseY;
            } else if (NickedHUD.enabled && HUDController.nickedHUD.isHovered(mouseX, mouseY)) {
                draggingNicked = true;
                lastX = mouseX; lastY = mouseY;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        // Move the HUDs smoothly based on mouse movement
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

    @Override
    public boolean doesGuiPauseGame() {
        return false; // Keeps the world rendering behind the menu
    }
}