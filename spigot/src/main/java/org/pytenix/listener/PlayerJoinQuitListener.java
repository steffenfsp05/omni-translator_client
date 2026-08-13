package org.pytenix.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.omni.event.EventService;
import org.omni.event.register.player.PlayerConnectEvent;
import org.omni.event.register.player.OmniPlayerDisconnectEvent;

@Singleton
public class PlayerJoinQuitListener implements Listener {

    private final EventService eventService;

    @Inject
    public PlayerJoinQuitListener(EventService eventService) {
        this.eventService = eventService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.eventService.callEvent(new PlayerConnectEvent(event.getPlayer().getUniqueId(), event.getPlayer().getName(), event.getPlayer().getLocale().toString()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.eventService.callEvent(new OmniPlayerDisconnectEvent(event.getPlayer().getUniqueId(), event.getPlayer().getName()));
    }
}