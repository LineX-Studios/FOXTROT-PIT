package com.linexstudios.foxtrot.Combat;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Robot;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class AutoClicker {
    public static final AutoClicker instance = new AutoClicker();
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random rand = new Random();
    private Robot robot;

    // Reflection Methods to force clicks
    private Method leftClickMethod;
    private Method rightClickMethod;

    // Toggles
    public static boolean enabled = false;
    public static int bind = Keyboard.KEY_NONE; // Custom Hotkey Binding
    public static boolean leftClick = true;
    public static boolean rightClick = false;

    // Settings
    public static boolean holdToClick = true;
    public static boolean inventoryFill = true;
    public static float inventoryFillCps = 15.0F;
    private long lastInvClick = 0L;

    public static float minCps = 9.0F;
    public static float maxCps = 13.0F;
    public static int randomMode = 1; 
    
    public static boolean breakBlocks = true;
    private int blockHitTicks = 0;

    public static boolean limitItems = false;
    public static List<String> itemWhitelist = new ArrayList<>(Arrays.asList("sword", "axe"));

    private long lastLeftClickTime = 0L;
    private long nextLeftDelay = 0L;
    private long lastRightClickTime = 0L;
    private long nextRightDelay = 0L;

    public AutoClicker() {
        try { this.robot = new Robot(); } catch (Exception ignored) {}
        
        // Setup Reflection to bypass Minecraft's input handler and guarantee clicks
        try {
            leftClickMethod = Minecraft.class.getDeclaredMethod("clickMouse"); // Dev Environment
        } catch (NoSuchMethodException e) {
            try { leftClickMethod = Minecraft.class.getDeclaredMethod("func_147116_af"); } catch (NoSuchMethodException ex) {} // Obfuscated
        }
        if (leftClickMethod != null) leftClickMethod.setAccessible(true);

        try {
            rightClickMethod = Minecraft.class.getDeclaredMethod("rightClickMouse"); // Dev Environment
        } catch (NoSuchMethodException e) {
            try { rightClickMethod = Minecraft.class.getDeclaredMethod("func_147121_ag"); } catch (NoSuchMethodException ex) {} // Obfuscated
        }
        if (rightClickMethod != null) rightClickMethod.setAccessible(true);
    }

    // --- HOTKEY LISTENER ---
    @SubscribeEvent
    public void onKeyInput(net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent event) {
        if (bind != Keyboard.KEY_NONE && Keyboard.isKeyDown(bind)) {
            // Ensure we only toggle once per press, not spam it
            if (mc.currentScreen == null) {
                enabled = !enabled;
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled || mc.thePlayer == null || mc.theWorld == null) return;

        // Inventory Fill Logic
        if (mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer) {
            if (inventoryFill && Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                long invDelay = inventoryFillCps >= 20.0F ? 0 : (long)(1000.0F / inventoryFillCps);
                if (System.currentTimeMillis() - lastInvClick >= invDelay) {
                    if (robot != null) {
                        robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                        robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                    }
                    lastInvClick = System.currentTimeMillis();
                }
            }
            return; 
        }

        // Do not click if a menu (like chat or escape menu) is open
        if (mc.currentScreen != null) return;

        // --- LEFT CLICK LOGIC ---
        if (leftClick) {
            boolean shouldLeftClick = !holdToClick || Mouse.isButtonDown(0);
            
            if (breakBlocks && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                blockHitTicks++;
                if (blockHitTicks > 10) shouldLeftClick = false; 
            } else {
                blockHitTicks = 0;
            }

            if (limitItems && !isHoldingWhitelistedItem()) shouldLeftClick = false;

            if (shouldLeftClick && System.currentTimeMillis() - lastLeftClickTime >= nextLeftDelay) {
                try {
                    if (leftClickMethod != null) leftClickMethod.invoke(mc); // Force the click
                } catch (Exception e) {}
                lastLeftClickTime = System.currentTimeMillis();
                nextLeftDelay = generateNextDelay();
            }
        }

        // --- RIGHT CLICK LOGIC ---
        if (rightClick) {
            boolean shouldRightClick = !holdToClick || Mouse.isButtonDown(1);
            
            if (shouldRightClick && System.currentTimeMillis() - lastRightClickTime >= nextRightDelay) {
                try {
                    if (rightClickMethod != null) rightClickMethod.invoke(mc); // Force the click
                } catch (Exception e) {}
                lastRightClickTime = System.currentTimeMillis();
                nextRightDelay = generateNextDelay();
            }
        }
    }

    private long generateNextDelay() {
        float targetCps = minCps + (rand.nextFloat() * (maxCps - minCps));
        long baseDelay = (long) (1000.0F / targetCps);
        
        long randomOffset = 0;
        switch (randomMode) {
            case 0: randomOffset = (long) ((rand.nextGaussian() * 15) - 7); break;
            case 1: randomOffset = (long) ((rand.nextGaussian() * 25) - 10); break;
            case 2: randomOffset = (long) ((rand.nextGaussian() * 35) - 15); break;
        }
        return Math.max(20, baseDelay + randomOffset);
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