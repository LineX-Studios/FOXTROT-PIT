package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.gui.GuiScreen;
import java.io.IOException;

public class EditHUDGui extends GuiScreen {
    private boolean draggingEnemy = false;
    private boolean draggingNicked = false;
    private int lastX, lastY;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Natively creates that darkened gray background from your screenshot
        this.drawDefaultBackground(); 

        // Force the HUDs to render in "Edit Mode" so you can drag them even if empty
        if (EnemyHUD.enabled) EnemyHUD.instance.render(true);
        if (NickedHUD.enabled) NickedHUD.instance.render(true);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) { // Left Click
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
        // Smoothly move the HUDs based on mouse movement
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
    public void onGuiClosed() {
        // Automatically turns off drag mode when you press ESC to save & exit
        HUDController.dragMode = false;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false; // Keeps the world active in the background
    }
}