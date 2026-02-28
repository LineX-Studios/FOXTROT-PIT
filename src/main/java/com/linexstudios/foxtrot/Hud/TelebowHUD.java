package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TelebowHUD extends DraggableHUD {
    
    public static final TelebowHUD instance = new TelebowHUD();
    private final Minecraft mc = Minecraft.getMinecraft();
    
    public static boolean enabled = true;

    private long timerEndTime = 0L;
    private double lastX = 0, lastY = 0, lastZ = 0;

    public TelebowHUD() {
        super("Telebow Timer", 200, 200); 
    }

    // Tells the DraggableHUD master registry whether this module should be hidden or not
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (mc.currentScreen instanceof EditHUDGui || mc.currentScreen instanceof HUDSettingsGui) return;
        render(false, 0, 0); 
    }

    @Override
    public void draw(boolean isEditing) {
        if (!enabled || mc.theWorld == null || mc.thePlayer == null) {
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

            EnumChatFormatting timeColor = EnumChatFormatting.RED;
            if (secondsLeft <= 10.0) timeColor = EnumChatFormatting.YELLOW;
            if (secondsLeft <= 3.0) timeColor = EnumChatFormatting.GREEN;

            String msg = EnumChatFormatting.DARK_PURPLE + "[" + EnumChatFormatting.LIGHT_PURPLE + "TB" + EnumChatFormatting.DARK_PURPLE + "] " + EnumChatFormatting.GOLD + "Telebow: " + timeColor + EnumChatFormatting.BOLD + formattedTime + "s";
            
            fr.drawStringWithShadow(msg, 0, 0, 0xFFFFFF);
            this.width = fr.getStringWidth(msg);
            this.height = fr.FONT_HEIGHT;
            
        } 
        else if (isEditing) {
            String dummyMsg = EnumChatFormatting.DARK_PURPLE + "[" + EnumChatFormatting.LIGHT_PURPLE + "TB" + EnumChatFormatting.DARK_PURPLE + "] " + EnumChatFormatting.GOLD + "Telebow: " + EnumChatFormatting.RED + EnumChatFormatting.BOLD + "14.5s";
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
    //  THE HIJACKER: BLOCKS VANILLA ACTION BAR
    // ==========================================
    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (event.message == null) return; 

        // Type 2 = Vanilla Action Bar
        if (event.type == 2) {
            String cleanMessage = StringUtils.stripControlCodes(event.message.getUnformattedText());

            if (cleanMessage.contains("Telebow") && cleanMessage.contains("cooldown")) {
                
                // 1. WE ALWAYS CANCEL IT! Even if our HUD is turned off!
                // This permanently stops the test server's fading text from ever showing up.
                event.setCanceled(true);

                // 2. ONLY parse the data and run the timer if our HUD is actually turned on
                if (enabled) {
                    Matcher m = Pattern.compile("(\\d+)s").matcher(cleanMessage);
                    if (!m.find()) m = Pattern.compile("(\\d+) seconds").matcher(cleanMessage);
                    
                    if (m.find()) {
                        long serverSecondsLeft = Long.parseLong(m.group(1));
                        this.timerEndTime = System.currentTimeMillis() + (serverSecondsLeft * 1000L);
                    }
                }
            }
        } else {
            // Failsafes (Normal Chat)
            if (!enabled) return;
            
            String cleanMessage = StringUtils.stripControlCodes(event.message.getUnformattedText());
            if (cleanMessage.contains("NOPE! Can't teleport there") || 
                cleanMessage.contains("You died!") || 
                cleanMessage.contains("RESPAWNED!") ||
                cleanMessage.contains("Teleporting to spawn")) {
                this.timerEndTime = 0L;
            }
        }
    }

    // ==========================================
    //  PHYSICS FALLBACK: IN CASE SERVER IS QUIET
    // ==========================================
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled || event.phase != TickEvent.Phase.END || mc.thePlayer == null) return;

        if (lastX != 0 || lastY != 0 || lastZ != 0) {
            double dx = mc.thePlayer.posX - lastX;
            double dy = mc.thePlayer.posY - lastY;
            double dz = mc.thePlayer.posZ - lastZ;
            double distSq = (dx * dx) + (dy * dy) + (dz * dz);

            // If you instantly teleport 6+ blocks...
            if (distSq > 36.0D) {
                ItemStack heldItem = mc.thePlayer.getHeldItem();
                int telebowLevel = getPitEnchantLevel(heldItem, "telebow");
                
                if (telebowLevel == 0 && heldItem != null && heldItem.hasDisplayName() && heldItem.getDisplayName().toLowerCase().contains("telebow")) {
                    telebowLevel = 3;
                }

                if (telebowLevel > 0) {
                    long cooldown = 30000L; 
                    if (telebowLevel == 1) cooldown = 90000L; 
                    else if (telebowLevel == 2) cooldown = 55000L; 
                    
                    if (System.currentTimeMillis() >= this.timerEndTime) {
                        this.timerEndTime = System.currentTimeMillis() + cooldown;
                    }
                }
            }
        }

        lastX = mc.thePlayer.posX;
        lastY = mc.thePlayer.posY;
        lastZ = mc.thePlayer.posZ;
    }

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