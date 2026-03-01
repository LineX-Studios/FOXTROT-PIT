package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class TelebowHUD extends DraggableHUD {
    
    public static final TelebowHUD instance = new TelebowHUD();
    private final Minecraft mc = Minecraft.getMinecraft();
    
    public static boolean enabled = true;

    // Timer & Tracking
    private long timerEndTime = 0L;
    private long lastTelebowPrimeTime = 0L;
    private int lastTelebowLevel = 3;
    
    private double lastX = 0, lastY = 0, lastZ = 0;
    private boolean wasTimerActiveLastTick = false;

    public TelebowHUD() {
        super("Telebow Timer", 200, 200); 
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // Called by MixinNetHandlerPlayClient to sync the timer
    public void setCooldown(long seconds) {
        this.timerEndTime = System.currentTimeMillis() + (seconds * 1000L);
    }

    // Called by MixinNetHandlerPlayClient to reset on death
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

    // ==========================================
    //  STEP 1: CAPTURE SHOT (FIXED NBT)
    // ==========================================
    @SubscribeEvent
    public void onArrowLoose(ArrowLooseEvent event) {
        if (!enabled || mc.thePlayer == null || event.bow == null) return;

        // Must be sneaking to telebow
        if (mc.thePlayer.isSneaking()) {
            int telebowLevel = getPitEnchantLevel(event.bow, "telebow");
            
            if (telebowLevel > 0) {
                this.lastTelebowPrimeTime = System.currentTimeMillis();
                this.lastTelebowLevel = telebowLevel;
            }
        }
    }

    // ==========================================
    //  STEP 2: WAIT FOR TELEPORT (Physics)
    // ==========================================
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled || event.phase != TickEvent.Phase.END || mc.thePlayer == null) return;

        // Audio Cue
        boolean isTimerActiveNow = this.timerEndTime > System.currentTimeMillis();
        if (this.wasTimerActiveLastTick && !isTimerActiveNow && this.timerEndTime != 0) {
            mc.thePlayer.playSound("random.successful_hit", 1.0f, 1.2f);
        }
        this.wasTimerActiveLastTick = isTimerActiveNow;

        if (lastX != 0 || lastY != 0 || lastZ != 0) {
            double dx = mc.thePlayer.posX - lastX;
            double dy = mc.thePlayer.posY - lastY;
            double dz = mc.thePlayer.posZ - lastZ;
            double distanceSquared = (dx * dx) + (dy * dy) + (dz * dz);

            // If player instantly moves more than 5 blocks...
            if (distanceSquared > 25.0D) {
                
                // ...and they shot a telebow in the last 15 seconds!
                if (System.currentTimeMillis() - lastTelebowPrimeTime <= 15000L) {
                    
                    long cooldown = 30000L; // Tier 3
                    if (lastTelebowLevel == 1) cooldown = 90000L; // Tier 1
                    else if (lastTelebowLevel == 2) cooldown = 55000L; // Tier 2

                    // We only start it if the Mixin hasn't already started it
                    if (System.currentTimeMillis() >= this.timerEndTime) {
                        this.timerEndTime = System.currentTimeMillis() + cooldown;
                    }
                    
                    this.lastTelebowPrimeTime = 0L; // Consume prime
                }
            }
        }

        // Save position
        lastX = mc.thePlayer.posX;
        lastY = mc.thePlayer.posY;
        lastZ = mc.thePlayer.posZ;
    }

    // ==========================================
    //  FIXED: NBT TAG LIST ITERATOR
    // ==========================================
    private int getPitEnchantLevel(ItemStack stack, String enchantKey) {
        if (stack == null || !stack.hasTagCompound()) return 0;
        
        NBTTagCompound tag = stack.getTagCompound();
        // Assuming CustomEnchants is inside ExtraAttributes like standard Pit items
        if (tag.hasKey("ExtraAttributes", 10)) {
            NBTTagCompound extra = tag.getCompoundTag("ExtraAttributes");
            
            // 9 is the NBT ID for a List!
            if (extra.hasKey("CustomEnchants", 9)) {
                // 10 is the NBT ID for the Compounds inside the list
                NBTTagList enchants = extra.getTagList("CustomEnchants", 10);
                
                for (int i = 0; i < enchants.tagCount(); i++) {
                    NBTTagCompound enchant = enchants.getCompoundTagAt(i);
                    // Match the JSON structure you provided!
                    if (enchant.hasKey("Key", 8) && enchant.getString("Key").equals(enchantKey)) {
                        return enchant.getInteger("Level");
                    }
                }
            }
        }
        
        // Failsafe: Check if CustomEnchants is directly on the root tag instead of ExtraAttributes
        if (tag.hasKey("CustomEnchants", 9)) {
            NBTTagList enchants = tag.getTagList("CustomEnchants", 10);
            for (int i = 0; i < enchants.tagCount(); i++) {
                NBTTagCompound enchant = enchants.getCompoundTagAt(i);
                if (enchant.hasKey("Key", 8) && enchant.getString("Key").equals(enchantKey)) {
                    return enchant.getInteger("Level");
                }
            }
        }
        
        return 0;
    }
}