package org.pytenix.tracking.listener;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import org.pytenix.tracking.ROIService;

public class PlayerDisconnectListener {

    private final ROIService roiService;

    @Inject
    public PlayerDisconnectListener(ROIService roiService) {
        this.roiService = roiService;
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        roiService.stopTrackingProcess(event.getPlayer().getUniqueId());
    }
}
