package org.pytenix.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.omni.event.EventService;
import org.omni.event.register.player.PlayerSettingsChangeEvent;
import org.pytenix.service.TaskScheduler;

// ONLY FOR DEMO SERVER
@Singleton
public class PlayerLocaleChangeListener implements Listener {

    private final EventService eventService;
    private final TaskScheduler taskScheduler;

    @Inject
    public PlayerLocaleChangeListener(TaskScheduler taskScheduler, EventService eventService) {
        this.taskScheduler = taskScheduler;
        this.eventService = eventService;
    }

    @EventHandler
    public void onLocaleChange(PlayerLocaleChangeEvent event) {
        Player player = event.getPlayer();
        eventService.callEvent(new PlayerSettingsChangeEvent(player.getUniqueId(),event.locale().toString()));

        final Location originalLocation = player.getLocation().clone();
        Location refreshLocation = originalLocation.clone().add(0, 0, 200);
        player.teleport(refreshLocation);

        taskScheduler.runSyncLater(() -> {
            player.teleport(originalLocation);
            player.sendMessage("§a[Omni] §7Language updated!");
        }, 3);
    }
}