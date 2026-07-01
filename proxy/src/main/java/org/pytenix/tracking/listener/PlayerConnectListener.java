package org.pytenix.tracking.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import lombok.RequiredArgsConstructor;
import org.pytenix.tracking.ROIService;


@RequiredArgsConstructor
public class PlayerConnectListener {

    final ROIService roiService;


    @Subscribe
    public void onConnect(PostLoginEvent event) {
        roiService.initTrackingProcess(event.getPlayer().getUniqueId());
    }

}
