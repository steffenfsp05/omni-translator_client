package org.pytenix.limbo.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.omni.proto.generated.Protobuf;
import org.omni.transport.endpoint.ProfileEndpoint;
import org.pytenix.limbo.ConsentMessageFactory;

@Singleton
public class ServerPreConnectListener {

    final ProxyServer proxyServer;
    final ProfileEndpoint profileEndpoint;
    final Component component = ConsentMessageFactory.build();

    @Inject
    public ServerPreConnectListener(ProxyServer proxyServer, ProfileEndpoint profileEndpoint) {
        this.proxyServer = proxyServer;
        this.profileEndpoint = profileEndpoint;
    }


    @Subscribe
    public EventTask onPlayerConnect(ServerPreConnectEvent event) {

        if (event.getOriginalServer().getServerInfo().getName().equals("dynamic-limbo")) {
            return null;
        }

        final long nano = System.nanoTime();

        return EventTask.resumeWhenComplete(
                profileEndpoint.sendRequest(event.getPlayer().getUniqueId())
                        .thenAccept(profileData -> {
                            System.out.println("TOOK " + ((System.nanoTime() - nano) / 1000000) + " ms PRECONNECT: " + profileData);

                            if (profileData.consentType().equals(Protobuf.ConsentType.UNKNOWN)) {
                                proxyServer.getServer("dynamic-limbo").ifPresent(limboServer -> {
                                    event.setResult(ServerPreConnectEvent.ServerResult.allowed(limboServer));


                                    event.getPlayer().sendMessage(component);


                                });
                            }

                        }).exceptionally(ex -> {
                            System.err.println("Fehler beim Abrufen des Profils: " + ex.getMessage());
                            return null;
                        })
        );
    }
}