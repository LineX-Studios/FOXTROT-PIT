package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import java.util.ArrayList;
import java.util.List;

public abstract class DraggableHUD {
    
    // --- AUTO-REGISTRY ---
    private static final List<DraggableHUD> REGISTRY = new ArrayList<>();

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
        // Register itself upon creation
        if (!REGISTRY.contains(this)) {
            REGISTRY.add(this);
        }
    }

    public static List<DraggableHUD> getRegistry() {
        return REGISTRY;
    }

    // Auto-detects if the specific module is enabled using Reflection
    public boolean isEnabled() {
        try {
            return this.getClass().getField("enabled").getBoolean(this);
        } catch (Exception e) {
            return true; // Default to true if no 'enabled' field exists
        }
    }

    public abstract void draw(boolean isEditing);

    public void render(boolean isEditing, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);

        if (isEditing) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.8f, 0.8f, 1.0f);
            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(name, 0, -11, 0xFFFFFF);
            GlStateManager.popMatrix();
        }

        GlStateManager.scale(scale, scale, 1);

        if (isEditing) {
            float bgAlpha = hovered ? 0.35F : 0.15F; 
            
            RenderUtils.setup2D();
            
            // Draw Translucent Fill
            RenderUtils.drawRect(0, 0, width, height, 1.0F, 1.0F, 1.0F, bgAlpha);
            
            // Draw Clean Thin Outlines
            RenderUtils.drawRect(0, 0, width, 1, 1.0F, 1.0F, 1.0F, 0.4F); // Top
            RenderUtils.drawRect(0, height - 1, width, 1, 1.0F, 1.0F, 1.0F, 0.4F); // Bottom
            RenderUtils.drawRect(0, 0, 1, height, 1.0F, 1.0F, 1.0F, 0.4F); // Left
            RenderUtils.drawRect(width - 1, 0, 1, height, 1.0F, 1.0F, 1.0F, 0.4F); // Right
            
            RenderUtils.end2D();
        }

        draw(isEditing); 

        GlStateManager.popMatrix();

        if (isEditing && hovered) {
            int corner = getHoveredCorner(mouseX, mouseY);
            
            float boxSize = 2.0f; 
            float actualW = width * scale;
            float actualH = height * scale;

            // Draw perfectly centered corner nodes using RenderUtils
            RenderUtils.setup2D();
            if (corner == 1) drawCornerBox(x, y, boxSize); 
            else if (corner == 2) drawCornerBox(x + actualW, y, boxSize); 
            else if (corner == 3) drawCornerBox(x, y + actualH, boxSize); 
            else if (corner == 4) drawCornerBox(x + actualW, y + actualH, boxSize); 
            RenderUtils.end2D();
        }
    }

    private void drawCornerBox(float cx, float cy, float halfSize) {
        RenderUtils.drawRect(cx - halfSize, cy - halfSize, halfSize * 2, halfSize * 2, 1.0F, 0.0F, 0.0F, 1.0F); 
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