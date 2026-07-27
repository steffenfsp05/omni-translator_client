package org.pytenix.limbo.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.omni.proto.generated.Protobuf;
import org.omni.transport.endpoint.ProfileEndpoint;
import org.pytenix.TranslatorPlugin;
import org.pytenix.limbo.book.ConsentMessageService;

import java.time.Duration;

@Singleton
public class ServerPreConnectListener {

    private final ProxyServer proxyServer;
    private final ProfileEndpoint profileEndpoint;
    private final TranslatorPlugin plugin;
    private final ConsentMessageService consentMessageService;

    @Inject
    public ServerPreConnectListener(ProxyServer proxyServer, ProfileEndpoint profileEndpoint, TranslatorPlugin plugin, ConsentMessageService consentMessageService) {
        this.proxyServer = proxyServer;
        this.profileEndpoint = profileEndpoint;
        this.plugin = plugin;
        this.consentMessageService = consentMessageService;
    }

    @Subscribe
    public EventTask onPlayerConnect(ServerPreConnectEvent event) {

        if (event.getOriginalServer().getServerInfo().getName().equals("dynamic-limbo")) {
            return null;
        }

        return EventTask.resumeWhenComplete(
                profileEndpoint.sendRequest(event.getPlayer().getUniqueId())
                        .thenAccept(profileData -> {

                            boolean needsTranslationConsent = profileData.translationConsent() == Protobuf.ConsentType.UNKNOWN;
                            boolean needsAnalyticsConsent = profileData.analyticConsent() == Protobuf.ConsentType.UNKNOWN;

                            if (needsTranslationConsent || needsAnalyticsConsent) {
                                proxyServer.getServer("dynamic-limbo").ifPresent(limboServer -> {
                                    event.setResult(ServerPreConnectEvent.ServerResult.allowed(limboServer));
                                });
                            }

                        }).exceptionally(ex -> {
                            System.err.println("Fehler beim Abrufen des Profils: " + ex.getMessage());
                            return null;
                        })
        );
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {

        if (event.getServer().getServerInfo().getName().equals("dynamic-limbo")) {

            proxyServer.getScheduler()
                    .buildTask(plugin, () -> {
                        if (event.getPlayer().isActive()) {
                            consentMessageService.getConsentMessage(event.getPlayer().getUniqueId()).thenAccept(message ->
                                    event.getPlayer().sendMessage(message));

                        }
                    })
                    .delay(Duration.ofMillis(500))
                    .schedule();
        }
    }
}