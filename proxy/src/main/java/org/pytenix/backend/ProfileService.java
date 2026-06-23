package org.pytenix.backend;

import org.pytenix.packets.PacketMapperRegistry;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.packets.impl.GeoRequestMapper;
import org.pytenix.packets.impl.GeoResultMapper;
import org.pytenix.packets.impl.ProfileMapper;
import org.pytenix.proto.generated.NetworkPackets;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ProfileService {
    private final OmniConnectionService connectionManager;
    private final ConcurrentHashMap<UUID, CompletableFuture<ProfileMapper.ProfileData>> queue = new ConcurrentHashMap<>();

    public ProfileService(OmniConnectionService connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void handleProfileResult(ProfileMapper.ProfileData resultData) {
        CompletableFuture<ProfileMapper.ProfileData> future = queue.remove(resultData.requestId());
        if (future != null) future.complete(resultData);
    }

    public void updateProfile(ProfileMapper.ProfileData profileData) {

        connectionManager.sendPacket(PacketRegistry.PROFILE,
                PacketMapperRegistry.toProto(
                        profileData.withAction(NetworkPackets.ProfilePacket.Action.UPDATE)
                ));
    }

    public CompletableFuture<ProfileMapper.ProfileData> getProfile(UUID playerId) {
        CompletableFuture<ProfileMapper.ProfileData> future = new CompletableFuture<>();

        final UUID uuid = UUID.randomUUID();
        queue.put(uuid, future);

        final ProfileMapper.ProfileData profileData = new ProfileMapper.ProfileData(
                connectionManager.getApiKey(),
                NetworkPackets.ProfilePacket.Action.FETCH,
                playerId,
                uuid,
                NetworkPackets.ProfilePacket.ConsentType.UNKNOWN
        );

        System.out.println("SENDING " + profileData);

        connectionManager.sendPacket(PacketRegistry.PROFILE,
                PacketMapperRegistry.toProto(
                        profileData
        ));

        return future.orTimeout(60, TimeUnit.SECONDS).exceptionally(ex -> {
            queue.remove(uuid);
            return new ProfileMapper.ProfileData("NULL",null,null,null,null);
        });
    }

}
