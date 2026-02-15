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
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class FriendsHUD {
    public static final FriendsHUD instance = new FriendsHUD();
    
    private final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = true;
    public static boolean debugMode = false;
    public static boolean notificationsEnabled = true;

    // Offset X so it doesn't render directly on top of EnemyHUD
    public static int hudX = 350;
    public static int hudY = 80;
    
    public int width = 0;
    public int height = 0;

    public static List<String> friendsList = new ArrayList<>();

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
        int maxWidth = fr.getStringWidth("Friends");
        boolean foundFriend = false;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!(player instanceof EntityOtherPlayerMP)) continue;
            EntityOtherPlayerMP other = (EntityOtherPlayerMP) player;
            if (!isFriend(other.getName())) continue;

            if (!foundFriend) {
                // Changed title to match the screenshot
                fr.drawStringWithShadow(EnumChatFormatting.GREEN + "Friends", hudX, currentY, 0xFFFFFF);
                currentY += fr.FONT_HEIGHT + 2;
                foundFriend = true;
            }

            // Grabs prestige bracket and rank color natively from Hypixel
            String displayName;
            String rawFormatted = other.getDisplayName().getFormattedText();
            int nameIndex = rawFormatted.indexOf(other.getName());
            if (nameIndex >= 0) {
                displayName = rawFormatted.substring(0, nameIndex + other.getName().length());
            } else {
                displayName = EnumChatFormatting.GRAY + "[?] " + EnumChatFormatting.GREEN + other.getName();
            }

            // NEW: Prepend the green [F] tag
            displayName = EnumChatFormatting.GREEN + "[F] " + EnumChatFormatting.RESET + displayName;

            String gear = getShortEnchants(other);
            String dist = getDistanceOrSpawn(other);

            String fullLine = displayName + EnumChatFormatting.GRAY + " - " + gear + " " + dist;
            fr.drawStringWithShadow(fullLine, hudX, currentY, 0xFFFFFF);

            int lineWidth = fr.getStringWidth(fullLine);
            if (lineWidth > maxWidth) maxWidth = lineWidth;
            currentY += fr.FONT_HEIGHT;
        }

        if (!foundFriend) {
            if (isEditing) {
                fr.drawStringWithShadow(EnumChatFormatting.GREEN + "Friends", hudX, currentY, 0xFFFFFF);
                currentY += fr.FONT_HEIGHT + 2;
                String placeholder = EnumChatFormatting.GREEN + "[F] " + EnumChatFormatting.GRAY + "[96] Placeholder " + EnumChatFormatting.GREEN + "[SPAWN]";
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
            Gui.drawRect(hudX - 2, hudY - 2, hudX + width + 2, hudY + height + 2, 0x44888888);
        }
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= hudX - 2 && mouseX <= hudX + width + 2 && mouseY >= hudY - 2 && mouseY <= hudY + height + 2;
    }

    // NEW: Updated to use the green bracket format from your screenshot
    private String getDistanceOrSpawn(EntityOtherPlayerMP player) {
        if (player.posY > 113.0D || (player.posX > -20 && player.posX < 20 && player.posZ > -20 && player.posZ < 20)) {
            return EnumChatFormatting.GREEN + "[SPAWN]";
        }
        float dist = player.getDistanceToEntity(mc.thePlayer);
        return EnumChatFormatting.GREEN + "[" + String.format("%.0f", dist) + "m]";
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
                    if (formatted != null) {
                        shortNames.add(formatted);
                    }
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
            default:
                return null;
        }
    }

    public static boolean isFriend(String name) {
        return name != null && friendsList.stream().anyMatch(t -> t.equalsIgnoreCase(name));
    }

    public static String getFormattedName(String name) {
        return EnumChatFormatting.GREEN + (name == null ? "null" : name) + EnumChatFormatting.RESET;
    }
}