package com.linexstudios.foxtrot.Misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AutoGhead {
    public static final AutoGhead instance = new AutoGhead();
    public static Minecraft mc = Minecraft.getMinecraft();
    
    // GUI Toggles
    public static boolean enabled = true;
    public static double healthThreshold = 12.0; // 6 Hearts

    public static int oldSlot = -1;
    public static int gHeadSlot = -1;
    private static int tickDelay = 0;
    private static int timeoutTimer = 0; // Prevents getting stuck
    
    private enum State {IDLE, SWAP, EAT, SWAPBACK}
    private static State state = State.IDLE;

    /**
     * Checks if the item is a Golden Head OR a First-Aid Egg.
     */
    public static boolean isHealingItem(ItemStack item) {
        if (item == null || !item.hasTagCompound()) return false;
        NBTTagCompound tag = item.getTagCompound();
        if (tag.hasKey("display")) {
            NBTTagCompound display = tag.getCompoundTag("display");
            if (display.hasKey("Name")) {
                String displayName = display.getString("Name");
                // Strip formatting entirely to safely check the string
                String cleanName = EnumChatFormatting.getTextWithoutFormattingCodes(displayName);
                
                return "Golden Head".equals(cleanName) || "First-Aid Egg".equals(cleanName);
            }
        }
        return false;
    }

    private void forceReset() {
        state = State.IDLE;
        tickDelay = 0;
        timeoutTimer = 0;
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || mc.theWorld == null) return;
        
        if (state == State.IDLE) {
            // Only trigger if enabled, health is low, and we have no GUI open
            if (enabled && mc.thePlayer.getHealth() <= healthThreshold && mc.currentScreen == null) {
                state = State.SWAP;
                timeoutTimer = 0;
            }
        }

        if (state != State.IDLE) {
            timeoutTimer++;
            // FAILSAFE: If the whole process takes more than 1 second (20 ticks), force reset it.
            if (timeoutTimer > 20) {
                forceReset();
                return;
            }

            switch (state) {
                case SWAP:
                    if (mc.currentScreen != null) {
                        forceReset();
                        break;
                    }
                    gHeadSlot = -1;
                    // Scan hotbar for Ghead or First-Aid Egg
                    for (int i = 0; i <= 8; i++) {
                        ItemStack item = mc.thePlayer.inventory.getStackInSlot(i);
                        if (isHealingItem(item)) {
                            oldSlot = mc.thePlayer.inventory.currentItem;
                            gHeadSlot = i;
                            break;
                        }
                    }

                    if (gHeadSlot != -1) {
                        mc.thePlayer.inventory.currentItem = gHeadSlot;
                        state = State.EAT;
                        tickDelay = 0;
                    } else {
                        forceReset(); // No healing item found, go back to idle
                    }
                    break;
                case EAT:
                    tickDelay++;
                    // Wait a few ticks for the server to register the slot change before clicking
                    if (tickDelay >= 3){
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
                        // Hold right click slightly longer to guarantee it eats/uses the egg
                        if (tickDelay >= 6){ 
                            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                            state = State.SWAPBACK;
                            tickDelay = 0;
                        }
                    }
                    break;
                case SWAPBACK:
                    tickDelay++;
                    // Wait 2 ticks before swapping back to prevent ghost-items
                    if (tickDelay >= 2){
                        if (oldSlot != -1) {
                            mc.thePlayer.inventory.currentItem = oldSlot;
                        }
                        forceReset();
                    }
                    break;
            }
        }
    }
}