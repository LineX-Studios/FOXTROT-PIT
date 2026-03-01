package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public class TelebowHUD extends DraggableHUD {
    
    public static final TelebowHUD instance = new TelebowHUD();
    private final Minecraft mc = Minecraft.getMinecraft();
    
    public static boolean enabled = true;

    // Timer & Tracking
    private long timerEndTime = 0L;
    private long lastTelebowPrimeTime = 0L;
    private long lastSpawnCommandTime = 0L; // ADDED: Tracks when you typed /spawn
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

    public void setCooldown(long seconds) {
        this.timerEndTime = System.currentTimeMillis() + (seconds * 1000L);
    }

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
    //  STEP 1: CAPTURE THE TELEBOW SHOT
    // ==========================================
    @SubscribeEvent
    public void onArrowLoose(ArrowLooseEvent event) {
        if (!enabled || mc.thePlayer == null || event.bow == null) return;

        if (mc.thePlayer.isSneaking()) {
            int telebowLevel = getPitEnchantLevel(event.bow, "telebow");
            if (telebowLevel > 0) {
                this.lastTelebowPrimeTime = System.currentTimeMillis();
                this.lastTelebowLevel = telebowLevel;
            }
        }
    }

    // ==========================================
    //  STEP 1.5: CAPTURE THE /SPAWN COMMAND
    // ==========================================
    @SubscribeEvent
    public void onPlayerCommand(ClientChatEvent event) {
        if (!enabled || event.message == null) return;

        String msg = event.message.toLowerCase().trim();
        
        // Prime the "Spawn Tracker" for the next 10 seconds
        if (msg.equals("/spawn") || msg.equals("/respawn") || msg.equals("/oof")) {
            this.lastSpawnCommandTime = System.currentTimeMillis();
        }
    }

    // ==========================================
    //  STEP 2: WAIT FOR THE PHYSICAL TELEPORT
    // ==========================================
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled || event.phase != TickEvent.Phase.END || mc.thePlayer == null) return;

        // Audio Cue
        boolean isTimerActiveNow = this.timerEndTime > System.currentTimeMillis();
        if (this.wasTimerActiveLastTick && !isTimerActiveNow && this.timerEndTime != 0) {
            mc.thePlayer.playSound("random.orb", 1.0f, 0.5f);
        }
        this.wasTimerActiveLastTick = isTimerActiveNow;

        if (lastX != 0 || lastY != 0 || lastZ != 0) {
            double dx = mc.thePlayer.posX - lastX;
            double dy = mc.thePlayer.posY - lastY;
            double dz = mc.thePlayer.posZ - lastZ;
            double distanceSquared = (dx * dx) + (dy * dy) + (dz * dz);

            // Did the player instantly move more than 5 blocks?
            if (distanceSquared > 25.0D) { 
                
                // 1. Was it a Telebow shot? (Takes priority!)
                if (System.currentTimeMillis() - lastTelebowPrimeTime <= 15000L) {
                    long cooldown = 20000L; 
                    if (lastTelebowLevel == 1) cooldown = 90000L; 
                    else if (lastTelebowLevel == 2) cooldown = 45000L; 

                    if (System.currentTimeMillis() >= this.timerEndTime) {
                        this.timerEndTime = System.currentTimeMillis() + cooldown;
                    }
                    
                    this.lastTelebowPrimeTime = 0L; 
                    this.lastSpawnCommandTime = 0L; // Cancel spawn tracker so it doesn't double-trigger
                }
                
                // 2. Was it a successful /spawn teleport?
                else if (System.currentTimeMillis() - lastSpawnCommandTime <= 10000L) {
                    clearCooldown();
                    this.lastSpawnCommandTime = 0L;
                }
            }
        }

        lastX = mc.thePlayer.posX;
        lastY = mc.thePlayer.posY;
        lastZ = mc.thePlayer.posZ;
    }

    // ==========================================
    //  MECHANIC: -3 SECONDS PER ARROW HIT
    // ==========================================
    @SubscribeEvent
    public void onArrowHit(PlaySoundEvent event) {
        if (!enabled || mc.thePlayer == null) return;

        if (event.name.equals("random.successful_hit")) {
            if (this.timerEndTime > System.currentTimeMillis()) {
                this.timerEndTime -= 3000L;
            }
        }
    }

    // ==========================================
    //  MECHANIC: RESET ON LOBBY LEAVE
    // ==========================================
    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        clearCooldown();
    }

    // ==========================================
    //  NBT TAG LIST ITERATOR
    // ==========================================
    private int getPitEnchantLevel(ItemStack stack, String enchantKey) {
        if (stack == null || !stack.hasTagCompound()) return 0;
        
        NBTTagCompound tag = stack.getTagCompound();
        if (tag.hasKey("ExtraAttributes", 10)) {
            NBTTagCompound extra = tag.getCompoundTag("ExtraAttributes");
            if (extra.hasKey("CustomEnchants", 9)) {
                NBTTagList enchants = extra.getTagList("CustomEnchants", 10);
                for (int i = 0; i < enchants.tagCount(); i++) {
                    NBTTagCompound enchant = enchants.getCompoundTagAt(i);
                    if (enchant.hasKey("Key", 8) && enchant.getString("Key").equals(enchantKey)) {
                        return enchant.getInteger("Level");
                    }
                }
            }
        }
        
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