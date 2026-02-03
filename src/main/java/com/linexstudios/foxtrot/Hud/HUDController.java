package com.linexstudios.foxtrot.Hud;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class HUDController {
    public static boolean dragMode = false;
    public static boolean enabled = true;

    private static final NickedHUD nickedHUD = new NickedHUD();
    private static final EventsHUD eventsHUD = new EventsHUD();
    private static final EnemyHUD enemyHUD = new EnemyHUD();

    public static void toggleDragMode() {
        dragMode = !dragMode;
        NickedHUD.dragMode = dragMode;
        EventsHUD.dragMode = dragMode;
        EnemyHUD.dragMode = dragMode;
    }

    public static void setEnabled(boolean state) {
        enabled = state;
        NickedHUD.enabled = state;
        EventsHUD.enabled = state;
        EnemyHUD.enabled = state;
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (!enabled) return;
        if (NickedHUD.enabled) nickedHUD.onRender(event);
        if (EventsHUD.enabled) eventsHUD.onRender(event);
        if (EnemyHUD.enabled) enemyHUD.onRender(event);
    }
}
