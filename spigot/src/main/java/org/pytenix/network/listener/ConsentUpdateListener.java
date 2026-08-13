package org.pytenix.network.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.player.OmniConsentUpdateEvent;
import org.pytenix.service.TaskScheduler;

@Singleton
public class ConsentUpdateListener {

    private final TaskScheduler taskScheduler;

    @Inject
    public ConsentUpdateListener(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    @OmniSubscribe(priority = 90)
    public void onConsentUpdate(OmniConsentUpdateEvent event) {
        taskScheduler.runSyncLater(() -> {
            Player player = Bukkit.getPlayer(event.refreshRequestData().playerId());
            if (player == null) return;

            final Location originalLocation = player.getLocation().clone();

            taskScheduler.runForEntity(player, () -> {
                Location refreshLocation = originalLocation.clone().add(0, 0, 200);
                final double hearts = player.getHealth();
                player.teleport(refreshLocation);

                taskScheduler.runSyncLater(() -> {
                    player.teleport(originalLocation);
                    player.setHealth(hearts);

                }, 5);
            });
        }, 2);
    }
}