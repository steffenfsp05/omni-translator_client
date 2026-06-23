package org.pytenix.limbo.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import org.pytenix.limbo.ConsentMessageFactory;

@AllArgsConstructor
public class ServerPreConnectListener {

    final ProxyServer proxyServer;
    final Component component = ConsentMessageFactory.build();

    @Subscribe
    public void onPlayerConnect(ServerPreConnectEvent event) {
        if (true)
            proxyServer.getServer("dynamic-limbo").ifPresent(limboServer -> {
                event.setResult(ServerPreConnectEvent.ServerResult.allowed(limboServer));

                event.getPlayer().sendMessage(component);
            });

    }

}
