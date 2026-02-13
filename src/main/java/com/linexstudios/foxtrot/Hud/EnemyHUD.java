package com.linexstudios.foxtrot.Hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class EnemyHUD {
    private final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = true;
    public static boolean debugMode = false;
    public static boolean notificationsEnabled = true;

    public static int hudX = 200;
    public static int hudY = 80;
    
    // Bounding box for dragging
    public int width = 0;
    public int height = 0;

    public static List<String> targetList = new ArrayList<>();

    public void render(boolean isEditing) {
        if (!enabled || mc.theWorld == null) return;

        FontRenderer fr = mc.fontRendererObj;
        int currentY = hudY;
        int maxWidth = fr.getStringWidth("Enemy List:");
        boolean foundEnemy = false;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!(player instanceof EntityOtherPlayerMP)) continue;
            EntityOtherPlayerMP other = (EntityOtherPlayerMP) player;
            if (!isTarget(other.getName())) continue;

            if (!foundEnemy) {
                fr.drawStringWithShadow(EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "Enemy List:", hudX, currentY, 0xFFFFFF);
                currentY += fr.FONT_HEIGHT + 2;
                foundEnemy = true;
            }

            String prefix = getPlayerPrefix(other);
            String name = EnumChatFormatting.RED + other.getName() + EnumChatFormatting.RESET;
            String gear = getShortEnchants(other);
            String dist = getDistanceOrSpawn(other);

            String fullLine = prefix + " " + name + EnumChatFormatting.GRAY + " - " + gear + EnumChatFormatting.GRAY + " - " + dist;
            fr.drawStringWithShadow(fullLine, hudX, currentY, 0xFFFFFF);

            int lineWidth = fr.getStringWidth(fullLine);
            if (lineWidth > maxWidth) maxWidth = lineWidth;
            currentY += fr.FONT_HEIGHT;
        }

        // Render Placeholder if empty but we are in Edit Mode
        if (!foundEnemy) {
            if (isEditing) {
                fr.drawStringWithShadow(EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "Enemy List:", hudX, currentY, 0xFFFFFF);
                currentY += fr.FONT_HEIGHT + 2;
                String placeholder = EnumChatFormatting.GRAY + "[Enemy HUD Position]";
                fr.drawStringWithShadow(placeholder, hudX, currentY, 0xFFFFFF);
                currentY += fr.FONT_HEIGHT;
                maxWidth = Math.max(maxWidth, fr.getStringWidth(placeholder));
            } else {
                this.width = 0; this.height = 0;
                return; 
            }
        }

        this.width = maxWidth;
        this.height = currentY - hudY;

        // Draw a subtle white box around it while dragging so you see the hitbox
        if (isEditing) {
            Gui.drawRect(hudX - 2, hudY - 2, hudX + width + 2, hudY + height + 2, 0x33FFFFFF);
        }
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= hudX - 2 && mouseX <= hudX + width + 2 && mouseY >= hudY - 2 && mouseY <= hudY + height + 2;
    }

    private String getPlayerPrefix(EntityOtherPlayerMP player) {
        String formatted = player.getDisplayName().getFormattedText();
        int index = formatted.indexOf(player.getName());
        if (index > 0) return formatted.substring(0, index).trim();
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
        ItemStack pants = player.inventory.armorInventory[1]; 
        if (pants != null && pants.hasTagCompound()) {
            NBTTagCompound extra = pants.getTagCompound().getCompoundTag("ExtraAttributes");
            if (extra != null && extra.hasKey("CustomEnchants")) {
                NBTTagList enchants = extra.getTagList("CustomEnchants", 10);
                List<String> shortNames = new ArrayList<>();
                for (int i = 0; i < enchants.tagCount(); i++) {
                    shortNames.add(formatEnchant(enchants.getCompoundTagAt(i).getString("Key")));
                }
                if (!shortNames.isEmpty()) {
                    return String.join(EnumChatFormatting.WHITE + "/", shortNames);
                }
            }
            if (pants.hasDisplayName() && pants.getDisplayName().contains("Dark Pants")) return EnumChatFormatting.DARK_PURPLE + "Darks";
        }
        return EnumChatFormatting.GRAY + "Shop";
    }

    public static String formatEnchant(String key) {
        if (key == null) return "";
        switch (key.toLowerCase()) {
            case "regularity": return EnumChatFormatting.DARK_RED + "Reg";
            case "respawn_absorption": return EnumChatFormatting.GOLD + "Abs";
            case "mirror": return EnumChatFormatting.WHITE + "Mirror";
            case "critically_funky": return EnumChatFormatting.AQUA + "Crit Funky";
            case "venom": case "combo_venom": return EnumChatFormatting.DARK_PURPLE + "Venom";
            case "mind_assault": return EnumChatFormatting.DARK_PURPLE + "Assaults";
            case "solitude": return EnumChatFormatting.DARK_GREEN + "Soli";
            case "protection": return EnumChatFormatting.BLUE + "Prot";
            case "fractional_reserve": return EnumChatFormatting.BLUE + "Frac";
            case "not_gladiator": return EnumChatFormatting.BLUE + "Glad";
            default:
                String[] words = key.split("_");
                if (words.length > 0 && words[0].length() > 0) {
                    String first = words[0];
                    return EnumChatFormatting.GRAY + first.substring(0, 1).toUpperCase() + first.substring(1);
                }
                return EnumChatFormatting.GRAY + key;
        }
    }

    public static boolean isTarget(String name) {
        return name != null && targetList.stream().anyMatch(t -> t.equalsIgnoreCase(name));
    }

    public static String getFormattedName(String name) {
        return EnumChatFormatting.RED + (name == null ? "null" : name) + EnumChatFormatting.RESET;
    }
}