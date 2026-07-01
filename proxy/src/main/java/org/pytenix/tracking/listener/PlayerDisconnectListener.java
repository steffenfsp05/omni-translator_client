package org.pytenix.tracking.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import lombok.RequiredArgsConstructor;
import org.pytenix.tracking.ROIService;

@RequiredArgsConstructor
public class PlayerDisconnectListener {

    final ROIService roiService;


    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        roiService.stopTrackingProcess(event.getPlayer().getUniqueId());
    }
}
