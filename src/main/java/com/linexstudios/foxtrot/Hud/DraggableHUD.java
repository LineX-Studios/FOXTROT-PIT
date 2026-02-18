package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;

public abstract class DraggableHUD {
    public int x, y;
    public int width, height;
    public float scale = 1.0f;
    public boolean isHorizontal = false; 

    public final float MIN_SCALE = 0.5f;
    public final float MAX_SCALE = 1.5f;
    
    public String name = "HUD Element";

    public DraggableHUD(String name, int startX, int startY) {
        this.name = name;
        this.x = startX;
        this.y = startY;
    }

    public abstract void draw(boolean isEditing);

    public void render(boolean isEditing, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY);

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);

        if (isEditing) {
            // Label above the box
            GL11.glPushMatrix();
            GL11.glScalef(0.8f, 0.8f, 1.0f);
            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(name, 0, -11, 0xFFFFFF);
            GL11.glPopMatrix();
        }

        GL11.glScalef(scale, scale, 1);

        if (isEditing) {
            // CheatBreaker White Translucent Background
            int bgColor = hovered ? 0x25FFFFFF : 0x11FFFFFF; 
            Gui.drawRect(0, 0, width, height, bgColor);

            // Ultra-thin white border (opacity 30%)
            int borderColor = 0x4DFFFFFF; 
            drawThinBorder(0, 0, width, height, borderColor);
        }

        draw(isEditing); 

        GL11.glPopMatrix();

        // RED and SMALLER Corner Boxes (Size 2)
        if (isEditing && hovered) {
            int boxSize = 2; // Made smaller
            int actualW = (int)(width * scale);
            int actualH = (int)(height * scale);

            // Red color 0xFFFF0000
            drawCornerBox(x - boxSize, y - boxSize, boxSize); // TL
            drawCornerBox(x + actualW, y - boxSize, boxSize); // TR
            drawCornerBox(x - boxSize, y + actualH, boxSize); // BL
            drawCornerBox(x + actualW, y + actualH, boxSize); // BR
        }
    }

    private void drawThinBorder(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, top + 1, color);
        Gui.drawRect(left, bottom - 1, right, bottom, color);
        Gui.drawRect(left, top, left + 1, bottom, color);
        Gui.drawRect(right - 1, top, right, bottom, color);
    }

    private void drawCornerBox(int boxX, int boxY, int size) {
        Gui.drawRect(boxX, boxY, boxX + size, boxY + size, 0xFFFF0000); // RED
    }

    public boolean isHovered(int mouseX, int mouseY) {
        float actualW = width * scale;
        float actualH = height * scale;
        boolean inBody = mouseX >= x && mouseX <= x + actualW && mouseY >= y && mouseY <= y + actualH;
        return inBody || getHoveredCorner(mouseX, mouseY) != 0;
    }

    public boolean isHoveringCorner(int mouseX, int mouseY, int corner) {
        float actualW = width * scale;
        float actualH = height * scale;
        int hitBox = 5; 

        float cornerX = x;
        float cornerY = y;

        if (corner == 2) cornerX = x + actualW;
        if (corner == 3) cornerY = y + actualH;
        if (corner == 4) { cornerX = x + actualW; cornerY = y + actualH; }

        return mouseX >= cornerX - hitBox && mouseX <= cornerX + hitBox &&
               mouseY >= cornerY - hitBox && mouseY <= cornerY + hitBox;
    }

    public int getHoveredCorner(int mouseX, int mouseY) {
        if (isHoveringCorner(mouseX, mouseY, 1)) return 1;
        if (isHoveringCorner(mouseX, mouseY, 2)) return 2;
        if (isHoveringCorner(mouseX, mouseY, 3)) return 3;
        if (isHoveringCorner(mouseX, mouseY, 4)) return 4;
        return 0;
    }

    public void handleResize(int deltaX, int deltaY, int corner) {
        float sensitivity = 0.005f; 
        float scaleChange = 0;

        if (corner == 1) scaleChange = -(deltaX + deltaY) * sensitivity;
        else if (corner == 2) scaleChange = (deltaX - deltaY) * sensitivity;
        else if (corner == 3) scaleChange = (-deltaX + deltaY) * sensitivity;
        else if (corner == 4) scaleChange = (deltaX + deltaY) * sensitivity;

        this.scale += scaleChange;
        if (this.scale < MIN_SCALE) this.scale = MIN_SCALE;
        if (this.scale > MAX_SCALE) this.scale = MAX_SCALE;
    }
}