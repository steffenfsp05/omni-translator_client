package org.omni.placeholder.listener;


import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.ConfigUpdateEvent;
import org.omni.placeholder.service.PlaceholderService;

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
