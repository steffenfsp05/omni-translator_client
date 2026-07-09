package org.pytenix.tracking;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.translation.Translator;
import org.pytenix.TranslatorPlugin;
import org.pytenix.backend.OmniConnectionService;
import org.pytenix.packets.PacketMapperRegistry;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.packets.impl.HeartBeatRequestMapper;
import org.pytenix.packets.impl.ProfileMapper;
import org.pytenix.packets.impl.TrackPlayerRequestMapper;
import org.pytenix.profile.AnalyticsKey;
import org.pytenix.proto.generated.NetworkPackets;
import org.pytenix.tracking.mock.MockROIService;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ROIService {

    final TranslatorPlugin translatorPlugin;

    @Getter
    final OmniConnectionService omniConnectionService;

    final Cache<UUID,Long> trackCache = Caffeine.newBuilder()
            .maximumSize(3_000)
            .build();

    @Getter
    final Cache<UUID,String> languageCache = Caffeine.newBuilder()
            .maximumSize(3_000)
            .build();



    final boolean isMock = false;
    final MockROIService mockROIService;



    public ROIService(TranslatorPlugin translatorPlugin, OmniConnectionService omniConnectionService) {
        this.translatorPlugin = translatorPlugin;
        this.omniConnectionService = omniConnectionService;

        if(isMock)
        {
            this.mockROIService = new MockROIService(translatorPlugin, this);
        } else {
            mockROIService = null;
        }


        translatorPlugin.getServer().getScheduler()
                .buildTask(translatorPlugin, this::sendHeartbeat)
                .repeat(Duration.ofSeconds(20)) //TODO:
                .schedule();
    }

    private void sendHeartbeat() {

        Map<String, Integer> langDistribution;
        List<CompletableFuture<ProfileMapper.ProfileData>> futures;

            if(isMock)
            {
           /*     langDistribution = mockROIService.getVirtualPlayers().keySet().stream()
                        .collect(Collectors.toMap(
                                uuid -> translatorPlugin.getPlayerLocaleProcessor().retrieveLocale(uuid),
                                profile -> 1,
                                Integer::sum
                        ));
                futures = mockROIService.getVirtualPlayers().keySet().stream()
                        .map(uuid -> translatorPlugin.getProfileService().retrieveProfile(uuid))
                        .toList();

            */
            } else {
                langDistribution = translatorPlugin.getServer().getAllPlayers().stream()
                        .collect(Collectors.toMap(
                                player -> translatorPlugin.getPlayerLocaleProcessor().retrieveLocale(player.getUniqueId()),
                                profile -> 1,
                                Integer::sum
                        ));

                futures = translatorPlugin.getServer().getAllPlayers().stream()
                        .map(player -> translatorPlugin.getProfileService().retrieveProfile(player.getUniqueId()))
                        .toList();
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenAccept(unused -> {

                        List<ProfileMapper.ProfileData> data = futures.stream()
                                .map(CompletableFuture::join)
                                .filter(Objects::nonNull)
                                .toList();

                        omniConnectionService.sendPacket(PacketRegistry.HEART_BEAT,
                                PacketMapperRegistry.toProto(
                                        new HeartBeatRequestMapper.HeartBeatData(
                                                translatorPlugin.getConfigurationFile().getLicenseKey(),
                                                UUID.randomUUID(),
                                                System.currentTimeMillis(),
                                                data.size(),
                                                filterByConsent(data, NetworkPackets.ProfilePacket.ConsentType.UNKNOWN),
                                                filterByConsent(data, NetworkPackets.ProfilePacket.ConsentType.EXPLICIT),
                                                filterByConsent(data, NetworkPackets.ProfilePacket.ConsentType.AUTO),
                                                filterByConsent(data, NetworkPackets.ProfilePacket.ConsentType.DECLINED),
                                                langDistribution
                                        )
                                ));
                    });
        }






    private int filterByConsent(List<ProfileMapper.ProfileData> profileDataList, NetworkPackets.ProfilePacket.ConsentType consentType) {
        return (int) profileDataList.stream()
                .filter(profileData -> profileData.consentType() == consentType)
                .count();
    }

    public void initTrackingProcess(UUID uuid)
    {

            trackCache.put(uuid, System.nanoTime());
            System.out.println("TRACK CACHE PUT: " + trackCache.getIfPresent(uuid));
    }

    public void stopTrackingProcess(UUID uuid) {
        System.out.println("ASDASDAAAAAAAAAAAAAAAA");
        System.out.println("TRACK CACHE: " + trackCache.getIfPresent(uuid));
        final Long nanoTime = trackCache.getIfPresent(uuid);
        System.out.println("VVVV");
        if (nanoTime != null) {
            trackCache.invalidate(uuid);

            System.out.println("ADASDA");
            long elapsedNanos = System.nanoTime() - nanoTime;
            int playtimeInSeconds = (int) TimeUnit.NANOSECONDS.toSeconds(elapsedNanos);

            translatorPlugin.getTranslatorService().requiresTranslation(uuid).thenAccept(requiresTranslation ->
            {
                omniConnectionService.sendPacket(PacketRegistry.TRACK_PLAYER, PacketMapperRegistry.toProto(
                        new TrackPlayerRequestMapper.TrackData(
                                translatorPlugin.getConfigurationFile().getLicenseKey(),
                                UUID.randomUUID(),
                                translatorPlugin.getAnalyticsManager().getAnalyticsByteId(uuid),
                                System.currentTimeMillis(),
                                playtimeInSeconds,
                                requiresTranslation,
                                translatorPlugin.getPlayerLocaleProcessor().retrieveLocale(uuid)
                        )
                ));
                System.out.println("SENT PACK");
            });
        }
    }
}