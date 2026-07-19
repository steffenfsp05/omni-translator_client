package org.omni.packets;

import org.omni.proto.generated.Protobuf;
import org.transport.service.impl.PacketDefinition;

public class PacketRegistry {

    public static final PacketDefinition<Protobuf.ConfigRequestPacket> CONFIG_REQUEST =
            new PacketDefinition<>(
                    1,
                    Protobuf.ConfigRequestPacket.parser()
            );


    public static final PacketDefinition<Protobuf.ServerConfiguration> SERVER_CONFIG =
            new PacketDefinition<>(
                    2,
                    Protobuf.ServerConfiguration.parser()
            );

    public static final PacketDefinition<Protobuf.TranslationRequest> TRANSLATION_REQUEST =
            new PacketDefinition<>(
                    3,
                    Protobuf.TranslationRequest.parser()
            );


    public static final PacketDefinition<Protobuf.TranslationResult> TRANSLATION_RESULT =
            new PacketDefinition<>(
                    4,
                    Protobuf.TranslationResult.parser()
            );

    public static final PacketDefinition<Protobuf.GeoResultPacket> GEO_RESULT =
            new PacketDefinition<>(
                    5,
                    Protobuf.GeoResultPacket.parser()
            );

    public static final PacketDefinition<Protobuf.GeoRequestPacket> GEO_REQUEST =
            new PacketDefinition<>(
                    6,
                    Protobuf.GeoRequestPacket.parser()
            );

    public static final PacketDefinition<Protobuf.ProfilePacket> PROFILE =
            new PacketDefinition<>(
                    7,
                    Protobuf.ProfilePacket.parser()
            );

    public static final PacketDefinition<Protobuf.ConsentRefreshRequest> CONSENT_REFRESH =
            new PacketDefinition<>(
                    8,
                    Protobuf.ConsentRefreshRequest.parser()
            );

    public static final PacketDefinition<Protobuf.HeartbeatPacket> HEART_BEAT =
            new PacketDefinition<>(
                    9,
                    Protobuf.HeartbeatPacket.parser()
            );

    public static final PacketDefinition<Protobuf.TrackPlayerPacket> TRACK_PLAYER =
            new PacketDefinition<>(
                    10,
                    Protobuf.TrackPlayerPacket.parser()
            );


    public static final PacketDefinition<Protobuf.ProfileExternRequest> PROFILE_REQUEST_EXTERN =
            new PacketDefinition<>(
                    11,
                    Protobuf.ProfileExternRequest.parser()
            );

    public static final PacketDefinition<Protobuf.ProfileExternUpdate> PROFILE_UPDATE_EXTERN =
            new PacketDefinition<>(
                    12,
                    Protobuf.ProfileExternUpdate.parser()
            );

    public static final PacketDefinition<Protobuf.CacheInvalidationRequest> CACHE_INVALIDATION =
            new PacketDefinition<>(
                    13,
                    Protobuf.CacheInvalidationRequest.parser()
            );
}
