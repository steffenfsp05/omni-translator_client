package org.pytenix.limbo.listener;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import org.pytenix.TranslatorPlugin;
import org.pytenix.limbo.ConsentMessageFactory;
import org.pytenix.proto.generated.NetworkPackets;

@AllArgsConstructor
public class ServerPreConnectListener {

    final TranslatorPlugin translatorPlugin;
    final Component component = ConsentMessageFactory.build();

    @Subscribe
    public EventTask onPlayerConnect(ServerPreConnectEvent event) {

        if (event.getOriginalServer().getServerInfo().getName().equals("dynamic-limbo")) {
            return null;
        }

        return EventTask.resumeWhenComplete(
                translatorPlugin.getProfileService().getProfile(event.getPlayer().getUniqueId())
                        .thenAccept(profileData -> {
                            System.out.println("PRECONNECT: " + profileData);

                            if (profileData.consentType().equals(NetworkPackets.ProfilePacket.ConsentType.UNKNOWN)) {
                                translatorPlugin.getProxyServer().getServer("dynamic-limbo").ifPresent(limboServer -> {
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