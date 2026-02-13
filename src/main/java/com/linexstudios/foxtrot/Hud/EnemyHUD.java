package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

public class EnemyHUD {
    private final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = true;
    public static boolean dragMode = false;
    public static boolean debugMode = false;
    public static boolean notificationsEnabled = true;

    public static int hudX = 200;
    public static int hudY = 80;

    private static boolean dragging = false;
    private static int dragOffsetX;
    private static int dragOffsetY;

    public static List<String> targetList = new ArrayList<>();

    public void onRender(RenderGameOverlayEvent.Post event) {
        if (!enabled || event.type != RenderGameOverlayEvent.ElementType.TEXT || mc.theWorld == null) return;

        FontRenderer fr = mc.fontRendererObj;
        int xPos = hudX;
        int yPos = hudY;
        boolean foundEnemy = false;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!(player instanceof EntityOtherPlayerMP)) continue;
            EntityOtherPlayerMP other = (EntityOtherPlayerMP) player;
            if (!isTarget(other.getName())) continue;

            if (!foundEnemy) {
                fr.drawStringWithShadow(EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "EnemY List:", xPos, yPos, 0xFFFFFF);
                yPos += fr.FONT_HEIGHT + 2;
                foundEnemy = true;
            }

            String prefix = getPlayerPrefix(other);
            String name = EnumChatFormatting.RED + other.getName() + EnumChatFormatting.RESET;
            String gear = getShortEnchants(other);
            String dist = getDistanceOrSpawn(other);

            // Format: [120] PlayerName - Reg/Abs/Mirror - 14m
            String fullLine = prefix + " " + name + EnumChatFormatting.GRAY + " - " + gear + EnumChatFormatting.GRAY + " - " + dist;

            fr.drawStringWithShadow(fullLine, xPos, yPos, 0xFFFFFF);
            yPos += fr.FONT_HEIGHT;
        }

        if (!foundEnemy && dragMode) {
            fr.drawStringWithShadow(EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "Enemy List:", xPos, yPos, 0xFFFFFF);
            fr.drawStringWithShadow(EnumChatFormatting.GRAY + "[Enemy list Hud Position]", xPos, yPos + fr.FONT_HEIGHT + 2, 0xFFFFFF);
        }
    }

    // Extracts the [120] prestige bracket that Hypixel sends automatically
    private String getPlayerPrefix(EntityOtherPlayerMP player) {
        String formatted = player.getDisplayName().getFormattedText();
        int index = formatted.indexOf(player.getName());
        if (index > 0) {
            return formatted.substring(0, index).trim();
        }
        return EnumChatFormatting.GRAY + "[?]";
    }

    private String getDistanceOrSpawn(EntityOtherPlayerMP player) {
        if (player.posY > 113.0D || (player.posX > -20 && player.posX < 20 && player.posZ > -20 && player.posZ < 20)) {
            return EnumChatFormatting.RED + "Spawn";
        }
        float dist = player.getDistanceToEntity(mc.thePlayer);
        return EnumChatFormatting.RED.toString() + String.format("%.0f", dist) + "m";
    }

    private String getShortEnchants(EntityOtherPlayerMP player) {
        ItemStack pants = player.inventory.armorInventory[1]; // Leggings
        if (pants != null && pants.hasTagCompound()) {
            NBTTagCompound extra = pants.getTagCompound().getCompoundTag("ExtraAttributes");
            if (extra != null && extra.hasKey("CustomEnchants")) {
                NBTTagList enchants = extra.getTagList("CustomEnchants", 10);
                List<String> shortNames = new ArrayList<>();
                
                for (int i = 0; i < enchants.tagCount(); i++) {
                    String key = enchants.getCompoundTagAt(i).getString("Key");
                    shortNames.add(formatEnchant(key));
                }
                
                if (!shortNames.isEmpty()) {
                    // Joins them with a white slash: Reg/Mirror/Abs
                    return String.join(EnumChatFormatting.WHITE + "/", shortNames);
                }
            }
            if (pants.getDisplayName().contains("Dark Pants")) return EnumChatFormatting.DARK_PURPLE + "Darks";
        }
        return EnumChatFormatting.GRAY + "Shop";
    }

    // Maps the NBT ID to the Short Name and Color
    public static String formatEnchant(String key) {
        if (key == null) return "";
        switch (key.toLowerCase()) {
            case "regularity": return EnumChatFormatting.DARK_RED + "Reg";
            case "respawn_absorption": return EnumChatFormatting.GOLD + "Abs";
            case "mirror": return EnumChatFormatting.WHITE + "Mirror";
            case "critically_funky": return EnumChatFormatting.AQUA + "Crit Funky";
            case "venom": case "combo_venom": return EnumChatFormatting.DARK_PURPLE + "Venom";
            case "mind_assault": return EnumChatFormatting.DARK_PURPLE + "Assaults";
            case "executioner": return EnumChatFormatting.RED + "Exec";
            case "billionaire": return EnumChatFormatting.GOLD + "Bill";
            case "gamble": return EnumChatFormatting.LIGHT_PURPLE + "Gam";
            case "solitude": return EnumChatFormatting.DARK_GREEN + "Soli";
            case "protection": return EnumChatFormatting.BLUE + "Prot";
            case "fractional_reserve": return EnumChatFormatting.BLUE + "Frac";
            case "not_gladiator": return EnumChatFormatting.BLUE + "Glad";
            default:
                // If enchant isn't recognized, just grab the first word
                String[] words = key.split("_");
                if (words.length > 0 && words[0].length() > 0) {
                    String first = words[0];
                    return EnumChatFormatting.GRAY + first.substring(0, 1).toUpperCase() + first.substring(1);
                }
                return EnumChatFormatting.GRAY + key;
        }
    }

    public void handleDrag(int mouseX, int mouseY) {
        if (!dragMode) return;
        boolean isHovered = mouseX >= hudX && mouseX <= hudX + 100 && mouseY >= hudY && mouseY <= hudY + 20;

        if (Mouse.isButtonDown(0)) {
            if (!dragging && isHovered) {
                dragging = true;
                dragOffsetX = mouseX - hudX;
                dragOffsetY = mouseY - hudY;
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
        return name != null && targetList.stream().anyMatch(t -> t.equalsIgnoreCase(name));
    }
}