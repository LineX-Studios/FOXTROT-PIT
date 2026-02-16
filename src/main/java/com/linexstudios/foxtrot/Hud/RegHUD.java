package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Util.SpawnRegions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
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

public class RegHUD {
    public static final RegHUD instance = new RegHUD();
    private final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = true;
    public static int hudX = 10;
    public static int hudY = 180;

    public int width = 0;
    public int height = 0;

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
        int maxWidth = fr.getStringWidth("Regularity Players");
        boolean foundReg = false;

        Set<String> renderedPlayers = new HashSet<>();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!(player instanceof EntityOtherPlayerMP)) continue;
            EntityOtherPlayerMP other = (EntityOtherPlayerMP) player;

            // 1. Check if the player has Regularity pants and get their formatted enchants
            String enchantsDisplay = getRegEnchants(other);
            if (enchantsDisplay == null) continue; // They don't have Regularity, skip them!

            String name = other.getName();
            if (renderedPlayers.contains(name.toLowerCase())) continue;
            renderedPlayers.add(name.toLowerCase());

            if (!foundReg) {
                fr.drawStringWithShadow(EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Regularity Players:", hudX, currentY, 0xFFFFFF);
                currentY += fr.FONT_HEIGHT + 2;
                foundReg = true;
            }

            // 2. Format Display Name
            String displayName = other.getDisplayName().getFormattedText();
            displayName = EnumChatFormatting.DARK_RED + "[" + EnumChatFormatting.RED + "R" + EnumChatFormatting.DARK_RED + "] " + EnumChatFormatting.RESET + displayName;

            // 3. Get exact location / distance
            String dist = SpawnRegions.getLocationFormat(mc.thePlayer, other);

            // 4. Combine and Draw
            String fullLine = displayName + EnumChatFormatting.GRAY + " - " + enchantsDisplay + EnumChatFormatting.GRAY + " - " + dist;
            fr.drawStringWithShadow(fullLine, hudX, currentY, 0xFFFFFF);

            int lineWidth = fr.getStringWidth(fullLine);
            if (lineWidth > maxWidth) maxWidth = lineWidth;
            currentY += fr.FONT_HEIGHT;
        }

        // PLACEHOLDER text for when you are moving the HUD in the GUI
        if (!foundReg) {
            if (isEditing) {
                fr.drawStringWithShadow(EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Regularity Players:", hudX, currentY, 0xFFFFFF);
                currentY += fr.FONT_HEIGHT + 2;
                String placeholder = EnumChatFormatting.DARK_RED + "[" + EnumChatFormatting.RED + "R" + EnumChatFormatting.DARK_RED + "] " + EnumChatFormatting.GRAY + "[120] iTzRegPlayer" + EnumChatFormatting.GRAY + " - " + EnumChatFormatting.DARK_RED + "Reg" + EnumChatFormatting.WHITE + "/" + EnumChatFormatting.GOLD + "Abs" + EnumChatFormatting.WHITE + "/" + EnumChatFormatting.DARK_PURPLE + "GTGF" + EnumChatFormatting.GRAY + " - " + EnumChatFormatting.GREEN + "" + EnumChatFormatting.BOLD + "SPAWN";
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

        if (isEditing) Gui.drawRect(hudX - 2, hudY - 2, hudX + width + 2, hudY + height + 2, 0x44888888);
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= hudX - 2 && mouseX <= hudX + width + 2 && mouseY >= hudY - 2 && mouseY <= hudY + height + 2;
    }

    /**
     * Scans pants for the Regularity enchant.
     * Enforces that Regularity is ALWAYS first, and only whitelisted side-enchants are shown.
     */
    private String getRegEnchants(EntityOtherPlayerMP player) {
        ItemStack pants = player.inventory.armorInventory[1]; // Index 1 is leggings
        if (pants == null || !pants.hasTagCompound()) return null;

        NBTTagCompound extra = pants.getTagCompound().getCompoundTag("ExtraAttributes");
        if (extra == null || !extra.hasKey("CustomEnchants")) return null;

        NBTTagList enchants = extra.getTagList("CustomEnchants", 10);
        boolean hasRegularity = false;
        String regString = "";
        List<String> sideEnchants = new ArrayList<>();

        for (int i = 0; i < enchants.tagCount(); i++) {
            String key = enchants.getCompoundTagAt(i).getString("Key");
            String formatted = formatEnchant(key);

            // If the enchant is on our final list, it will not be null
            if (formatted != null) {
                if (key.equalsIgnoreCase("regularity")) {
                    hasRegularity = true;
                    regString = formatted;
                } else {
                    sideEnchants.add(formatted);
                }
            }
        }

        // Abort if they don't have regularity at all
        if (!hasRegularity) return null;

        // Combine the lists: Force 'Reg' to be the absolute first item
        List<String> finalEnchants = new ArrayList<>();
        finalEnchants.add(regString);
        finalEnchants.addAll(sideEnchants); // Adds the 1 or 2 other enchants after Reg

        // Joins them together like: Reg/Abs/GTGF
        return String.join(EnumChatFormatting.WHITE + "/", finalEnchants);
    }

    /**
     * The FINAL LIST of strictly whitelisted Regularity side-enchants.
     * Anything not on this list is completely ignored and hidden.
     */
    public static String formatEnchant(String key) {
        if (key == null) return null;

        switch (key.toLowerCase()) {
            case "regularity": return EnumChatFormatting.DARK_RED + "Reg";
            case "respawn_absorption": return EnumChatFormatting.GOLD + "Abs";
            case "mirror": return EnumChatFormatting.WHITE + "Mirror";
            case "critically_funky": return EnumChatFormatting.DARK_AQUA + "Crit Funky";
            case "solitude": return EnumChatFormatting.RED + "Soli";
            case "protection": return EnumChatFormatting.BLUE + "Prot";
            case "fractional_reserve": return EnumChatFormatting.BLUE + "Frac";
            case "not_gladiator": return EnumChatFormatting.BLUE + "Glad";
            case "hunt_the_hunter": return EnumChatFormatting.AQUA + "Hunter";
            case "peroxide": return EnumChatFormatting.RED + "Pero";
            case "assassin": return EnumChatFormatting.DARK_PURPLE + "Assasin";
            case "escape_pod": return EnumChatFormatting.DARK_RED + "Pods";
            case "phoenix": return EnumChatFormatting.GOLD + "Phoenix";
            case "retro_gravity_microcosm": return EnumChatFormatting.GOLD + "RGM";
            case "singularity": return EnumChatFormatting.RED + "singularity";
            case "gomraws_heart": return EnumChatFormatting.RED + "Gomraw";
            case "last_stand": return EnumChatFormatting.RED + "Last Stand";
            case "gotta_go_fast": return EnumChatFormatting.DARK_PURPLE + "GTGF";
            case "diamond_allergy": return EnumChatFormatting.AQUA + "Diamond Allergy";
            case "david_and_goliath": return EnumChatFormatting.YELLOW + "D&G";
            case "heigh_ho": return EnumChatFormatting.RED + "HeighHo";
            default: return null; // CRITICAL: This hides ANY enchant not explicitly on this list!
        }
    }
}