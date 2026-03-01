package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class TelebowHUD extends DraggableHUD {
    
    public static final TelebowHUD instance = new TelebowHUD();
    private final Minecraft mc = Minecraft.getMinecraft();
    
    public static boolean enabled = true;

    private long timerEndTime = 0L;
    private boolean wasTimerActiveLastTick = false;

    public TelebowHUD() {
        super("Telebow Timer", 200, 200); 
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // Called by the Mixin!
    public void setCooldown(long seconds) {
        this.timerEndTime = System.currentTimeMillis() + (seconds * 1000L);
    }

    // Called by the Mixin!
    public void clearCooldown() {
        this.timerEndTime = 0L;
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (mc.currentScreen instanceof EditHUDGui || mc.currentScreen instanceof HUDSettingsGui) return;
        
        render(false, 0, 0); 
    }

    @Override
    public void draw(boolean isEditing) {
        if (!enabled || mc.fontRendererObj == null) {
            this.width = 0;
            this.height = 0;
            return;
        }

        FontRenderer fr = mc.fontRendererObj;
        long currentTime = System.currentTimeMillis();
        boolean isActive = currentTime < timerEndTime;

        if (isActive) {
            double secondsLeft = (timerEndTime - currentTime) / 1000.0;
            String formattedTime = String.format("%.1f", Math.max(0.0, secondsLeft));

            String msg = EnumChatFormatting.GOLD + "Telebow cooldown: " + EnumChatFormatting.RED + formattedTime + "s";
            fr.drawStringWithShadow(msg, 0, 0, 0xFFFFFF);
            
            this.width = fr.getStringWidth(msg);
            this.height = fr.FONT_HEIGHT;
        } 
        else if (isEditing) {
            String dummyMsg = EnumChatFormatting.GOLD + "Telebow cooldown: " + EnumChatFormatting.RED + "14.5s";
            fr.drawStringWithShadow(dummyMsg, 0, 0, 0xFFFFFF);
            this.width = fr.getStringWidth(dummyMsg);
            this.height = fr.FONT_HEIGHT;
        } 
        else {
            this.width = 0;
            this.height = 0;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled || event.phase != TickEvent.Phase.END || mc.thePlayer == null) return;

        // Audio Cue: Play a ding when the timer hits zero!
        boolean isTimerActiveNow = this.timerEndTime > System.currentTimeMillis();
        if (this.wasTimerActiveLastTick && !isTimerActiveNow && this.timerEndTime != 0) {
            mc.thePlayer.playSound("random.successful_hit", 1.0f, 1.2f);
        }
        this.wasTimerActiveLastTick = isTimerActiveNow;
    }
}