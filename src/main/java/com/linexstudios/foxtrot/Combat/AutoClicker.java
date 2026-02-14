package com.linexstudios.foxtrot.Combat;

import com.linexstudios.foxtrot.Foxtrot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.*;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Robot;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class AutoClicker {
    public static final AutoClicker instance = new AutoClicker();
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random rand = new Random();
    private Robot robot;
    private Field leftClickCounterField;

    // --- Toggles & Debug ---
    public static boolean enabled = false;
    public static boolean debugMode = false;
    public static boolean leftClick = true;
    public static boolean rightClick = false;
    public static boolean holdToClick = true;

    // --- Inventory Fill ---
    public static boolean inventoryFill = true;
    public static float inventoryFillCps = 15.0F;
    private long lastInvClick = 0L;

    // --- Settings ---
    public static float minCps = 9.0F;
    public static float maxCps = 13.0F;
    public static int randomMode = 1; 
    public static boolean breakBlocks = true;
    private int blockHitTicks = 0;

    // --- Whitelist ---
    public static boolean limitItems = false;
    public static List<String> itemWhitelist = new ArrayList<>(Arrays.asList("sword", "axe", "pickaxe"));

    private long lastLeftClickTime = 0L;
    private long nextLeftDelay = 0L;
    private long lastRightClickTime = 0L;
    private long nextRightDelay = 0L;

    public AutoClicker() {
        try { this.robot = new Robot(); } catch (Exception ignored) {}
        try {
            leftClickCounterField = Minecraft.class.getDeclaredField("leftClickCounter");
        } catch (NoSuchFieldException e) {
            try { leftClickCounterField = Minecraft.class.getDeclaredField("field_71429_W"); } catch (Exception ex) {}
        }
        if (leftClickCounterField != null) leftClickCounterField.setAccessible(true);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (Foxtrot.toggleCombatKey.isPressed()) {
            enabled = !enabled;
            if (debugMode) System.out.println("[Foxtrot-Debug] AutoClicker Toggled: " + enabled);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || !enabled) return;

        int attackKey = mc.gameSettings.keyBindAttack.getKeyCode();
        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();

        if (event.phase == TickEvent.Phase.START) {
            if (leftClick && holdToClick && Mouse.isButtonDown(0)) KeyBinding.setKeyBindState(attackKey, false);
            if (rightClick && holdToClick && Mouse.isButtonDown(1)) KeyBinding.setKeyBindState(useKey, false);
            return;
        }

        // --- PHASE.END LOGIC ---
        if (mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer) {
            if (inventoryFill && Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                long invDelay = inventoryFillCps >= 20.0F ? 0 : (long)(1000.0F / inventoryFillCps);
                if (System.currentTimeMillis() - lastInvClick >= invDelay) {
                    if (robot != null) {
                        // Using fully qualified name here to avoid the compile error
                        robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                        robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                    }
                    lastInvClick = System.currentTimeMillis();
                }
            }
            return; 
        }

        if (mc.currentScreen != null) return;

        if (leftClick) {
            boolean shouldLeftClick = !holdToClick || Mouse.isButtonDown(0);
            if (breakBlocks && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                blockHitTicks++;
                if (blockHitTicks > 15) shouldLeftClick = false; 
            } else blockHitTicks = 0;

            if (limitItems && !isHoldingWhitelistedItem()) shouldLeftClick = false;

            if (shouldLeftClick && System.currentTimeMillis() - lastLeftClickTime >= nextLeftDelay) {
                try {
                    if (leftClickCounterField != null) leftClickCounterField.set(mc, 0);
                    if (debugMode) System.out.println("[Foxtrot-Debug] FIRING LEFT CLICK");
                    KeyBinding.setKeyBindState(attackKey, true);
                    KeyBinding.onTick(attackKey);
                } catch (Exception e) {}
                lastLeftClickTime = System.currentTimeMillis();
                nextLeftDelay = generateNextDelay();
            }
        }

        if (rightClick) {
            boolean shouldRightClick = !holdToClick || Mouse.isButtonDown(1);
            if (shouldRightClick && System.currentTimeMillis() - lastRightClickTime >= nextRightDelay) {
                KeyBinding.setKeyBindState(useKey, true);
                KeyBinding.onTick(useKey);
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
        Item item = held.getItem();
        if (itemWhitelist.contains("blocks") && item instanceof ItemBlock) return true;
        if (item instanceof ItemSword && itemWhitelist.contains("sword")) return true;
        if (item instanceof ItemAxe && itemWhitelist.contains("axe")) return true;
        if (item instanceof ItemPickaxe && itemWhitelist.contains("pickaxe")) return true;
        String name = item.getUnlocalizedName().toLowerCase();
        for (String w : itemWhitelist) if (name.contains(w.toLowerCase())) return true;
        return false;
    }
}