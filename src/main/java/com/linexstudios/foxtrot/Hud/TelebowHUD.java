package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class TelebowHUD extends DraggableHUD {
    
    public static final TelebowHUD instance = new TelebowHUD();
    private final Minecraft mc = Minecraft.getMinecraft();
    
    public static boolean enabled = true;

    // Tracking variables
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

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (mc.currentScreen instanceof EditHUDGui || mc.currentScreen instanceof HUDSettingsGui) return;
        
        // Routes to DraggableHUD base class
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

        // 1. Active Timer
        if (isActive) {
            double secondsLeft = (timerEndTime - currentTime) / 1000.0;
            String formattedTime = String.format("%.1f", Math.max(0.0, secondsLeft));

            // Gold Prefix, Red Timer!
            String msg = EnumChatFormatting.GOLD + "Telebow cooldown: " + EnumChatFormatting.RED + formattedTime + "s";
            
            fr.drawStringWithShadow(msg, 0, 0, 0xFFFFFF);
            
            this.width = fr.getStringWidth(msg);
            this.height = fr.FONT_HEIGHT;
        } 
        // 2. Dummy state for EditHUDGui
        else if (isEditing) {
            String dummyMsg = EnumChatFormatting.GOLD + "Telebow cooldown: " + EnumChatFormatting.RED + "14.5s";
            
            fr.drawStringWithShadow(dummyMsg, 0, 0, 0xFFFFFF);
            
            this.width = fr.getStringWidth(dummyMsg);
            this.height = fr.FONT_HEIGHT;
        } 
        // 3. Hidden
        else {
            this.width = 0;
            this.height = 0;
        }
    }

    // ==========================================
    //  STEP 1: CAPTURE THE SHOT (Anti-Fast Swap)
    // ==========================================
    @SubscribeEvent
    public void onArrowLoose(ArrowLooseEvent event) {
        if (!enabled || mc.thePlayer == null || event.bow == null) return;

        // You must be sneaking to Telebow!
        if (mc.thePlayer.isSneaking()) {
            int telebowLevel = getPitEnchantLevel(event.bow, "telebow");
            
            // Test Server Failsafe: Strips color codes to guarantee it finds the name!
            if (telebowLevel == 0 && event.bow.hasDisplayName()) {
                String cleanName = StringUtils.stripControlCodes(event.bow.getDisplayName()).toLowerCase();
                if (cleanName.contains("telebow")) {
                    telebowLevel = 3; // Assume max tier if custom named
                }
            }

            if (telebowLevel > 0) {
                lastTelebowPrimeTime = System.currentTimeMillis();
                lastTelebowLevel = telebowLevel;
            }
        }
    }

    // ==========================================
    //  STEP 2: WAIT FOR THE TELEPORT
    // ==========================================
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled || event.phase != TickEvent.Phase.END || mc.thePlayer == null) return;

        // Audio Cue: Play a ding when the timer hits zero
        boolean isTimerActiveNow = this.timerEndTime > System.currentTimeMillis();
        if (this.wasTimerActiveLastTick && !isTimerActiveNow && this.timerEndTime != 0) {
            mc.thePlayer.playSound("random.successful_hit", 1.0f, 1.2f);
        }
        this.wasTimerActiveLastTick = isTimerActiveNow;

        // Coordinate Physics Tracking
        if (lastX != 0 || lastY != 0 || lastZ != 0) {
            double dx = mc.thePlayer.posX - lastX;
            double dy = mc.thePlayer.posY - lastY;
            double dz = mc.thePlayer.posZ - lastZ;
            double distanceSquared = (dx * dx) + (dy * dy) + (dz * dz);

            // Did the player instantly move more than 5 blocks?
            if (distanceSquared > 25.0D) {
                
                // Did they shoot a telebow within the last 15 seconds?
                if (System.currentTimeMillis() - lastTelebowPrimeTime <= 15000L) {
                    
                    long cooldown = 30000L; // Tier 3
                    if (lastTelebowLevel == 1) cooldown = 90000L; // Tier 1
                    else if (lastTelebowLevel == 2) cooldown = 55000L; // Tier 2

                    // START THE HUD TIMER!
                    this.timerEndTime = System.currentTimeMillis() + cooldown;
                    
                    // Reset memory to prevent double-triggering
                    this.lastTelebowPrimeTime = 0L;
                }
            }
        }

        // Save position for the next tick
        lastX = mc.thePlayer.posX;
        lastY = mc.thePlayer.posY;
        lastZ = mc.thePlayer.posZ;
    }

    // NBT Scanner
    private int getPitEnchantLevel(ItemStack stack, String enchantKey) {
        if (stack == null || !stack.hasTagCompound()) return 0;
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey("ExtraAttributes", 10)) return 0; 
        NBTTagCompound extra = tag.getCompoundTag("ExtraAttributes");
        if (!extra.hasKey("CustomEnchants", 10)) return 0; 
        NBTTagCompound enchants = extra.getCompoundTag("CustomEnchants");
        if (enchants.hasKey(enchantKey)) return enchants.getInteger(enchantKey);
        return 0;
    }
}