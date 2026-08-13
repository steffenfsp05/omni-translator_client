package org.omni.locale.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.player.OmniPlayerDisconnectEvent;
import org.omni.locale.LocaleManager;

@Singleton
public class LocalePlayerDisconnectListener {

    final LocaleManager localeManager;

    @Inject
    public LocalePlayerDisconnectListener(LocaleManager localeManager) {
        this.localeManager = localeManager;
    }


    @OmniSubscribe(priority = 92)
    public void onDisconnect(OmniPlayerDisconnectEvent event) {
        localeManager.cleanupPlayer(event.playerId());

    }
}
