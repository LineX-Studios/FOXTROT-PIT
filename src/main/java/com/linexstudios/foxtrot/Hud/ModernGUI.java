package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

public class ModernGUI extends GuiButton {

    public ModernGUI(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            // Check if the user's mouse is hovering over the button
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

            int backgroundColor = this.hovered ? 0xFF2A2A2A : 0xFF1C1C1C; 
            int outlineColor = 0xFF101010; 
            int textColor = 0xFFFFFFFF; 

            // Draw Outline
            Gui.drawRect(this.xPosition - 1, this.yPosition - 1, this.xPosition + this.width + 1, this.yPosition + this.height + 1, outlineColor);
            
            // Draw Main Button Background
            Gui.drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, backgroundColor);

            mc.fontRendererObj.drawStringWithShadow(this.displayString, this.xPosition + 6, this.yPosition + (this.height - 8) / 2.0F, textColor);
        }
    }
}