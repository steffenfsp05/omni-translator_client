package org.pytenix.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.omni.placeholder.protector.PlayerNameProtector;

@Singleton
public class PlayerJoinQuitListener implements Listener {

    private final PlayerNameProtector playerNameProtector;

    @Inject
    public PlayerJoinQuitListener(PlayerNameProtector playerNameProtector) {
        this.playerNameProtector = playerNameProtector;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.playerNameProtector.addPlayer(event.getPlayer().getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.playerNameProtector.removePlayer(event.getPlayer().getName());
    }
}