package org.pytenix.listener;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerSettingsChangedEvent;
import com.velocitypowered.api.proxy.Player;
import org.omni.event.EventService;
import org.omni.event.register.player.PlayerSettingsChangeEvent;

public class PlayerSettingsChangeListener {

    private final EventService eventService;

    @Inject
    public PlayerSettingsChangeListener(EventService eventService) {
        this.eventService = eventService;
    }

    @Subscribe
    public void onSettingsChange(PlayerSettingsChangedEvent event) {

        final Player player = event.getPlayer();
        String newLocale = event.getPlayerSettings().getLocale().toString();

        eventService.callEvent(new PlayerSettingsChangeEvent(player.getUniqueId(), newLocale));
    }


}
