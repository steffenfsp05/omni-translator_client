package org.pytenix.tracking.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerSettingsChangedEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import org.pytenix.tracking.ROIService;

@RequiredArgsConstructor
public class PlayerSettingsChangeListener {

    final ROIService roiService;

    //TODO:
    @Subscribe
    public void onSettingsChange(PlayerSettingsChangedEvent event) {
        Player player = event.getPlayer();

        String newLocale = player.getPlayerSettings().getLocale().toString();

        System.out.println("LOCALE: " + newLocale);
        roiService.getLanguageCache().put(event.getPlayer().getUniqueId(),newLocale.toLowerCase());
    }


}
