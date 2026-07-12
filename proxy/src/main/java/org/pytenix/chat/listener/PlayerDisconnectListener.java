package org.pytenix.chat.listener;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import org.pytenix.chat.MessageSequencer;

import java.util.UUID;

public class PlayerDisconnectListener {

    private final MessageSequencer messageSequencer;

    @Inject
    public PlayerDisconnectListener(MessageSequencer messageSequencer) {
        this.messageSequencer = messageSequencer;
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent disconnectEvent) {
        final UUID uuid = disconnectEvent.getPlayer().getUniqueId();
        messageSequencer.cleanup(uuid);
    }
}