package org.pytenix.roi.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.TrackPlayerRequestData;
import org.omni.profile.AbstractAnalyticsSecret;
import org.omni.proto.generated.Protobuf;
import org.omni.translation.TranslatorService;
import org.omni.translation.locale.PlayerLocaleProcessor;
import org.omni.transport.TransportSender;
import org.omni.transport.endpoint.ProfileEndpoint;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Singleton
public class PlayerTrackerService {


    @Getter
    final Cache<UUID, Long> trackCache = Caffeine.newBuilder()
            .maximumSize(3_000)
            .build();


    private final TranslatorService translatorService;
    private final TransportSender transportSender;
    private final AbstractAnalyticsSecret abstractAnalyticsSecret;
    private final PlayerLocaleProcessor playerLocaleProcessor;
    private final ProfileEndpoint profileEndpoint;


    @Inject
    public PlayerTrackerService(TranslatorService translatorService, TransportSender transportSender, AbstractAnalyticsSecret abstractAnalyticsSecret,
                                PlayerLocaleProcessor playerLocaleProcessor, ProfileEndpoint profileEndpoint) {
        this.translatorService = translatorService;
        this.transportSender = transportSender;
        this.abstractAnalyticsSecret = abstractAnalyticsSecret;
        this.playerLocaleProcessor = playerLocaleProcessor;
        this.profileEndpoint = profileEndpoint;
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

            profileEndpoint.sendRequest(uuid).thenAccept(profileResultData ->
            {
                if(profileResultData.analyticConsent().equals(Protobuf.ConsentType.DECLINED))
                    return;

                translatorService.requiresTranslation(uuid).thenAccept(requiresTranslation ->
                {

                    final UUID requestId = UUID.randomUUID();
                    final byte[] analyticsId = abstractAnalyticsSecret.getAnalyticsByteId(uuid);
                    final long currentTime = System.currentTimeMillis();
                    playerLocaleProcessor.retrieveLocale(uuid).thenAccept(locale ->
                    {
                        transportSender.sendPacket(PacketRegistry.TRACK_PLAYER,
                                new TrackPlayerRequestData(
                                        requestId,
                                        analyticsId,
                                        currentTime,
                                        playtimeInSeconds,
                                        requiresTranslation,
                                        locale
                                )
                        );
                    });


                });

            });

        }
    }


}
