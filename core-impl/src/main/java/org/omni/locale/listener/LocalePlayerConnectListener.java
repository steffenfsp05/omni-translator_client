package org.omni.locale.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.player.PlayerConnectEvent;
import org.omni.locale.LocaleManager;
import org.omni.placeholder.protector.PlayerNameProtector;

@Singleton
public class LocalePlayerConnectListener {

    final LocaleManager localeManager;

    @Inject
    public LocalePlayerConnectListener(LocaleManager localeManager) {
        this.localeManager = localeManager;
    }


    @OmniSubscribe(priority = 92)
    public void onConnect(PlayerConnectEvent event) {


        if(event.locale() != null)
          localeManager.updateLocale(event.playerId(),event.locale());

    }

}
