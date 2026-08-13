package org.pytenix.listener;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import org.omni.event.EventService;
import org.omni.event.register.player.OmniPlayerConnectEvent;


public class PlayerConnectListener {

    private final EventService eventService;

    @Inject
    public PlayerConnectListener(EventService eventService) {
        this.eventService = eventService;
    }

    @Subscribe
    public void onConnect(LoginEvent event) {

        String locale = event.getPlayer().getEffectiveLocale() == null ? null : event.getPlayer().getEffectiveLocale().toString().toLowerCase();

        eventService.callEvent(new OmniPlayerConnectEvent(event.getPlayer().getUniqueId(), event.getPlayer().getUsername(),locale ));
    }
}
