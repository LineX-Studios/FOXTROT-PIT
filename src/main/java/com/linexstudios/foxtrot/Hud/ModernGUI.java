package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

public class ModernGUI extends GuiButton {

    public ModernGUI(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
    }

    /**
     * Completely disables the vanilla "click" sound.
     * This ensures the menu remains silent as requested.
     */
    @Override
    public void playPressSound(SoundHandler soundHandlerIn) {
        // Leave empty for silence
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            // Calculate hover state
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && 
                           mouseX < this.xPosition + this.width && 
                           mouseY < this.yPosition + this.height;

            // --- High Opacity Colors ---
            // Background is slightly lighter when hovered for better visibility
            int backgroundColor = this.hovered ? 0xFF2A2A2A : 0xFF1C1C1C; 
            int outlineColor = 0xFF101010; // Solid dark outline
            int textColor = 0xFFFFFFFF;    // Solid white (full opacity)

            // 1. Draw solid outline (1 pixel border)
            Gui.drawRect(this.xPosition - 1, this.yPosition - 1, 
                         this.xPosition + this.width + 1, this.yPosition + this.height + 1, 
                         outlineColor);
            
            // 2. Draw solid button body
            Gui.drawRect(this.xPosition, this.yPosition, 
                         this.xPosition + this.width, this.yPosition + this.height, 
                         backgroundColor);

            /**
             * Using drawStringWithShadow ensures the text stays visible even on bright maps.
             */
            mc.fontRendererObj.drawStringWithShadow(this.displayString, 
                                                    this.xPosition + 6, 
                                                    this.yPosition + (this.height - 8) / 2.0F, 
                                                    textColor);
        }
    }
}