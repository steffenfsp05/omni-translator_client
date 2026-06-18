package org.pytenix.placeholder.listener;

import org.pytenix.event.annotation.OmniSubscribe;
import org.pytenix.event.register.ConfigUpdateEvent;
import org.pytenix.placeholder.PlaceholderService;

public class ConfigUpdateListener {

    final PlaceholderService placeholderService;

    public ConfigUpdateListener(PlaceholderService placeholderService) {
        this.placeholderService = placeholderService;
    }


    @OmniSubscribe(priority = 90)
    public void onUpdate(ConfigUpdateEvent event) {

        placeholderService.updateProtectedWords(event.translationConfiguration().getBlacklistedWords());

    }
}
