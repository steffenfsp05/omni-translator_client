package org.pytenix.chat.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import lombok.AllArgsConstructor;
import org.pytenix.TranslatorPlugin;

import java.util.UUID;

@AllArgsConstructor
public class PlayerDisconnectListener {

    final TranslatorPlugin translatorPlugin;


    @Subscribe
    public void onDisconnect(DisconnectEvent disconnectEvent) {
        final UUID uuid = disconnectEvent.getPlayer().getUniqueId();
        translatorPlugin.getSystemChatService().getMessageSequencer().cleanup(uuid);
    }
}
