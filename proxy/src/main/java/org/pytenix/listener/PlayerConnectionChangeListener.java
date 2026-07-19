package org.pytenix.listener;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import org.omni.placeholder.protector.PlayerNameProtector;

public class PlayerConnectionChangeListener {


    private final PlayerNameProtector playerNameProtector;


    @Inject
    public PlayerConnectionChangeListener(PlayerNameProtector playerNameProtector) {
        this.playerNameProtector = playerNameProtector;
    }


    @Subscribe
    public void onJoin(LoginEvent event) {
        playerNameProtector.addPlayer(event.getPlayer().getUsername());
    }

    @Subscribe
    public void onQuit(DisconnectEvent event) {
        playerNameProtector.removePlayer(event.getPlayer().getUsername());
    }
}
