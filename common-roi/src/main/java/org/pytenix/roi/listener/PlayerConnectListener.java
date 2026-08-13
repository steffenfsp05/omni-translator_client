package org.pytenix.roi.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.player.OmniPlayerConnectEvent;
import org.pytenix.roi.service.PlayerTrackerService;

@Singleton
public class PlayerConnectListener {

    private final PlayerTrackerService playerTrackerService;


    @Inject
    public PlayerConnectListener(PlayerTrackerService playerTrackerService) {
        this.playerTrackerService = playerTrackerService;
    }

    @OmniSubscribe(priority = 90)
    public void onPlayerConnect(OmniPlayerConnectEvent event) {
        playerTrackerService.initTrackingProcess(event.playerId());

    }

}
