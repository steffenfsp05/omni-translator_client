package org.pytenix.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.omni.event.EventService;
import org.omni.event.register.player.OmniAsyncPlayerPreDisconnectEvent;
import org.pytenix.service.TaskScheduler;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class PlayerConnectionInterruptListener implements Listener {

    private final TaskScheduler taskScheduler;
    private final EventService eventService;

    private final Set<UUID> processedKicks = ConcurrentHashMap.newKeySet();

    @Inject
    public PlayerConnectionInterruptListener(TaskScheduler taskScheduler, EventService eventService) {
        this.taskScheduler = taskScheduler;
        this.eventService = eventService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {

            OmniAsyncPlayerPreDisconnectEvent omniEvent = new OmniAsyncPlayerPreDisconnectEvent(
                    event.getUniqueId(),
                    event.getKickMessage()
            );

            eventService.callEventAsync(omniEvent).thenAccept(result -> {
                event.setKickMessage(result.getReason());
            }).join();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (processedKicks.remove(uuid)) return;

        event.setCancelled(true);
        String foreignReason = event.getReason();

        OmniAsyncPlayerPreDisconnectEvent omniEvent = new OmniAsyncPlayerPreDisconnectEvent(
                uuid,
                foreignReason
        );

        eventService.callEventAsync(omniEvent).thenAccept(result ->
                taskScheduler.runForEntity(player, () -> {
                    processedKicks.add(uuid);
                    player.kickPlayer(result.getReason());
        }));
    }


}
