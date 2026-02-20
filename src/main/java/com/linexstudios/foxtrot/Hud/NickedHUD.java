package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Denick.CacheManager;
import com.linexstudios.foxtrot.Denick.NickedManager;
import com.linexstudios.foxtrot.Util.SpawnRegions;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NickedHUD extends DraggableHUD {
    public static final NickedHUD instance = new NickedHUD();
    private final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = true;

    public static List<String> nickedPlayers = new ArrayList<>();

    private static final Pattern PREFIX_STRIP_PATTERN = Pattern.compile("^\\[.*?\\]\\s+");

    public NickedHUD() { super("Nicked List", 10, 80); }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (mc.currentScreen instanceof EditHUDGui) return;
        if (mc.currentScreen instanceof HUDSettingsGui) return;
        render(false, 0, 0);
    }

    @Override
    public void draw(boolean isEditing) {
        if (!enabled || mc.theWorld == null) return;

        FontRenderer fr = mc.fontRendererObj;
        int currentY = 0;
        int maxWidth = fr.getStringWidth("Nicked Players");
        boolean foundNicked = false;

        Set<String> renderedNicks = new HashSet<>();

        NetHandlerPlayClient netHandler = mc.getNetHandler();
        if (netHandler != null) {
            for (NetworkPlayerInfo info : netHandler.getPlayerInfoMap()) {
                if (info == null || info.getGameProfile() == null || info.getGameProfile().getId() == null) continue;
                if (info.getGameProfile().getId().version() != 1) continue;

                String nickedName = info.getGameProfile().getName();
                if (nickedName.startsWith("§")) continue;

                if (renderedNicks.contains(nickedName.toLowerCase())) continue;
                renderedNicks.add(nickedName.toLowerCase());

                String realIGN = CacheManager.getFromCache(nickedName);
                if (realIGN == null) {
                    realIGN = NickedManager.getResolvedIGN(nickedName);
                }

                if (!foundNicked) {
                    fr.drawStringWithShadow(EnumChatFormatting.DARK_AQUA + "" + EnumChatFormatting.BOLD + "Nicked Players:", 0, currentY, 0xFFFFFF);
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

                String nickDisplay;
                if (other != null) {
                    nickDisplay = other.getDisplayName().getFormattedText();
                } else if (info.getDisplayName() != null) {
                    nickDisplay = info.getDisplayName().getFormattedText();
                } else {
                    nickDisplay = EnumChatFormatting.GRAY + "[?] " + EnumChatFormatting.AQUA + nickedName;
                }

                String cleanedRealIGN = "Scraping";
                if (realIGN != null && !realIGN.isEmpty()) {
                    // Strip the formatting codes from the status message
                    String unformattedIGN = EnumChatFormatting.getTextWithoutFormattingCodes(realIGN).trim();
                    
                    if (unformattedIGN.equalsIgnoreCase("Failed")) cleanedRealIGN = "Failed";
                    else if (unformattedIGN.equalsIgnoreCase("No Nonce")) cleanedRealIGN = "No Nonce";
                    else if (unformattedIGN.equalsIgnoreCase("Scraping") || unformattedIGN.equalsIgnoreCase("Scraping...")) cleanedRealIGN = "Scraping";
                    else cleanedRealIGN = stripAllPrefixes(realIGN); // It's an actual name, process it
                }

                String finalDisplayName = EnumChatFormatting.DARK_AQUA + "[" + EnumChatFormatting.AQUA + "N" + EnumChatFormatting.DARK_AQUA + "] " + EnumChatFormatting.RESET + nickDisplay + " " + EnumChatFormatting.YELLOW + "(" + cleanedRealIGN + ")";

                String gear = other != null ? getShortEnchants(other) : EnumChatFormatting.GRAY + "Shop";
                String dist = other != null ? SpawnRegions.getLocationFormat(mc.thePlayer, other) : EnumChatFormatting.GRAY + "Far";

                String fullLine = finalDisplayName + EnumChatFormatting.GRAY + " - " + gear + EnumChatFormatting.GRAY + " - " + dist;
                fr.drawStringWithShadow(fullLine, 0, currentY, 0xFFFFFF);

                int lineWidth = fr.getStringWidth(fullLine);
                if (lineWidth > maxWidth) maxWidth = lineWidth;
                currentY += fr.FONT_HEIGHT;
            }
        }

        if (!foundNicked) {
            if (isEditing) {
                fr.drawStringWithShadow(EnumChatFormatting.DARK_AQUA + "" + EnumChatFormatting.BOLD + "Nicked Players:", 0, currentY, 0xFFFFFF);
                currentY += fr.FONT_HEIGHT + 2;
                String placeholder = EnumChatFormatting.DARK_AQUA + "[" + EnumChatFormatting.AQUA + "N" + EnumChatFormatting.DARK_AQUA + "] " + EnumChatFormatting.GRAY + "[120] Player" + EnumChatFormatting.GRAY + " - " + EnumChatFormatting.GREEN + "" + EnumChatFormatting.BOLD + "SPAWN";
                fr.drawStringWithShadow(placeholder, 0, currentY, 0xFFFFFF);
                currentY += fr.FONT_HEIGHT;
                maxWidth = Math.max(maxWidth, fr.getStringWidth(placeholder));
            } else {
                this.width = 0; this.height = 0;
                return;
            }
        }

        this.width = maxWidth;
        this.height = currentY;
    }

    private String stripAllPrefixes(String input) {
        if (input == null || input.isEmpty()) return input;
        String plain = EnumChatFormatting.getTextWithoutFormattingCodes(input);
        Matcher matcher = PREFIX_STRIP_PATTERN.matcher(plain);
        while (matcher.find()) {
            plain = plain.substring(matcher.end());
            matcher = PREFIX_STRIP_PATTERN.matcher(plain);
        }
        return plain.trim();
    }

    private String getShortEnchants(EntityOtherPlayerMP player) {
        ItemStack pants = player.inventory.armorInventory[1];
        if (pants != null) {
            if (pants.hasTagCompound()) {
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
                if (pants.hasDisplayName() && pants.getDisplayName().contains("Dark Pants")) return EnumChatFormatting.DARK_PURPLE + "DARKS";
            }
            if (pants.getItem() == net.minecraft.init.Items.diamond_leggings) {
                return EnumChatFormatting.AQUA + "" + EnumChatFormatting.BOLD + "DIAMOND";
            }
        }
        return EnumChatFormatting.GRAY + "" + EnumChatFormatting.BOLD + "SHOP";
    }

    public static String formatEnchant(String key) {
        if (key == null) return null;
        switch (key.toLowerCase()) {
            case "regularity": return EnumChatFormatting.DARK_RED + "Reg";
            case "respawn_absorption": return EnumChatFormatting.GOLD + "Abs";
            case "mirror": return EnumChatFormatting.WHITE + "Mirror";
            case "critically_funky": return EnumChatFormatting.DARK_AQUA + "Crit Funky";
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