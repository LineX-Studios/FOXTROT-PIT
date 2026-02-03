package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

public class EnemyHUD {
    private final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = true;
    public static boolean dragMode = false;

    // Position state
    public static int hudX = 200;
    public static int hudY = 80;

    private static boolean dragging = false;
    private static int dragOffsetX;
    private static int dragOffsetY;

    public static List<String> targetList = new ArrayList<>();

    public void onRender(RenderGameOverlayEvent.Post event) {
        // Essential: Only render during the TEXT phase
        if (!enabled || event.type != RenderGameOverlayEvent.ElementType.TEXT || mc.theWorld == null) return;

        FontRenderer fr = mc.fontRendererObj;
        int xPos = hudX;
        int yPos = hudY;

        boolean foundEnemy = false;

        // Iterate through all loaded players in the world
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!(player instanceof EntityOtherPlayerMP)) continue;
            
            EntityOtherPlayerMP other = (EntityOtherPlayerMP) player;
            if (!isTarget(other.getName())) continue;

            // Draw Header once if an enemy is found
            if (!foundEnemy) {
                String header = EnumChatFormatting.RED.toString() + EnumChatFormatting.BOLD.toString() + "Enemies List:";
                fr.drawStringWithShadow(header, xPos, yPos, 0xFFFFFF);
                yPos += fr.FONT_HEIGHT + 1;
                foundEnemy = true;
            }

            String formattedName = getFormattedName(other.getName());
            String gear = getMainGear(other);
            String distanceDisplay = getDistanceOrSpawn(other);

            String fullLine = formattedName + EnumChatFormatting.WHITE + " in " +
                    gear + EnumChatFormatting.RESET + EnumChatFormatting.GRAY + " - " +
                    distanceDisplay;

            fr.drawStringWithShadow(fullLine, xPos, yPos, 0xFFFFFF);
            yPos += fr.FONT_HEIGHT;
        }
        
        // If no enemies are online, but we are in DRAG MODE, show a placeholder so the user can see where it is
        if (!foundEnemy && dragMode) {
            fr.drawStringWithShadow(EnumChatFormatting.GRAY + "[EnemyHUD Position]", xPos, yPos, 0xFFFFFF);
        }
    }

    private String getDistanceOrSpawn(EntityOtherPlayerMP player) {
        if (player == null || mc.thePlayer == null) return EnumChatFormatting.GRAY + "[?]";

        // Spawn detection logic
        if (player.posY > 113.0D || isInSpawnRegion(player)) {
            return EnumChatFormatting.GRAY + "[" + EnumChatFormatting.GREEN + "SPAWN" + EnumChatFormatting.GRAY + "]";
        }

        float dist = player.getDistanceToEntity(mc.thePlayer);
        String distStr = String.format("%.1f", dist);
        
        if (dist < 15.0F) return EnumChatFormatting.RED + distStr + "m";
        return EnumChatFormatting.GREEN + distStr + "m";
    }

    private boolean isInSpawnRegion(EntityOtherPlayerMP player) {
        return player.posX > -20 && player.posX < 20 && player.posZ > -20 && player.posZ < 20;
    }

    private String getMainGear(EntityOtherPlayerMP player) {
        ItemStack legs = player.inventory.armorInventory[1]; // Leggings slot
        if (legs != null && legs.hasTagCompound()) {
            String nbt = legs.getTagCompound().toString();
            if (nbt.contains("Regularity")) return EnumChatFormatting.DARK_RED + "Regularities";
            if (nbt.contains("Mind Assault")) return EnumChatFormatting.DARK_PURPLE + "Mind Assault";
            if (nbt.contains("Venom")) return EnumChatFormatting.DARK_PURPLE + "Venom";
            if (nbt.contains("Evil") || nbt.contains("Dark")) return EnumChatFormatting.DARK_PURPLE + "Darks";
        }
        return EnumChatFormatting.GRAY + "SHOP";
    }

    public void handleDrag(int mouseX, int mouseY) {
        if (!dragMode) return;
        
        // Check if mouse is within the general area of the HUD to start dragging
        if (Mouse.isButtonDown(0)) {
            if (!dragging) {
                // Check if mouse is roughly over the HUD (Header area)
                if (mouseX >= hudX && mouseX <= hudX + 100 && mouseY >= hudY && mouseY <= hudY + 20) {
                    dragging = true;
                    dragOffsetX = mouseX - hudX;
                    dragOffsetY = mouseY - hudY;
                }
            }
            if (dragging) {
                hudX = mouseX - dragOffsetX;
                hudY = mouseY - dragOffsetY;
            }
        } else {
            dragging = false;
        }
    }

    public static boolean isTarget(String name) {
        if (name == null) return false;
        return targetList.stream().anyMatch(t -> t.equalsIgnoreCase(name));
    }

    public static String getFormattedName(String name) {
        return EnumChatFormatting.RED + (name == null ? "null" : name) + EnumChatFormatting.RESET;
    }
}