package org.pytenix.limbo.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.omni.profile.ProfileService;
import org.omni.proto.generated.Protobuf;
import org.pytenix.limbo.ConsentMessageFactory;

@Singleton
public class ServerPreConnectListener {

    final ProxyServer proxyServer;
    final ProfileService profileService;
    final Component component = ConsentMessageFactory.build();

    @Inject
    public ServerPreConnectListener(ProxyServer proxyServer, ProfileService profileService) {
        this.proxyServer = proxyServer;
        this.profileService = profileService;
    }


    @Subscribe
    public EventTask onPlayerConnect(ServerPreConnectEvent event) {

        if (event.getOriginalServer().getServerInfo().getName().equals("dynamic-limbo")) {
            return null;
        }

        final long nano = System.nanoTime();

        return EventTask.resumeWhenComplete(
                profileService.retrieveProfile(event.getPlayer().getUniqueId())
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