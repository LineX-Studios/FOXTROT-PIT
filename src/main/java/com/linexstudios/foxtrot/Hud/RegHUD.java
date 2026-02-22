package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Util.SpawnRegions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
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

public class RegHUD extends DraggableHUD {
    public static final RegHUD instance = new RegHUD();
    private final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = true;

    // Set the default position here
    public RegHUD() { super("Regularity List", 10, 180); }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (mc.currentScreen instanceof EditHUDGui) return;
        if (mc.currentScreen instanceof HUDSettingsGui) return;
        render(false, 0, 0); // Re-routes to the DraggableHUD render method
    }

    @Override
    public void draw(boolean isEditing) {
        if (!enabled || mc.theWorld == null) return;

        FontRenderer fr = mc.fontRendererObj;
        int currentY = 0; // Local Y starts at 0 now
        int maxWidth = fr.getStringWidth("Regularity Players");
        boolean foundReg = false;

        Set<String> renderedPlayers = new HashSet<>();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!(player instanceof EntityOtherPlayerMP)) continue;
            EntityOtherPlayerMP other = (EntityOtherPlayerMP) player;

            String enchantsDisplay = getRegEnchants(other);
            if (enchantsDisplay == null) continue;

            String name = other.getName();
            if (renderedPlayers.contains(name.toLowerCase())) continue;
            renderedPlayers.add(name.toLowerCase());

            if (!foundReg) {
                fr.drawStringWithShadow(EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Regularity Players:", 0, currentY, 0xFFFFFF);
                currentY += fr.FONT_HEIGHT + 2;
                foundReg = true;
            }

            String displayName = other.getDisplayName().getFormattedText();
            displayName = EnumChatFormatting.DARK_RED + "[" + EnumChatFormatting.RED + "R" + EnumChatFormatting.DARK_RED + "] " + EnumChatFormatting.RESET + displayName;

            String dist = SpawnRegions.getLocationFormat(mc.thePlayer, other);

            String fullLine = displayName + EnumChatFormatting.GRAY + " - " + enchantsDisplay + EnumChatFormatting.GRAY + " - " + dist;
            fr.drawStringWithShadow(fullLine, 0, currentY, 0xFFFFFF);

            int lineWidth = fr.getStringWidth(fullLine);
            if (lineWidth > maxWidth) maxWidth = lineWidth;
            currentY += fr.FONT_HEIGHT;
        }

        if (!foundReg) {
            if (isEditing) {
                fr.drawStringWithShadow(EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Regularity Players:", 0, currentY, 0xFFFFFF);
                currentY += fr.FONT_HEIGHT + 2;
                String placeholder = EnumChatFormatting.DARK_RED + "[" + EnumChatFormatting.RED + "R" + EnumChatFormatting.DARK_RED + "] " + EnumChatFormatting.GRAY + "[120] Player" + EnumChatFormatting.GRAY + " - " + EnumChatFormatting.DARK_RED + "Reg" + EnumChatFormatting.WHITE + "/" + EnumChatFormatting.WHITE + "Mirror" + EnumChatFormatting.GRAY + " - " + EnumChatFormatting.GREEN + "" + EnumChatFormatting.BOLD + "SPAWN";
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

    private String getRegEnchants(EntityOtherPlayerMP player) {
        ItemStack pants = player.inventory.armorInventory[1];
        if (pants == null || !pants.hasTagCompound()) return null;

        NBTTagCompound extra = pants.getTagCompound().getCompoundTag("ExtraAttributes");
        if (extra == null || !extra.hasKey("CustomEnchants")) return null;

        NBTTagList enchants = extra.getTagList("CustomEnchants", 10);
        boolean hasRegularity = false;
        String regString = "";
        List<String> sideEnchants = new ArrayList<>();

        for (int i = 0; i < enchants.tagCount(); i++) {
            String key = enchants.getCompoundTagAt(i).getString("Key").trim();
            String formatted = formatEnchant(key);

            if (formatted != null) {
                if (key.equalsIgnoreCase("regularity")) {
                    hasRegularity = true;
                    regString = formatted;
                } else {
                    sideEnchants.add(formatted);
                }
            }
        }

        if (!hasRegularity) return null;

        List<String> finalEnchants = new ArrayList<>();
        finalEnchants.add(regString);
        finalEnchants.addAll(sideEnchants);

        return String.join(EnumChatFormatting.WHITE + "/", finalEnchants);
    }

    public static String formatEnchant(String rawKey) {
        if (rawKey == null) return null;

        String key = rawKey.trim().toLowerCase();

        switch (key) {
            case "regularity": return EnumChatFormatting.DARK_RED + "Reg";
            
            // Re-mapped to properly detect Mirror NBT
            case "immune_true_damage": 
            case "mirror":
            case "reflection":
                return EnumChatFormatting.WHITE + "Mirror";
                
            case "respawn_absorption": 
            case "respawn_with_absorption":
                return EnumChatFormatting.GOLD + "Abs";
                
            case "critically_funky": return EnumChatFormatting.DARK_AQUA + "Crit Funky";
            case "solitude": return EnumChatFormatting.RED + "Soli";
            case "protection": return EnumChatFormatting.BLUE + "Prot";
            case "fractional_reserve": return EnumChatFormatting.BLUE + "Frac";
            
            // Added mapped version of Not Gladiator
            case "less_damage_nearby_players":
            case "not_gladiator": 
                return EnumChatFormatting.BLUE + "Glad";
                
            case "hunt_the_hunter": return EnumChatFormatting.GOLD + "Hunter";
            
            // Added mapped version of Peroxide
            case "regen_when_hit":
            case "peroxide": 
                return EnumChatFormatting.RED + "Pero";
                
            case "assassin": return EnumChatFormatting.LIGHT_PURPLE + "Assasin";
            case "escape_pod": return EnumChatFormatting.RED + "Pods";
            case "phoenix": return EnumChatFormatting.GOLD + "Phoenix";
            
            // Added mapped version of RGM
            case "rgm":
            case "retro_gravity_microcosm": 
                return EnumChatFormatting.RED + "RGM";
                
            case "singularity": return EnumChatFormatting.RED + "Sing";
            
            // Added mapped version of Gomraws Heart
            case "regen_when_ooc":
            case "gomraws_heart": 
                return EnumChatFormatting.RED + "Gomraw";
                
            // Added mapped version of Last Stand
            case "resistance_when_low":
            case "last_stand": 
                return EnumChatFormatting.AQUA + "Stand";
                
            // Added mapped version of Gotta Go Fast
            case "perma_speed":
            case "gotta_go_fast": 
                return EnumChatFormatting.DARK_PURPLE + "GTGF";
                
            case "diamond_allergy": return EnumChatFormatting.AQUA + "Diamond Allergy";
            
            // Added mapped version of David & Goliath
            case "less_damage_vs_bounties":
            case "david_and_goliath": 
                return EnumChatFormatting.YELLOW + "D&G";
                
            case "heigh_ho": return EnumChatFormatting.RED + "HeighHo";
            default: return null;
        }
    }
}