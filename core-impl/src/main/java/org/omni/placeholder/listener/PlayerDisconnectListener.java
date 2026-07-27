package org.omni.placeholder.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.player.PlayerConnectEvent;
import org.omni.event.register.player.PlayerDisconnectEvent;
import org.omni.placeholder.protector.PlayerNameProtector;

@Singleton
public class PlayerDisconnectListener {

    final PlayerNameProtector playerNameProtector;

    @Inject
    public PlayerDisconnectListener(PlayerNameProtector playerNameProtector) {
        this.playerNameProtector = playerNameProtector;
    }


    @OmniSubscribe(priority = 91)
    public void onDisconnect(PlayerDisconnectEvent event) {
        playerNameProtector.removePlayer(event.playerName());

    }
}
