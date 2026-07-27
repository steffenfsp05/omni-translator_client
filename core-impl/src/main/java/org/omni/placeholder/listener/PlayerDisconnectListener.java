package org.omni.placeholder.listener;

import com.google.inject.Singleton;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.player.PlayerConnectEvent;
import org.omni.placeholder.protector.PlayerNameProtector;

@Singleton
public class PlayerDisconnectListener {

    final PlayerNameProtector playerNameProtector;

    public PlayerDisconnectListener(PlayerNameProtector playerNameProtector) {
        this.playerNameProtector = playerNameProtector;
    }


    @OmniSubscribe(priority = 91)
    public void onConnect(PlayerConnectEvent event) {
        playerNameProtector.removePlayer(event.playerName());

    }
}
