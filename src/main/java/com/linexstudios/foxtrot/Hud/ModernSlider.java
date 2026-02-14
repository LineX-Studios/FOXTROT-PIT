package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import org.lwjgl.input.Mouse;

public class ModernSlider extends GuiButton {
    public float sliderValue;
    private final float min, max;
    public boolean dragging;
    private final String prefix;

    public ModernSlider(int id, int x, int y, int width, int height, String prefix, float value, float min, float max) {
        super(id, x, y, width, height, "");
        this.prefix = prefix;
        this.min = min;
        this.max = max;
        this.sliderValue = (value - min) / (max - min); 
        this.updateDisplayString();
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;
        this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

        // Smoothly update the slider while the mouse is held down
        if (this.dragging) {
            if (!Mouse.isButtonDown(0)) {
                this.dragging = false; // Stop dragging if they let go of left click
            } else {
                this.sliderValue = (float)(mouseX - (this.xPosition + 4)) / (float)(this.width - 8);
                if (this.sliderValue < 0.0F) this.sliderValue = 0.0F;
                if (this.sliderValue > 1.0F) this.sliderValue = 1.0F;
                updateDisplayString();
            }
        }

        int backgroundColor = this.hovered ? 0xFF2A2A2A : 0xFF1C1C1C;
        Gui.drawRect(this.xPosition - 1, this.yPosition - 1, this.xPosition + this.width + 1, this.yPosition + this.height + 1, 0xFF101010);
        Gui.drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, backgroundColor);

        int sliderWidth = (int)(this.sliderValue * (this.width - 8));
        Gui.drawRect(this.xPosition + 4, this.yPosition + this.height - 4, this.xPosition + 4 + sliderWidth, this.yPosition + this.height - 2, 0xFFFFFFFF);
        Gui.drawRect(this.xPosition + 4 + sliderWidth - 2, this.yPosition + this.height - 6, this.xPosition + 4 + sliderWidth + 2, this.yPosition + this.height, 0xFFAAAAAA);

        mc.fontRendererObj.drawStringWithShadow(this.displayString, this.xPosition + 6, this.yPosition + 3, 0xFFFFFFFF);
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (super.mousePressed(mc, mouseX, mouseY)) {
            this.sliderValue = (float)(mouseX - (this.xPosition + 4)) / (float)(this.width - 8);
            if (this.sliderValue < 0.0F) this.sliderValue = 0.0F;
            if (this.sliderValue > 1.0F) this.sliderValue = 1.0F;
            this.dragging = true;
            updateDisplayString();
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        this.dragging = false;
    }

    private void updateDisplayString() {
        float actualValue = min + (sliderValue * (max - min));
        this.displayString = prefix + ": " + String.format("%.1f", actualValue);
    }

    public float getValue() {
        return min + (sliderValue * (max - min));
    }
}