package org.pytenix.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.pytenix.TranslatorPlugin;
import org.pytenix.placeholder.protect.PlayerNameProtector;
import org.pytenix.translation.TranslatorService;

public class JoinQuitListener implements Listener {

    final TranslatorPlugin translatorPlugin;
    final TranslatorService translatorService;
    final PlayerNameProtector playernameProtector;


    public JoinQuitListener(TranslatorPlugin plugin) {
        this.translatorPlugin = plugin;
        this.translatorService = plugin.getTranslatorService();
        this.playernameProtector = translatorService.getPlaceholderService().getPlayerNameProtector();

    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.playernameProtector.addPlayer(event.getPlayer().getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.playernameProtector.removePlayer(event.getPlayer().getName());
    }
}
