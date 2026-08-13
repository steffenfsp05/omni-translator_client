package org.omni.locale.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.player.OmniPlayerConnectEvent;
import org.omni.event.register.player.OmniPlayerSettingsChangeEvent;
import org.omni.locale.LocaleManager;

@Singleton
public class LocaleSettingsChangeListener {

    final LocaleManager localeManager;

    @Inject
    public LocaleSettingsChangeListener(LocaleManager localeManager) {
        this.localeManager = localeManager;
    }


    @OmniSubscribe(priority = 92)
    public void onLocale(OmniPlayerSettingsChangeEvent event) {
        localeManager.updateLocale(event.uuid(), event.newLocale());

    }

}
