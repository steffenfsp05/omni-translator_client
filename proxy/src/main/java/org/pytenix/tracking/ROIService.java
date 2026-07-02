package org.pytenix.tracking;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.translation.Translator;
import org.pytenix.TranslatorPlugin;
import org.pytenix.backend.OmniConnectionService;
import org.pytenix.packets.PacketMapperRegistry;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.packets.impl.HeartBeatRequestMapper;
import org.pytenix.packets.impl.ProfileMapper;
import org.pytenix.packets.impl.TrackPlayerRequestMapper;
import org.pytenix.proto.generated.NetworkPackets;

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
    final OmniConnectionService omniConnectionService;

    final Cache<UUID,Long> trackCache = Caffeine.newBuilder()
            .maximumSize(3_000)
            .build();



    public ROIService(TranslatorPlugin translatorPlugin, OmniConnectionService omniConnectionService) {
        this.translatorPlugin = translatorPlugin;
        this.omniConnectionService = omniConnectionService;
        translatorPlugin.getServer().getScheduler()
                .buildTask(translatorPlugin, this::sendHeartbeat)
                .repeat(Duration.ofSeconds(20)) //TODO:
                .schedule();
    }

        private void sendHeartbeat() {


        //TODO: OPTIMIZATION
        Map<String, Integer> langDistribution = translatorPlugin.getServer().getAllPlayers().stream()
                .collect(Collectors.toMap(
                        player -> translatorPlugin.getPlayerLocaleProcessor().retrieveLocale(player.getUniqueId()),
                        profile -> 1,
                        Integer::sum
                ));

            List<CompletableFuture<ProfileMapper.ProfileData>> futures = translatorPlugin.getServer().getAllPlayers().stream()
                    .map(player -> translatorPlugin.getProfileService().retrieveProfile(player.getUniqueId()))
                    .toList();

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
  }

    public void stopTrackingProcess(UUID uuid) {
        final Long nanoTime = trackCache.getIfPresent(uuid);

        if (nanoTime != null) {
            trackCache.invalidate(uuid);

            long elapsedNanos = System.nanoTime() - nanoTime;
            int playtimeInSeconds = (int) TimeUnit.NANOSECONDS.toSeconds(elapsedNanos);

            translatorPlugin.getTranslatorService().requiresTranslation(uuid).thenAccept(requiresTranslation ->
            {
                omniConnectionService.sendPacket(PacketRegistry.TRACK_PLAYER, PacketMapperRegistry.toProto(
                        new TrackPlayerRequestMapper.TrackData(
                                translatorPlugin.getConfigurationFile().getLicenseKey(),
                                UUID.randomUUID(),
                                uuid,
                                System.currentTimeMillis(),
                                playtimeInSeconds,
                                requiresTranslation,
                                translatorPlugin.getPlayerLocaleProcessor().retrieveLocale(uuid)
                        )
                ));


            });
        }
    }

}
