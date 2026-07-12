package org.pytenix.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.pytenix.service.TaskScheduler;

// ONLY FOR DEMO SERVER
@Singleton
public class PlayerLocaleChangeListener implements Listener {

    private final TaskScheduler taskScheduler;

    @Inject
    public PlayerLocaleChangeListener(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    @EventHandler
    public void onLocaleChange(PlayerLocaleChangeEvent event) {
        Player player = event.getPlayer();

        final Location originalLocation = player.getLocation().clone();
        Location refreshLocation = originalLocation.clone().add(0, 0, 200);
        player.teleport(refreshLocation);

        taskScheduler.runSyncLater(() -> {
            player.teleport(originalLocation);
            player.sendMessage("§a[Omni] §7Language updated!");
        }, 3);
    }
}