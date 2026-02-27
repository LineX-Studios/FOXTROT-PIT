package com.linexstudios.foxtrot.Misc;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Telebow Timer
 * Tracks arrow shots, reads NBT for cooldown times, and renders to the screen
 */
public class TelebowTimer {
    
    public static final TelebowTimer instance = new TelebowTimer();
    private final CooldownTimer timer = new CooldownTimer();
    private long lastCrouchTime = 0L;
    
    // Configurable: Provides leniency if the player uncrouches a split-second before shooting
    public long crouchWindowMs = 250L; // Increased to 250ms for better reliability
    
    // Toggle for the module itself
    public static boolean isEnabled = true;

    private final Minecraft mc = Minecraft.getMinecraft();

    /**
     * Renders the cooldown on the screen without flickering.
     */
    @SubscribeEvent
    public void onRenderHUD(RenderGameOverlayEvent.Text event) {
        if (!isEnabled || mc.thePlayer == null) return;

        // Keep updating crouch time
        if (mc.thePlayer.isSneaking()) {
            this.lastCrouchTime = System.currentTimeMillis();
        }

        // Render the cooldown if active
        if (this.timer.isActive()) {
            double secondsLeft = this.timer.getTimeRemaining() / 1000.0;
            String formattedTime = String.format("%.1f", secondsLeft);
            
            String msg = EnumChatFormatting.GOLD + "Telebow: " + EnumChatFormatting.RED + formattedTime + "s";
            
            // Draw it right above the hotbar
            int x = event.resolution.getScaledWidth() / 2 - (mc.fontRendererObj.getStringWidth(msg) / 2);
            int y = event.resolution.getScaledHeight() / 2 + 15; 
            
            mc.fontRendererObj.drawStringWithShadow(msg, x, y, -1);
        }
    }

    /**
     * Listens for the player shooting their bow and triggers the cooldown.
     */
    @SubscribeEvent
    public void onArrowLoose(ArrowLooseEvent event) {
        if (!isEnabled || mc.thePlayer == null) return;
        
        ItemStack heldItem = event.bow;
        if (heldItem == null) return;

        // 1. Ensure they shot it while sneaking
        if (System.currentTimeMillis() - this.lastCrouchTime <= crouchWindowMs) {
            
            // 2. Read NBT
            int telebowLevel = getPitEnchantLevel(heldItem, "telebow");
            
            if (telebowLevel > 0) {
                long cooldownLength = 0L;
                if (telebowLevel == 1) cooldownLength = 90000L; // Tier 1: 90 seconds
                else if (telebowLevel == 2) cooldownLength = 55000L; // Tier 2: 55 seconds
                else if (telebowLevel >= 3) cooldownLength = 30000L; // Tier 3: 30 seconds
                
                this.timer.start(cooldownLength);
            }
        }
    }

    /**
     * Reads chat to cancel the timer if the server rejected the teleport.
     */
    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!isEnabled || event.type == 2) return; // Ignore action bar packets
        
        // Strip Minecraft color codes (like §c) to read raw text
        String cleanMessage = StringUtils.stripControlCodes(event.message.getUnformattedText());

        // If the teleport failed (shot out of bounds or on cooldown) resets the timer
        if (cleanMessage.contains("NOPE! Can't teleport there") || cleanMessage.contains("Telebow is on cooldown!")) {
            this.timer.reset();
        }
    }

    // ==========================================
    //            STANDALONE UTILITIES
    // ==========================================

    /**
     * Parses NBT data correctly to find enchant tiers.
     */
    private int getPitEnchantLevel(ItemStack stack, String enchantKey) {
        if (stack == null || !stack.hasTagCompound()) return 0;
        
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey("ExtraAttributes", 10)) return 0; // 10 = Compound Tag
        
        NBTTagCompound extraAttributes = tag.getCompoundTag("ExtraAttributes");
        if (!extraAttributes.hasKey("CustomEnchants", 10)) return 0; // Pit stores enchants as a Compound, not a List!
        
        NBTTagCompound enchants = extraAttributes.getCompoundTag("CustomEnchants");
        
        // Look for "telebow" directly inside the CustomEnchants compound
        if (enchants.hasKey(enchantKey)) {
            return enchants.getInteger(enchantKey);
        }
        
        return 0;
    }

    /**
     * A lightweight stopwatch for tracking cooldowns.
     */
    private static class CooldownTimer {
        private long endTime = 0L;

        public void start(long durationMs) {
            this.endTime = System.currentTimeMillis() + durationMs;
        }

        public boolean isActive() {
            return System.currentTimeMillis() < this.endTime;
        }

        public long getTimeRemaining() {
            return Math.max(0, this.endTime - System.currentTimeMillis());
        }

        public void reset() {
            this.endTime = 0L;
        }
    }
}