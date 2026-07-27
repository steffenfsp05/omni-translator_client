package org.pytenix.roi.sender;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.HeartBeatUpdateData;
import org.omni.packets.data.ProfileResultData;
import org.omni.proto.generated.Protobuf;
import org.omni.translation.locale.PlayerLocaleProcessor;
import org.omni.transport.TransportSender;
import org.omni.transport.endpoint.ProfileEndpoint;
import org.pytenix.roi.service.PlayerTrackerService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class HeartBeatSender implements AutoCloseable {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final PlayerTrackerService playerTrackerService;
    private final PlayerLocaleProcessor playerLocaleProcessor;
    private final ProfileEndpoint profileEndpoint;
    private final TransportSender transportSender;

    @Inject
    public HeartBeatSender(PlayerLocaleProcessor playerLocaleProcessor, ProfileEndpoint profileEndpoint,
                           TransportSender transportSender, PlayerTrackerService playerTrackerService) {

        this.playerTrackerService = playerTrackerService;
        this.playerLocaleProcessor = playerLocaleProcessor;
        this.profileEndpoint = profileEndpoint;
        this.transportSender = transportSender;

        scheduler.scheduleWithFixedDelay(this::sendHeartbeat, 20, 20, TimeUnit.SECONDS);
    }

    private void sendHeartbeat() {

        System.out.println("SENDING HEARTBEAT");
        final Set<UUID> onlinePlayers = playerTrackerService.getTrackCache().asMap().keySet();

        if (onlinePlayers.isEmpty()) {
            sendEmptyHeartbeat();
            return;
        }

        List<CompletableFuture<String>> localeFutures = onlinePlayers.stream()
                .map(playerLocaleProcessor::retrieveLocale)
                .toList();

        List<CompletableFuture<ProfileResultData>> profileFutures = onlinePlayers.stream()
                .map(profileEndpoint::sendRequest)
                .toList();

        List<CompletableFuture<?>> allFutures = new ArrayList<>();
        allFutures.addAll(localeFutures);
        allFutures.addAll(profileFutures);

        CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]))
                .thenAccept(unused -> {

                    Map<String, Integer> langDistribution = localeFutures.stream()
                            .map(CompletableFuture::join)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toMap(
                                    locale -> locale,
                                    locale -> 1,
                                    Integer::sum
                            ));

                    List<ProfileResultData> data = profileFutures.stream()
                            .map(CompletableFuture::join)
                            .filter(Objects::nonNull)
                            .toList();

                    System.out.println("SENT!");

                    transportSender.sendPacket(PacketRegistry.HEART_BEAT,
                            new HeartBeatUpdateData(
                                    UUID.randomUUID(),
                                    System.currentTimeMillis(),
                                    data.size(), // totalOnline

                                    filterByTranslationConsent(data, Protobuf.ConsentType.UNKNOWN),
                                    filterByTranslationConsent(data, Protobuf.ConsentType.EXPLICIT),
                                    filterByTranslationConsent(data, Protobuf.ConsentType.AUTO),
                                    filterByTranslationConsent(data, Protobuf.ConsentType.DECLINED),

                                    filterByAnalyticConsent(data, Protobuf.ConsentType.UNKNOWN),
                                    filterByAnalyticConsent(data, Protobuf.ConsentType.EXPLICIT),
                                    filterByAnalyticConsent(data, Protobuf.ConsentType.AUTO),
                                    filterByAnalyticConsent(data, Protobuf.ConsentType.DECLINED),

                                    langDistribution
                            )
                    );
                }).exceptionally(ex -> {
                    System.err.println("Fehler beim Senden des Heartbeats: " + ex.getMessage());
                    return null;
                });
    }

    private void sendEmptyHeartbeat() {
        transportSender.sendPacket(PacketRegistry.HEART_BEAT,
                new HeartBeatUpdateData(
                        UUID.randomUUID(),
                        System.currentTimeMillis(),
                        0, // totalOnline
                        0, 0, 0, 0,
                        0, 0, 0, 0,
                        Collections.emptyMap()
                )
        );
    }

    private int filterByTranslationConsent(List<ProfileResultData> profileDataList, Protobuf.ConsentType consentType) {
        return (int) profileDataList.stream()
                .filter(profileData -> profileData.translationConsent() == consentType)
                .count();
    }

    private int filterByAnalyticConsent(List<ProfileResultData> profileDataList, Protobuf.ConsentType consentType) {
        return (int) profileDataList.stream()
                .filter(profileData -> profileData.analyticConsent() == consentType)
                .count();
    }

    @Override
    public void close() throws Exception {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}