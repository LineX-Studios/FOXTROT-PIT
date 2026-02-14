package com.linexstudios.foxtrot.Combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class AutoClicker {
    public static final AutoClicker instance = new AutoClicker();
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random rand = new Random();
    private Robot robot;

    public AutoClicker() {
        try { this.robot = new Robot(); } catch (Exception ignored) {}
    }

    // Main Toggle
    public static boolean enabled = false;

    // Vape V4 Settings
    public static boolean holdToClick = true;
    public static boolean triggerMode = false;
    
    // Inventory Fill
    public static boolean inventoryFill = true;
    public static float inventoryFillCps = 15.0F;
    private long lastInvClick = 0L;

    // World CPS
    public static float minCps = 9.0F;
    public static float maxCps = 13.0F;
    public static int randomMode = 1; // 0=Normal, 1=Extra, 2=Extra+
    
    // Add-ons
    public static boolean jitter = false;
    public static boolean breakBlocks = true;
    public static float breakBlocksDelay = 2.0F; // Converted to slider format
    private int blockHitTicks = 0;

    // Whitelist
    public static boolean limitItems = true;
    public static List<String> itemWhitelist = new ArrayList<>(Arrays.asList("sword", "axe"));

    private long lastClickTime = 0L;
    private long nextDelay = 0L;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled || mc.thePlayer == null || mc.theWorld == null) return;

        // --- INVENTORY FILL LOGIC ---
        if (mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer) {
            if (inventoryFill && Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                long invDelay = inventoryFillCps >= 20.0F ? 0 : (long)(1000.0F / inventoryFillCps);
                if (System.currentTimeMillis() - lastInvClick >= invDelay) {
                    // Uses Java Robot to simulate a real hardware click in the GUI
                    if (robot != null) {
                        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                    }
                    lastInvClick = System.currentTimeMillis();
                }
            }
            return; // Don't run world combat logic if in inventory
        }

        // --- WORLD COMBAT LOGIC ---
        if (holdToClick && !Mouse.isButtonDown(0)) {
            blockHitTicks = 0;
            return;
        }

        if (limitItems && !isHoldingWhitelistedItem()) return;

        if (breakBlocks && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            blockHitTicks++;
            if (blockHitTicks > (breakBlocksDelay * 20)) return; // Convert seconds/slider value to ticks
        } else {
            blockHitTicks = 0;
        }

        if (triggerMode && (mc.objectMouseOver == null || mc.objectMouseOver.entityHit == null)) return;

        if (System.currentTimeMillis() - lastClickTime >= nextDelay) {
            int attackKey = mc.gameSettings.keyBindAttack.getKeyCode();
            KeyBinding.setKeyBindState(attackKey, true);
            KeyBinding.onTick(attackKey);
            KeyBinding.setKeyBindState(attackKey, false);

            if (jitter) {
                mc.thePlayer.rotationYaw += (rand.nextFloat() - 0.5F);
                mc.thePlayer.rotationPitch += (rand.nextFloat() - 0.5F);
            }

            lastClickTime = System.currentTimeMillis();
            generateNextDelay();
        }
    }

    private void generateNextDelay() {
        float min = Math.min(minCps, maxCps);
        float max = Math.max(minCps, maxCps);
        float targetCps = min + (rand.nextFloat() * (max - min));
        long baseDelay = (long) (1000.0F / targetCps);

        long randomOffset = 0;
        switch (randomMode) {
            case 0: randomOffset = (long) ((rand.nextGaussian() * 15) - 7); break;
            case 1: 
                randomOffset = (long) ((rand.nextGaussian() * 25) - 10);
                if (rand.nextInt(10) == 1) randomOffset += 40; 
                break;
            case 2: 
                randomOffset = (long) ((rand.nextGaussian() * 35) - 15);
                if (rand.nextInt(20) == 1) randomOffset -= 30; 
                if (rand.nextInt(15) == 1) randomOffset += 60; 
                break;
        }
        this.nextDelay = Math.max(20, baseDelay + randomOffset);
    }

    private boolean isHoldingWhitelistedItem() {
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return false;
        if (held.getItem() instanceof ItemSword && itemWhitelist.contains("sword")) return true;
        if (held.getItem() instanceof ItemAxe && itemWhitelist.contains("axe")) return true;
        String name = held.getItem().getUnlocalizedName().toLowerCase();
        for (String w : itemWhitelist) if (name.contains(w.toLowerCase())) return true;
        return false;
    }
}