package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class EnemyHUD {
    private final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = true;
    public static boolean dragMode = false;
    public static boolean debugMode = false;
    public static boolean notificationsEnabled = true;
    public static List<String> targetList = new ArrayList<>();

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (!enabled || event.type != RenderGameOverlayEvent.ElementType.TEXT || mc.theWorld == null) return;

        FontRenderer fr = mc.fontRendererObj;
        int xPos = 10;
        int yPos = 50;

        boolean foundEnemy = false;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!isTarget(player.getName())) continue;

            if (!foundEnemy) {
                String header = EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "Enemies List:";
                fr.drawStringWithShadow(header, xPos, yPos, 16777215);
                yPos += fr.FONT_HEIGHT + 1;
                foundEnemy = true;
            }

            String formattedName = player.getDisplayName().getFormattedText();
            String gear = getMainGear(player);
            String distanceDisplay = getDistanceOrSpawn(player);

            String fullLine = formattedName + EnumChatFormatting.WHITE + " in " +
                    gear + EnumChatFormatting.RESET + EnumChatFormatting.GRAY + " - " +
                    distanceDisplay;

            fr.drawStringWithShadow(fullLine, xPos, yPos, 16777215);
            yPos += fr.FONT_HEIGHT;
        }
    }

    private String getDistanceOrSpawn(EntityPlayer player) {
        // Detect spawn either by Y level or region bounds
        if (player.posY > 113.0D || isInSpawnRegion(player)) {
            return EnumChatFormatting.GRAY + "[" + EnumChatFormatting.GREEN + "SPAWN" + EnumChatFormatting.GRAY + "]";
        }

        float dist = player.getDistanceToEntity(mc.thePlayer);
        String distStr = String.format("%.2f", dist);

        if (dist < 15.0F) return EnumChatFormatting.RED + distStr;
        return EnumChatFormatting.GREEN + distStr;
    }

    private boolean isInSpawnRegion(EntityPlayer player) {
        // Example bounds for Hypixel Pit spawn platform, adjust as needed
        return player.posX > -20 && player.posX < 20 && player.posZ > -20 && player.posZ < 20;
    }

    private String getMainGear(EntityPlayer player) {
        ItemStack legs = player.inventory.armorInventory[1];
        if (legs != null && legs.hasTagCompound()) {
            String nbt = legs.getTagCompound().toString();
            if (nbt.contains("Regularity")) return EnumChatFormatting.DARK_RED + "Regularities";
            if (nbt.contains("Mind Assault")) return EnumChatFormatting.DARK_PURPLE + "Mind Assault";
            if (nbt.contains("Venom")) return EnumChatFormatting.DARK_PURPLE + "Venom";
            if (nbt.contains("Evil") || nbt.contains("Dark")) return EnumChatFormatting.DARK_PURPLE + "Darks";
        }
        return EnumChatFormatting.GRAY + "SHOP";
    }

    public static boolean isTarget(String name) {
        return targetList.stream().anyMatch(target -> target.equalsIgnoreCase(name));
    }

    public static String getFormattedName(String name) {
        return EnumChatFormatting.RED + name + EnumChatFormatting.RESET;
    }
}
