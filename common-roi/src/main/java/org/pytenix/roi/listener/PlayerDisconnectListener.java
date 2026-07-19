package org.pytenix.roi.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.player.PlayerConnectEvent;
import org.omni.event.register.player.PlayerDisconnectEvent;
import org.pytenix.roi.service.PlayerTrackerService;


@Singleton
public class PlayerDisconnectListener {


    private final PlayerTrackerService playerTrackerService;


    @Inject
    public PlayerDisconnectListener(PlayerTrackerService playerTrackerService)
    {
        this.playerTrackerService = playerTrackerService;
    }

    @OmniSubscribe(priority = 90)
    public void onPlayerDisconnect(PlayerDisconnectEvent event)
    {
        playerTrackerService.stopTrackingProcess(event.playerId());

    }

}
