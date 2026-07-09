package org.pytenix.tracking.listener;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerSettingsChangedEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import org.pytenix.tracking.ROIService;

public class PlayerSettingsChangeListener {

    private final ROIService roiService;

    @Inject
    public PlayerSettingsChangeListener(ROIService roiService) {
        this.roiService = roiService;
    }

    //TODO:
    @Subscribe
    public void onSettingsChange(PlayerSettingsChangedEvent event) {
        Player player = event.getPlayer();

        String newLocale = player.getPlayerSettings().getLocale().toString();

        System.out.println("LOCALE: " + newLocale);
        roiService.getLanguageCache().put(event.getPlayer().getUniqueId(),newLocale.toLowerCase());
    }


}
