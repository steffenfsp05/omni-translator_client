package org.pytenix.tracking;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.pytenix.TranslatorPlugin;
import org.pytenix.backend.OmniConnectionService;
import org.pytenix.packets.PacketMapperRegistry;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.packets.impl.TrackPlayerRequestMapper;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class ROIService {

    final TranslatorPlugin translatorPlugin;
    final OmniConnectionService omniConnectionService;

    final Cache<UUID,Long> trackCache = Caffeine.newBuilder()
            .maximumSize(3_000)
            .build();


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

            omniConnectionService.sendPacket(PacketRegistry.TRACK_PLAYER, PacketMapperRegistry.toProto(
                    new TrackPlayerRequestMapper.TrackData(
                            translatorPlugin.getConfigurationFile().getLicenseKey(),
                            UUID.randomUUID(),
                            uuid,
                            System.nanoTime(),
                            playtimeInSeconds
                    )
            ));
        }
    }

}
