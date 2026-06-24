package org.pytenix.network.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import org.pytenix.TranslatorPlugin;
import org.pytenix.packets.PacketMapperRegistry;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.packets.impl.ProfileMapper;
import org.pytenix.proto.generated.NetworkPackets;
import org.transport.TransportService;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
//TODO: LOOK PROXY SIDE; REFACTOR!

public class ProfileService {
    private final TranslatorPlugin translatorPlugin;
    private final String pluginMessageChannel;
    private final TransportService<String> transportService;


    private final ConcurrentHashMap<UUID, CompletableFuture<ProfileMapper.ProfileData>> queue = new ConcurrentHashMap<>();

    //TODO REFACTOR
    @Getter
    final Cache<UUID, ProfileMapper.ProfileData> profileCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(3)).build();

    public ProfileService(TranslatorPlugin translatorPlugin) {
        this.translatorPlugin = translatorPlugin;
        this.transportService = translatorPlugin.getSpigotTransport().getTransportService();
        this.pluginMessageChannel = translatorPlugin.getPluginMessagingChannel();
    }

    public void handleProfileResult(ProfileMapper.ProfileData resultData) {
        CompletableFuture<ProfileMapper.ProfileData> future = queue.remove(resultData.requestId());
        profileCache.put(resultData.playerId(), resultData);

        if (future != null) future.complete(resultData);
    }


    public CompletableFuture<ProfileMapper.ProfileData> getProfile(UUID playerId) {
        CompletableFuture<ProfileMapper.ProfileData> future = new CompletableFuture<>();

        ProfileMapper.ProfileData cachedProfile = profileCache.getIfPresent(playerId);
        if(cachedProfile != null)
            future.complete(cachedProfile);

        final UUID uuid = UUID.randomUUID();
        queue.put(uuid, future);

        final ProfileMapper.ProfileData profileData = new ProfileMapper.ProfileData(
                translatorPlugin.getConfigurationFile().getLicenseKey(),
                NetworkPackets.ProfilePacket.Action.FETCH,
                playerId,
                uuid,
                NetworkPackets.ProfilePacket.ConsentType.UNKNOWN
        );

        System.out.println("SENDING " + profileData);

        transportService.send(pluginMessageChannel, PacketRegistry.PROFILE, PacketMapperRegistry.toProto(
                profileData
        ));



        return future.orTimeout(60, TimeUnit.SECONDS).exceptionally(ex -> {
            queue.remove(uuid);
            return new ProfileMapper.ProfileData("NULL", null, null, null, null);
        });
    }

}
