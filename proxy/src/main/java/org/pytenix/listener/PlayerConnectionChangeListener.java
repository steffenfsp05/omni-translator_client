package org.pytenix.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import org.pytenix.TranslatorPlugin;
import org.pytenix.translation.TranslatorService;

public class PlayerConnectionChangeListener {


    final TranslatorPlugin translator;
     final TranslatorService translatorService;


    public PlayerConnectionChangeListener(TranslatorPlugin translator) {
        this.translator = translator;
        this.translatorService = translator.getTranslatorService();
    }


    @Subscribe
    public void onJoin(LoginEvent event) {
        translatorService.getPlaceholderService().getPlayerNameProtector().addPlayer(event.getPlayer().getUsername());
    }

    @Subscribe
    public void onQuit(DisconnectEvent event) {
        translatorService.getPlaceholderService().getPlayerNameProtector().addPlayer(event.getPlayer().getUsername());
    }
}
