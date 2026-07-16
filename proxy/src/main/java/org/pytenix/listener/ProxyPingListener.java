package org.pytenix.listener;

import com.google.inject.Inject;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.server.ServerPing;
import org.omni.entity.ServerConfiguration;
import org.omni.entity.TranslationModule;
import org.omni.packets.data.GeoRequestData;
import org.omni.translation.TranslatorService;
import org.omni.translation.component.TextComponentService;
import org.omni.transport.endpoint.GeoEndpoint;
import org.pytenix.socket.endpoint.GeoSocketEndpoint;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ProxyPingListener {


    private final GeoEndpoint geoEndpoint;
    private final TranslatorService translatorService;
    private final TextComponentService textComponentService;

    private final TranslationModule translationModule;

    @Inject
    public ProxyPingListener(TranslatorService translatorService, GeoEndpoint geoEndpoint, TextComponentService textComponentService) {
        this.geoEndpoint = geoEndpoint;
        this.translatorService = translatorService;
        this.textComponentService = textComponentService;

        this.translationModule = TranslationModule.MOTD;
    }

    @Subscribe
    public EventTask onPing(com.velocitypowered.api.event.proxy.ProxyPingEvent event) {
        System.out.println("[MOTD] Ping empfangen!");

        ServerConfiguration configuration = translatorService.getTranslationConfiguration();

        if (configuration == null) {
            System.out.println("[MOTD] Abbruch: ServerConfiguration ist NULL. (Noch nicht geladen?)");
            return null;
        }

        if (!configuration.getModules().getOrDefault(translationModule, true)) {
            System.out.println("[MOTD] Abbruch: MOTD-Modul ist in der Config deaktiviert.");
            return null;
        }

        String ipAddress = anonymizeAddress(event.getConnection().getRemoteAddress().getAddress().getHostAddress());
        final UUID uuid = UUID.randomUUID();

        System.out.println("[MOTD] Starte asynchrone Übersetzung für IP: " + ipAddress);

        CompletableFuture<Void> pingPipeline = geoEndpoint.sendRequest(new GeoRequestData(uuid, ipAddress))
                .orTimeout(400, TimeUnit.MILLISECONDS)
                .thenCompose(locale -> {
                    System.out.println("[MOTD] Geo-Location erhalten: " + locale + ". Übersetze Text...");

                    return textComponentService.translateComplexMessage(
                            event.getPing().getDescriptionComponent(),
                            locale,
                            translationModule
                    );
                })
                .orTimeout(400, TimeUnit.MILLISECONDS)
                .thenAccept(translatedComponent -> {
                    System.out.println("[MOTD] Übersetzung fertig! Wende neue MOTD an.");

                    ServerPing.Builder builder = event.getPing().asBuilder();
                    builder.description(translatedComponent);
                    event.setPing(builder.build());
                })
                .exceptionally(throwable -> {
                    System.err.println("[MOTD] Fehler in der Pipeline (oder Timeout erreicht)! Nutze Standard-MOTD.");
                    throwable.printStackTrace();
                    return null;
                });

        return EventTask.resumeWhenComplete(pingPipeline);
    }


    private String anonymizeAddress(String ipAddress) {
        String anonymizedIp;
        if (ipAddress.contains(".")) {
            anonymizedIp = ipAddress.substring(0, ipAddress.lastIndexOf('.')) + ".0";
        } else if (ipAddress.contains(":")) {
            String[] parts = ipAddress.split(":");
            if (parts.length >= 4) {
                anonymizedIp = parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3] + "::";
            } else {
                anonymizedIp = ipAddress;
            }
        } else {
            anonymizedIp = ipAddress;
        }
        return anonymizedIp;
    }
}