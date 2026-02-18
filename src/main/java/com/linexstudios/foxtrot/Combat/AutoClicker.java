package com.linexstudios.foxtrot.Combat;

import com.linexstudios.foxtrot.Foxtrot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.*;
import net.minecraft.util.ChatComponentText;
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

    // --- Toggles ---
    public static boolean enabled = false;
    public static boolean debugMode = false;
    public static boolean leftClick = true;
    public static boolean fastPlaceEnabled = false; 
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

    // --- Whitelist (Strictly for Left Click) ---
    public static boolean limitItems = false;
    public static List<String> itemWhitelist = new ArrayList<>(Arrays.asList("swords", "axes", "pickaxes"));

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
        if (Foxtrot.toggleCombatKey != null && Foxtrot.toggleCombatKey.isPressed()) {
            enabled = !enabled;
            if (mc.thePlayer != null) {
                String state = enabled ? "\u00a7aON" : "\u00a7cOFF";
                mc.thePlayer.addChatMessage(new ChatComponentText("\u00a7c[PIT] \u00a77AutoClicker: " + state));
            }
        }
        if (Foxtrot.toggleInvFillKey != null && Foxtrot.toggleInvFillKey.isPressed()) {
            inventoryFill = !inventoryFill;
            if (mc.thePlayer != null) {
                String state = inventoryFill ? "\u00a7aON" : "\u00a7cOFF";
                mc.thePlayer.addChatMessage(new ChatComponentText("\u00a7c[PIT] \u00a77Inventory Fill: " + state));
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer) {
            if (inventoryFill && Mouse.isButtonDown(0)) {
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

        if (!enabled) return;

        int attackKey = mc.gameSettings.keyBindAttack.getKeyCode();
        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
        
        ItemStack held = mc.thePlayer.getHeldItem();
        boolean holdingBlock = (held != null && held.getItem() instanceof ItemBlock);

        if (event.phase == TickEvent.Phase.START) {
            boolean lookingAtBlock = (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK);
            if (leftClick && holdToClick && Mouse.isButtonDown(0)) {
                if (!(breakBlocks && lookingAtBlock)) {
                    KeyBinding.setKeyBindState(attackKey, false);
                }
            }
            if (fastPlaceEnabled && holdToClick && Mouse.isButtonDown(1)) {
                if (holdingBlock) KeyBinding.setKeyBindState(useKey, false);
            }
            return;
        }

        if (mc.currentScreen != null) return;

        if (leftClick) {
            boolean shouldLeftClick = !holdToClick || Mouse.isButtonDown(0);
            if (breakBlocks && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                shouldLeftClick = false; 
            }
            if (limitItems && !isHoldingWhitelistedItem()) {
                shouldLeftClick = false;
            }

            if (shouldLeftClick && System.currentTimeMillis() - lastLeftClickTime >= nextLeftDelay) {
                try {
                    if (leftClickCounterField != null) leftClickCounterField.set(mc, 0); 
                    KeyBinding.setKeyBindState(attackKey, true);
                    KeyBinding.onTick(attackKey);
                    net.minecraftforge.client.event.MouseEvent fakeEvent = new net.minecraftforge.client.event.MouseEvent();
                    Field btnField = net.minecraftforge.client.event.MouseEvent.class.getDeclaredField("button");
                    btnField.setAccessible(true);
                    btnField.set(fakeEvent, 0); 
                    Field stateField = net.minecraftforge.client.event.MouseEvent.class.getDeclaredField("buttonstate");
                    stateField.setAccessible(true);
                    stateField.set(fakeEvent, true); 
                    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(fakeEvent);
                    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.fml.common.gameevent.InputEvent.MouseInputEvent());
                } catch (Exception e) {}
                lastLeftClickTime = System.currentTimeMillis();
                nextLeftDelay = generateNextDelay();
            }
        }

        if (fastPlaceEnabled) {
            boolean shouldRightClick = (!holdToClick || Mouse.isButtonDown(1)) && holdingBlock;
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

    // --- ADVANCED ALLOWLIST LOGIC ---
    private boolean isHoldingWhitelistedItem() {
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return false;
        Item item = held.getItem();
        
        for (String w : itemWhitelist) {
            String check = w.toLowerCase().trim();
            if (check.isEmpty()) continue;
            
            // Allow universal category parsing
            if (check.equals("swords") || check.equals("sword")) {
                if (item instanceof ItemSword) return true;
            } else if (check.equals("axes") || check.equals("axe")) {
                if (item instanceof ItemAxe) return true;
            } else if (check.equals("pickaxes") || check.equals("pickaxe")) {
                if (item instanceof ItemPickaxe) return true;
            } else if (check.equals("shovels") || check.equals("shovel") || check.equals("spades") || check.equals("spade")) {
                if (item instanceof ItemSpade) return true;
            } else if (check.equals("blocks") || check.equals("block")) {
                if (item instanceof ItemBlock) return true;
            } else if (item.getUnlocalizedName().toLowerCase().contains(check)) {
                // Failsafe for specific items
                return true;
            }
        }
        return false;
    }
}