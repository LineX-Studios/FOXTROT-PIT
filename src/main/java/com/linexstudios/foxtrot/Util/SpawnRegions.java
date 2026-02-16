package com.linexstudios.foxtrot.Util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SpawnRegions {

    public enum PitMap {
        ELEMENTS, CASTLE, CORALS, GENESIS, FOUR_SEASONS
    }

    public static class BoundingBox {
        public final int minX, minY, minZ, maxX, maxY, maxZ;

        public BoundingBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
        }

        public boolean contains(double x, double y, double z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
    }

    // ==========================================
    //          GENESIS FACTION REGIONS
    // ==========================================

    /** ANGEL FACTION SPAWN (Aqua) */
    public static final List<BoundingBox> GENESIS_ANGEL_SPAWN = Collections.singletonList(
            new BoundingBox(-20, 0, -20, 20, 255, 20)
    );

    /** DEMON FACTION SPAWN (Red) */
    public static final List<BoundingBox> GENESIS_DEMON_SPAWN = Arrays.asList(
            new BoundingBox(-160, 0, -160, -20, 255, 0),
            new BoundingBox(-20, 0, -160, 0, 255, -20)
    );

    public static final List<BoundingBox> GENESIS_SPAWN = Collections.singletonList(
            new BoundingBox(-20, 86, -20, 20, 223, 20)
    );

    // ==========================================
    //             OTHER MAP SPAWNS
    // ==========================================

    public static final List<BoundingBox> FOUR_SEASONS_SPAWN = Collections.singletonList(
            new BoundingBox(-23, 92, -23, 23, 255, 23)
    );

    public static final List<BoundingBox> ELEMENTS_SPAWN = Collections.singletonList(
            new BoundingBox(-20, 114, -20, 20, 255, 20)
    );

    public static final List<BoundingBox> CORALS_SPAWN = Collections.singletonList(
            new BoundingBox(-20, 215, -20, 20, 255, 20)
    );

    public static final List<BoundingBox> CASTLE_SPAWN = Arrays.asList(
            new BoundingBox(-300, 0, -300, 35, 100, 35),
            new BoundingBox(-120, 0, -120, 35, 100, 35)
    );

    // ==========================================
    //             CORE LOGIC
    // ==========================================

    public static PitMap getCurrentMap() {
        long epoch = 1768341600000L;
        long weekMs = 604800000L;

        long diff = System.currentTimeMillis() - epoch;
        if (diff < 0) {
            diff = (diff % (weekMs * 5)) + (weekMs * 5);
        }

        int mapIndex = (int) ((diff / weekMs) % 5);

        switch (mapIndex) {
            case 0: return PitMap.ELEMENTS;
            case 1: return PitMap.CASTLE;
            case 2: return PitMap.CORALS;
            case 3: return PitMap.GENESIS;
            case 4: return PitMap.FOUR_SEASONS;
            default: return PitMap.FOUR_SEASONS;
        }
    }

    public static String getRegionString(EntityPlayer player) {
        if (player == null) return "";

        double x = player.posX;
        double y = player.posY;
        double z = player.posZ;

        PitMap currentMap = getCurrentMap();

        switch (currentMap) {
            case GENESIS:
                if (isInside(x, y, z, GENESIS_SPAWN)) return EnumChatFormatting.GREEN + "" + EnumChatFormatting.BOLD + "SPAWN";
                if (isInside(x, y, z, GENESIS_ANGEL_SPAWN)) return EnumChatFormatting.AQUA + "" + EnumChatFormatting.BOLD + "ANGEL";
                if (isInside(x, y, z, GENESIS_DEMON_SPAWN)) return EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "DEMON";
                break;
            case FOUR_SEASONS:
                if (isInside(x, y, z, FOUR_SEASONS_SPAWN)) return EnumChatFormatting.GREEN + "" + EnumChatFormatting.BOLD + "SPAWN";
                break;
            case ELEMENTS:
                if (isInside(x, y, z, ELEMENTS_SPAWN)) return EnumChatFormatting.GREEN + "" + EnumChatFormatting.BOLD + "SPAWN";
                break;
            case CORALS:
                if (isInside(x, y, z, CORALS_SPAWN)) return EnumChatFormatting.GREEN + "" + EnumChatFormatting.BOLD + "SPAWN";
                break;
            case CASTLE:
                if (isInside(x, y, z, CASTLE_SPAWN)) return EnumChatFormatting.GREEN + "" + EnumChatFormatting.BOLD + "SPAWN";
                break;
        }
        return "";
    }

    private static boolean isInside(double x, double y, double z, List<BoundingBox> region) {
        for (BoundingBox box : region) {
            if (box.contains(x, y, z)) return true;
        }
        return false;
    }

    // ==========================================
    //       DYNAMIC DISTANCE / HUD FORMATTER
    // ==========================================

    /**
     * Call this in your HUDs. It returns the region if they are in spawn,
     * otherwise it calculates the exact distance with a color gradient!
     */
    public static String getLocationFormat(EntityPlayer localPlayer, EntityPlayer targetPlayer) {
        if (targetPlayer == null || localPlayer == null) return "";

        // 1. Check if they are in a defined region
        String region = getRegionString(targetPlayer);
        if (!region.isEmpty()) {
            return region;
        }

        // 2. If out in the map, calculate exact distance
        int distance = (int) localPlayer.getDistanceToEntity(targetPlayer);
        EnumChatFormatting distColor;

        // Apply dynamic color gradient based on threat proximity
        if (distance >= 100) {
            distColor = EnumChatFormatting.GREEN;          // > 100m away GREEN COLOR
        } else if (distance >= 50) {
            distColor = EnumChatFormatting.YELLOW;         // 50m - 99m away YELLOW COLOR
        } else if (distance >= 20) {
            distColor = EnumChatFormatting.GOLD;       // 20m - 49m away GOLD COLOR
        } else {
            distColor = EnumChatFormatting.RED;        // < 20m away (Danger close) - RED COLOR
        }

        // Returns formatted string like: "45m" in Yellow
        return distColor + String.valueOf(distance) + "m";
    }
}