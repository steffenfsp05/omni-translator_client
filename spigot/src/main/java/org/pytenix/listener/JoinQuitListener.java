package org.pytenix.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.pytenix.AdvancedTranslationBridge;
import org.pytenix.SpigotTranslator;
import org.pytenix.placeholder.protect.PlayernameProtector;

public class JoinQuitListener implements Listener {

    final SpigotTranslator spigotTranslator;
    final AdvancedTranslationBridge translationBridge;
    final PlayernameProtector playernameProtector;


    public JoinQuitListener(SpigotTranslator plugin)
    {
        this.spigotTranslator = plugin;
        this.translationBridge = plugin.getSpigotBridge();
        this.playernameProtector = translationBridge.getPlaceholderService().getPlayernameProtector();

    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        this.playernameProtector.addPlayer(event.getPlayer().getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        this.playernameProtector.removePlayer(event.getPlayer().getName());
    }
}
