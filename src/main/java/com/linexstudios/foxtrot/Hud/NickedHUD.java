package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Denick.NickedManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NickedHUD {
    public static final NickedHUD instance = new NickedHUD();
    private final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = true;
    public static boolean debugMode = false;

    public static int hudX = 10;
    public static int hudY = 80;
    
    public int width = 0;
    public int height = 0;

    public static List<String> nickedPlayers = new ArrayList<>();

    // Regex to match Hypixel Ranks like [VIP], [MVP+], [MVP++], [YOUTUBE], [ADMIN], etc.
    // It looks for uppercase letters and plus signs inside brackets.
    private static final Pattern RANK_PATTERN = Pattern.compile("\\[(VIP|MVP|YOUTUBE|OWNER|ADMIN|HELPER|MOD|EVENTS|BUILD TEAM|ALUMNI|GM|APPLE|MOJANG)[+]*\\] ");

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (mc.currentScreen instanceof EditHUDGui) return;
        render(false);
    }

    public void render(boolean isEditing) {
        if (!enabled || mc.theWorld == null) return;

        FontRenderer fr = mc.fontRendererObj;
        int currentY = hudY;
        int maxWidth = fr.getStringWidth("Nicked Players:");
        boolean foundNicked = false;

        NetHandlerPlayClient netHandler = mc.getNetHandler();
        if (netHandler != null) {
            for (NetworkPlayerInfo info : netHandler.getPlayerInfoMap()) {
                if (info.getGameProfile().getId().version() == 2) continue;
                
                String nickedName = info.getGameProfile().getName();
                if (nickedName.startsWith("§")) continue;

                String realIGN = NickedManager.getResolvedIGN(nickedName);
                boolean isManuallyScraped = nickedPlayers.contains(nickedName.toLowerCase());

                if ((realIGN != null && !realIGN.isEmpty()) || isManuallyScraped) {
                    if (!foundNicked) {
                        fr.drawStringWithShadow(EnumChatFormatting.DARK_AQUA + "" + EnumChatFormatting.BOLD + "Nicked Players:", hudX, currentY, 0xFFFFFF);
                        currentY += fr.FONT_HEIGHT + 2;
                        foundNicked = true;
                    }

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

                    String fullPrefixAndName;
                    if (other != null) {
                        fullPrefixAndName = other.getDisplayName().getFormattedText();
                    } else if (info.getDisplayName() != null) {
                        fullPrefixAndName = info.getDisplayName().getFormattedText();
                    } else {
                        fullPrefixAndName = EnumChatFormatting.GRAY + "[?] " + EnumChatFormatting.AQUA + nickedName;
                    }

                    // BUG FIX: Strip the MVP++/YOUTUBER ranks but keep the Prestige
                    fullPrefixAndName = stripRank(fullPrefixAndName);

                    String displayRealIGN = (realIGN != null && !realIGN.isEmpty()) ? realIGN : "Scraping...";
                    String finalDisplayName = fullPrefixAndName + EnumChatFormatting.GRAY + " (" + EnumChatFormatting.YELLOW + displayRealIGN + EnumChatFormatting.GRAY + ")";

                    String gear = other != null ? getShortEnchants(other) : EnumChatFormatting.GRAY + "Shop";
                    String dist = other != null ? getDistanceOrSpawn(other) : EnumChatFormatting.GRAY + "Far";

                    String fullLine = finalDisplayName + EnumChatFormatting.GRAY + " - " + gear + EnumChatFormatting.GRAY + " - " + dist;
                    fr.drawStringWithShadow(fullLine, hudX, currentY, 0xFFFFFF);
                    
                    int lineWidth = fr.getStringWidth(fullLine);
                    if (lineWidth > maxWidth) maxWidth = lineWidth;
                    currentY += fr.FONT_HEIGHT;
                }
            }
        }

        if (!foundNicked) {
            if (isEditing) {
                fr.drawStringWithShadow(EnumChatFormatting.DARK_AQUA + "" + EnumChatFormatting.BOLD + "Nicked Players:", hudX, currentY, 0xFFFFFF);
                currentY += fr.FONT_HEIGHT + 2;
                String placeholder = EnumChatFormatting.GRAY + "[Nicked Players HUD Position]";
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

        if (isEditing) {
            Gui.drawRect(hudX - 2, hudY - 2, hudX + width + 2, hudY + height + 2, 0x55A020F0);
        }
    }

    /**
     * Removes Hypixel rank prefixes (like [MVP++]) from the string while keeping Prestige.
     */
    private String stripRank(String name) {
        if (name == null || name.isEmpty()) return name;
        // Strip color codes temporarily to check for rank words, or just use regex on the raw string
        Matcher matcher = RANK_PATTERN.matcher(EnumChatFormatting.getTextWithoutFormattingCodes(name));
        if (matcher.find()) {
            String rankFound = matcher.group();
            // We find the rank in the unformatted text, then remove it from the formatted text
            return name.replace(rankFound, "");
        }
        return name;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= hudX - 2 && mouseX <= hudX + width + 2 && mouseY >= hudY - 2 && mouseY <= hudY + height + 2;
    }

    private String getDistanceOrSpawn(EntityOtherPlayerMP player) {
        if (player.posY > 113.0D || (player.posX > -20 && player.posX < 20 && player.posZ > -20 && player.posZ < 20)) {
            return EnumChatFormatting.GREEN + "Spawn";
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
                    String formatted = formatEnchant(enchants.getCompoundTagAt(i).getString("Key"));
                    if (formatted != null) shortNames.add(formatted);
                }
                if (!shortNames.isEmpty()) return String.join(EnumChatFormatting.WHITE + "/", shortNames);
            }
            if (pants.hasDisplayName() && pants.getDisplayName().contains("Dark Pants")) return EnumChatFormatting.DARK_PURPLE + "Darks";
        }
        return EnumChatFormatting.GRAY + "Shop";
    }

    public static String formatEnchant(String key) {
        if (key == null) return null;
        switch (key.toLowerCase()) {
            case "regularity": return EnumChatFormatting.DARK_RED + "Reg";
            case "respawn_absorption": return EnumChatFormatting.GOLD + "Abs";
            case "mirror": return EnumChatFormatting.WHITE + "Mirror";
            case "critically_funky": return EnumChatFormatting.DARK_BLUE + "Crit Funky";
            case "venom": case "combo_venom": return EnumChatFormatting.DARK_PURPLE + "Venom";
            case "mind_assault": return EnumChatFormatting.DARK_PURPLE + "Assaults";
            case "solitude": return EnumChatFormatting.RED + "Soli";
            case "protection": return EnumChatFormatting.BLUE + "Prot";
            case "fractional_reserve": return EnumChatFormatting.BLUE + "Frac";
            case "not_gladiator": return EnumChatFormatting.BLUE + "Glad";
            default: return null;
        }
    }
}