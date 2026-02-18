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
            // Label cleanly floating above the box (CheatBreaker Style)
            GL11.glPushMatrix();
            GL11.glScalef(0.8f, 0.8f, 1.0f);
            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(name, 0, -11, 0xFFFFFF);
            GL11.glPopMatrix();
        }

        GL11.glScalef(scale, scale, 1);

        if (isEditing) {
            // EXACT WHITE OPACITY FIX
            // 0x25FFFFFF = Constant low-opacity white background (always visible)
            // 0x45FFFFFF = Brighter white when hovered
            int bgColor = hovered ? 0x45FFFFFF : 0x25FFFFFF; 
            
            // Thin white border color
            int borderColor = 0x55FFFFFF; 
            
            // Draw the smooth CheatBreaker box
            drawCleanBox(0, 0, width, height, bgColor, borderColor);
        }

        draw(isEditing); 

        GL11.glPopMatrix();

        // RED RESIZING CORNERS: Only draws the exact corner your mouse is on
        if (isEditing && hovered) {
            int corner = getHoveredCorner(mouseX, mouseY);
            int boxSize = 2; // Small and elegant
            int actualW = (int)(width * scale);
            int actualH = (int)(height * scale);

            if (corner == 1) drawCornerBox(x - boxSize, y - boxSize, boxSize); // Top-Left
            else if (corner == 2) drawCornerBox(x + actualW, y - boxSize, boxSize); // Top-Right
            else if (corner == 3) drawCornerBox(x - boxSize, y + actualH, boxSize); // Bottom-Left
            else if (corner == 4) drawCornerBox(x + actualW, y + actualH, boxSize); // Bottom-Right
        }
    }

    // --- SMOOTH OPENGL RENDERING ---
    // This replaces the ugly overlapping rectangles with a perfect 1-pixel OpenGL line
    private void drawCleanBox(float x, float y, float w, float h, int bgColor, int borderColor) {
        // Draw the inner white translucent background
        Gui.drawRect((int)x, (int)y, (int)(x + w), (int)(y + h), bgColor);
        
        // Extract ARGB for OpenGL
        float alpha = (float)(borderColor >> 24 & 255) / 255.0F;
        float red = (float)(borderColor >> 16 & 255) / 255.0F;
        float green = (float)(borderColor >> 8 & 255) / 255.0F;
        float blue = (float)(borderColor & 255) / 255.0F;
        
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(red, green, blue, alpha);
        
        // This forces the border to be EXACTLY 1 pixel thick, fixing the "wonky" look entirely.
        GL11.glLineWidth(1.0F); 
        
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y + h);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x + w, y);
        GL11.glEnd();
        
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private void drawCornerBox(int boxX, int boxY, int size) {
        Gui.drawRect(boxX, boxY, boxX + size, boxY + size, 0xFFFF0000); // Solid Red
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