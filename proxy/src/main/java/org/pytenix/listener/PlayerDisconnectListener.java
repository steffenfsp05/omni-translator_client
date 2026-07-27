package org.pytenix.listener;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import org.omni.event.EventService;
import org.omni.event.register.player.PlayerDisconnectEvent;

public class PlayerDisconnectListener {

    private final EventService eventService;

    @Inject
    public PlayerDisconnectListener(EventService eventService) {
        this.eventService = eventService;
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        eventService.callEvent(new PlayerDisconnectEvent(event.getPlayer().getUniqueId(), event.getPlayer().getUsername()));
    }
}
