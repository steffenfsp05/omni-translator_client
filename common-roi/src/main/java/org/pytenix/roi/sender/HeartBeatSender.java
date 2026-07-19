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
    public HeartBeatSender(PlayerLocaleProcessor playerLocaleProcessor, ProfileEndpoint profileEndpoint, TransportSender transportSender, PlayerTrackerService playerTrackerService) {

        this.playerTrackerService = playerTrackerService;
        this.playerLocaleProcessor = playerLocaleProcessor;
        this.profileEndpoint = profileEndpoint;
        this.transportSender = transportSender;

        scheduler.schedule(this::sendHeartbeat, 20, TimeUnit.SECONDS);
    }


    private void sendHeartbeat() {


        final Set<UUID> onlinePlayers = playerTrackerService.getTrackCache().asMap().keySet();

        Map<String, Integer> langDistribution;
        List<CompletableFuture<ProfileResultData>> futures;


        langDistribution = onlinePlayers.stream()
                .collect(Collectors.toMap(
                        playerLocaleProcessor::retrieveLocale,
                        profile -> 1,
                        Integer::sum
                ));

        futures = onlinePlayers.stream()
                .map(profileEndpoint::sendRequest)
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


    @Override
    public void close() throws Exception {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}
