package org.pytenix.tracking;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import org.omni.config.ConfigurationFile;
import org.omni.packets.PacketMapperRegistry;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.HeartBeatUpdateData;
import org.omni.packets.data.ProfileResultData;
import org.omni.packets.data.TrackPlayerRequestData;
import org.omni.profile.AbstractAnalyticsSecret;
import org.omni.proto.generated.Protobuf;
import org.omni.translation.TranslatorService;
import org.omni.translation.locale.PlayerLocaleProcessor;
import org.omni.transport.TransportSender;
import org.omni.transport.endpoint.ProfileEndpoint;
import org.pytenix.TranslatorPlugin;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class ROIService {

    final TranslatorPlugin translatorPlugin;

    @Getter
    final TransportSender transportSender;
    final PlayerLocaleProcessor playerLocaleProcessor;
    final ProfileEndpoint profileEndpoint;
    final PacketMapperRegistry packetMapperRegistry;
    final ConfigurationFile configurationFile;
    final TranslatorService translatorService;
    final AbstractAnalyticsSecret abstractAnalyticsSecret;


    final Cache<UUID, Long> trackCache = Caffeine.newBuilder()
            .maximumSize(3_000)
            .build();

    @Getter
    final Cache<UUID, String> languageCache = Caffeine.newBuilder()
            .maximumSize(3_000)
            .build();


    @Inject
    public ROIService(
            TranslatorPlugin translatorPlugin,
            TransportSender transportSender,
            PlayerLocaleProcessor playerLocaleProcessor,
            ProfileEndpoint profileEndpoint,
            PacketMapperRegistry packetMapperRegistry,
            ConfigurationFile configurationFile,
            TranslatorService translatorService,
            AbstractAnalyticsSecret abstractAnalyticsSecret
    ) {

        this.translatorPlugin = translatorPlugin;
        this.transportSender = transportSender;
        this.playerLocaleProcessor = playerLocaleProcessor;
        this.profileEndpoint = profileEndpoint;
        this.packetMapperRegistry = packetMapperRegistry;
        this.configurationFile = configurationFile;
        this.translatorService = translatorService;
        this.abstractAnalyticsSecret = abstractAnalyticsSecret;

        translatorPlugin.getProxyServer().getScheduler()
                .buildTask(translatorPlugin, this::sendHeartbeat)
                .repeat(Duration.ofSeconds(20))
                .schedule();
    }

    private void sendHeartbeat() {

        Map<String, Integer> langDistribution;
        List<CompletableFuture<ProfileResultData>> futures;


        langDistribution = translatorPlugin.getProxyServer().getAllPlayers().stream()
                .collect(Collectors.toMap(
                        player -> playerLocaleProcessor.retrieveLocale(player.getUniqueId()),
                        profile -> 1,
                        Integer::sum
                ));

        futures = translatorPlugin.getProxyServer().getAllPlayers().stream()
                .map(player -> profileEndpoint.sendRequest(player.getUniqueId()))
                .toList();


        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenAccept(unused -> {

                    List<ProfileResultData> data = futures.stream()
                            .map(CompletableFuture::join)
                            .filter(Objects::nonNull)
                            .toList();

                    transportSender.sendPacket(PacketRegistry.HEART_BEAT,
                                    new HeartBeatUpdateData(
                                            UUID.randomUUID(),
                                            System.currentTimeMillis(),
                                            data.size(),
                                            filterByConsent(data, Protobuf.ConsentType.UNKNOWN),
                                            filterByConsent(data, Protobuf.ConsentType.EXPLICIT),
                                            filterByConsent(data, Protobuf.ConsentType.AUTO),
                                            filterByConsent(data, Protobuf.ConsentType.DECLINED),
                                            langDistribution
                                    )
                            );
                });
    }


    private int filterByConsent(List<ProfileResultData> profileDataList, Protobuf.ConsentType consentType) {
        return (int) profileDataList.stream()
                .filter(profileData -> profileData.consentType() == consentType)
                .count();
    }

    public void initTrackingProcess(UUID uuid) {
        trackCache.put(uuid, System.nanoTime());
    }

    public void stopTrackingProcess(UUID uuid) {
        final Long nanoTime = trackCache.getIfPresent(uuid);
        if (nanoTime != null) {
            trackCache.invalidate(uuid);

            long elapsedNanos = System.nanoTime() - nanoTime;
            int playtimeInSeconds = (int) TimeUnit.NANOSECONDS.toSeconds(elapsedNanos);

            translatorService.requiresTranslation(uuid).thenAccept(requiresTranslation ->
            {
                transportSender.sendPacket(PacketRegistry.TRACK_PLAYER,
                        new TrackPlayerRequestData(
                                configurationFile.getLicenseKey(),
                                UUID.randomUUID(),
                                abstractAnalyticsSecret.getAnalyticsByteId(uuid),
                                System.currentTimeMillis(),
                                playtimeInSeconds,
                                requiresTranslation,
                                playerLocaleProcessor.retrieveLocale(uuid)
                        )
                );
            });
        }
    }
}