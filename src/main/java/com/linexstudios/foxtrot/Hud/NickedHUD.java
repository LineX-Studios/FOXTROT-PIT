package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Denick.NickedManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

public class NickedHUD {
    private final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = true;
    public static boolean debugMode = false;
    public static boolean notificationsEnabled = true;

    public static int hudX = 10;
    public static int hudY = 80;
    public static boolean dragMode = false;

    private static boolean dragging = false;
    private static int dragOffsetX, dragOffsetY;

    public void onRender(RenderGameOverlayEvent.Post event) {
        if (!enabled || event.type != RenderGameOverlayEvent.ElementType.TEXT || mc.theWorld == null) return;

        FontRenderer fr = mc.fontRendererObj;
        int xPos = hudX;
        int yPos = hudY;
        boolean foundNicked = false;

        NetHandlerPlayClient netHandler = mc.getNetHandler();
        if (netHandler != null) {
            for (NetworkPlayerInfo info : netHandler.getPlayerInfoMap()) {
                String nickedName = info.getGameProfile().getName();
                boolean isV2 = info.getGameProfile().getId().version() == 2;
                String realIGN = NickedManager.getResolvedIGN(nickedName);

                if (isV2 || (realIGN != null && !realIGN.isEmpty())) {
                    if (!foundNicked) {
                        fr.drawStringWithShadow(EnumChatFormatting.AQUA + "" + EnumChatFormatting.BOLD + "Nicked Players:", xPos, yPos, 0xFFFFFF);
                        yPos += fr.FONT_HEIGHT + 2;
                        foundNicked = true;
                    }

                    // Look for the entity in the world to get their armor/prestige
                    EntityOtherPlayerMP other = null;
                    for (Object obj : mc.theWorld.playerEntities) {
                        if (obj instanceof EntityOtherPlayerMP) {
                            EntityOtherPlayerMP p = (EntityOtherPlayerMP) obj;
                            if (p.getName().equalsIgnoreCase(nickedName)) {
                                other = p;
                                break;
                            }
                        }
                    }

                    // Extract prestige bracket [120]
                    String prefix = other != null ? getPlayerPrefix(other) : EnumChatFormatting.GRAY + "[?]";
                    
                    // Format Name and Real IGN
                    String displayName = EnumChatFormatting.AQUA + nickedName;
                    if (realIGN != null) {
                        displayName += EnumChatFormatting.GRAY + " (" + EnumChatFormatting.YELLOW + realIGN + EnumChatFormatting.GRAY + ")";
                    }

                    // Get formatted enchants (Reg/Abs/Mirror) and distance
                    String gear = other != null ? getShortEnchants(other) : EnumChatFormatting.GRAY + "Shop";
                    String dist = other != null ? getDistanceOrSpawn(other) : EnumChatFormatting.GRAY + "Far";

                    // Combine into final line
                    String fullLine = prefix + " " + displayName + EnumChatFormatting.GRAY + " - " + gear + EnumChatFormatting.GRAY + " - " + dist;
                    
                    fr.drawStringWithShadow(fullLine, xPos, yPos, 0xFFFFFF);
                    yPos += fr.FONT_HEIGHT;
                }
            }
        }

        if (!foundNicked && dragMode) {
            fr.drawStringWithShadow(EnumChatFormatting.AQUA + "" + EnumChatFormatting.BOLD + "Nicked Players:", xPos, yPos, 0xFFFFFF);
            fr.drawStringWithShadow(EnumChatFormatting.GRAY + "[Nicked Players Hud Position]", xPos, yPos + fr.FONT_HEIGHT + 2, 0xFFFFFF);
        }
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

    // --- REPLACED old getMainGear WITH getShortEnchants ---
    private String getShortEnchants(EntityOtherPlayerMP player) {
        ItemStack pants = player.inventory.armorInventory[1]; 
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
                    return String.join(EnumChatFormatting.WHITE + "/", shortNames);
                }
            }
            // Safe check for Dark Pants
            if (pants.hasDisplayName() && pants.getDisplayName().contains("Dark Pants")) {
                return EnumChatFormatting.DARK_PURPLE + "Darks";
            }
        }
        return EnumChatFormatting.GRAY + "Shop";
    }

    // --- ADDED FULL SWITCH STATEMENT FOR ENCHANTS ---
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
                // If enchant isn't listed above, automatically grab the first word
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
}