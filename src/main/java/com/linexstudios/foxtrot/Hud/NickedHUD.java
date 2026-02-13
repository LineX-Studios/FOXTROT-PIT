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

public class NickedHUD {
    // Added instance so the Drag GUI can access this exact HUD
    public static final NickedHUD instance = new NickedHUD();

    private final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = true;
    public static boolean dragMode = false;
    public static boolean debugMode = false;
    public static boolean notificationsEnabled = true;

    public static int hudX = 10;
    public static int hudY = 80;
    
    // Hitbox dimensions for the new Drag GUI
    public int width = 0;
    public int height = 0;

    // BUG FIX: Added the Forge Event Subscriber so it actually renders in-game
    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        // Don't render normally if the Editor GUI is open (it renders itself)
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
                String nickedName = info.getGameProfile().getName();
                boolean isV2 = info.getGameProfile().getId().version() == 2;
                String realIGN = NickedManager.getResolvedIGN(nickedName);

                if (isV2 || (realIGN != null && !realIGN.isEmpty())) {
                    if (!foundNicked) {
                        fr.drawStringWithShadow(EnumChatFormatting.AQUA + "" + EnumChatFormatting.BOLD + "Nicked Players:", hudX, currentY, 0xFFFFFF);
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

                    String prefix = other != null ? getPlayerPrefix(other) : EnumChatFormatting.GRAY + "[?]";
                    String displayName = EnumChatFormatting.AQUA + nickedName;
                    if (realIGN != null) {
                        displayName += EnumChatFormatting.GRAY + " (" + EnumChatFormatting.YELLOW + realIGN + EnumChatFormatting.GRAY + ")";
                    }

                    String gear = other != null ? getShortEnchants(other) : EnumChatFormatting.GRAY + "Shop";
                    String dist = other != null ? getDistanceOrSpawn(other) : EnumChatFormatting.GRAY + "Far";

                    String fullLine = prefix + " " + displayName + EnumChatFormatting.GRAY + " - " + gear + EnumChatFormatting.GRAY + " - " + dist;
                    fr.drawStringWithShadow(fullLine, hudX, currentY, 0xFFFFFF);
                    
                    int lineWidth = fr.getStringWidth(fullLine);
                    if (lineWidth > maxWidth) maxWidth = lineWidth;
                    currentY += fr.FONT_HEIGHT;
                }
            }
        }

        // Render Placeholder if empty but we are in Edit Mode
        if (!foundNicked) {
            if (isEditing) {
                fr.drawStringWithShadow(EnumChatFormatting.AQUA + "" + EnumChatFormatting.BOLD + "Nicked Players:", hudX, currentY, 0xFFFFFF);
                currentY += fr.FONT_HEIGHT + 2;
                String placeholder = EnumChatFormatting.GRAY + "[NickedHUD Position]";
                fr.drawStringWithShadow(placeholder, hudX, currentY, 0xFFFFFF);
                currentY += fr.FONT_HEIGHT;
                maxWidth = Math.max(maxWidth, fr.getStringWidth(placeholder));
            } else {
                this.width = 0; 
                this.height = 0;
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
            if (pants.hasDisplayName() && pants.getDisplayName().contains("Dark Pants")) {
                return EnumChatFormatting.DARK_PURPLE + "Darks";
            }
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
}