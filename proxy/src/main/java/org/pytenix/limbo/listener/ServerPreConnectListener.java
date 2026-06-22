package org.pytenix.limbo.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ServerPreConnectListener {

    final ProxyServer proxyServer;

    @Subscribe
    public void onPlayerConnect(ServerPreConnectEvent event) {
        if (true)
            proxyServer.getServer("dynamic-limbo").ifPresent(limboServer -> {
                event.setResult(ServerPreConnectEvent.ServerResult.allowed(limboServer));

            });

    }

}
