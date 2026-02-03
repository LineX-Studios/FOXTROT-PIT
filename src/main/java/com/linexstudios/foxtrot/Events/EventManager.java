package com.linexstudios.foxtrot.Events;

import java.util.ArrayList;
import java.util.List;

public class EventManager {
    private static List<String> upcomingEvents = new ArrayList<>();

    public static void updateEvents(String rawText) {
        upcomingEvents.clear();

        String[] lines = rawText.split("\n");
        int count = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            upcomingEvents.add(line);
            count++;
            if (count >= 6) break;
        }
    }

    public static List<String> getUpcomingEvents() {
        return upcomingEvents;
    }
}
