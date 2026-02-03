package com.linexstudios.foxtrot;

import java.util.ArrayList;
import java.util.List;

public class EnemyManager {
    public static List<String> enemies = new ArrayList<>();

    public static void addEnemy(String name) {
        if (!enemies.contains(name.toLowerCase())) {
            enemies.add(name.toLowerCase());
        }
    }

    public static void removeEnemy(String name) {
        enemies.remove(name.toLowerCase());
    }

    public static boolean isEnemy(String name) {
        return enemies.contains(name.toLowerCase());
    }
}