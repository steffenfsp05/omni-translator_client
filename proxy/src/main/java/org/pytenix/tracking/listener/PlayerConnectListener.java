package org.pytenix.tracking.listener;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import org.pytenix.tracking.ROIService;


public class PlayerConnectListener {

    private final ROIService roiService;

    @Inject
    public PlayerConnectListener(ROIService roiService) {
        this.roiService = roiService;
    }

    @Subscribe
    public void onConnect(LoginEvent event) {
        roiService.initTrackingProcess(event.getPlayer().getUniqueId());
    }
}
