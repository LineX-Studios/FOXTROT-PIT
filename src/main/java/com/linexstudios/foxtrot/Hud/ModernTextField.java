package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;

public class ModernTextField extends GuiTextField {

    public ModernTextField(int componentId, int x, int y, int width, int height) {
        super(componentId, Minecraft.getMinecraft().fontRendererObj, x, y, width, height);
        this.setMaxStringLength(100); // Allow plenty of room for items
        this.setEnableBackgroundDrawing(false);
    }

    @Override
    public void drawTextBox() {
        if (this.getVisible()) {
            // Dark Theme - Outline glows slightly when you click it to type
            int outlineColor = this.isFocused() ? 0xFF4A4A4A : 0xFF101010;
            int backgroundColor = 0xFF1C1C1C;

            Gui.drawRect(this.xPosition - 1, this.yPosition - 1, this.xPosition + this.width + 1, this.yPosition + this.height + 1, outlineColor);
            Gui.drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, backgroundColor);

            // Shift text slightly so it doesn't touch the edge
            this.xPosition += 4;
            this.yPosition += (this.height - 8) / 2;
            super.drawTextBox(); // Renders the text and the blinking cursor
            this.yPosition -= (this.height - 8) / 2;
            this.xPosition -= 4;
        }
    }
}