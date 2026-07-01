package org.pytenix.packets;

import org.pytenix.proto.generated.NetworkPackets;
import org.transport.service.impl.PacketDefinition;

public class PacketRegistry {

    public static final PacketDefinition<NetworkPackets.ConfigRequestPacket> CONFIG_REQUEST =
            new PacketDefinition<>(
                    1,
                    NetworkPackets.ConfigRequestPacket.parser()
            );


    public static final PacketDefinition<NetworkPackets.ServerConfiguration> SERVER_CONFIG =
            new PacketDefinition<>(
                    2,
                    NetworkPackets.ServerConfiguration.parser()
            );

    public static final PacketDefinition<NetworkPackets.TranslationRequest> TRANSLATION_REQUEST =
            new PacketDefinition<>(
                    3,
                    NetworkPackets.TranslationRequest.parser()
            );


    public static final PacketDefinition<NetworkPackets.TranslationResult> TRANSLATION_RESULT =
            new PacketDefinition<>(
                    4,
                    NetworkPackets.TranslationResult.parser()
            );

    public static final PacketDefinition<NetworkPackets.GeoResultPacket> GEO_RESULT =
            new PacketDefinition<>(
                    5,
                    NetworkPackets.GeoResultPacket.parser()
            );

    public static final PacketDefinition<NetworkPackets.GeoRequestPacket> GEO_REQUEST =
            new PacketDefinition<>(
                    6,
                    NetworkPackets.GeoRequestPacket.parser()
            );

    public static final PacketDefinition<NetworkPackets.ProfilePacket> PROFILE =
            new PacketDefinition<>(
                    7,
                    NetworkPackets.ProfilePacket.parser()
            );

    public static final PacketDefinition<NetworkPackets.ConsentRefreshRequest> CONSENT_REFRESH =
            new PacketDefinition<>(
                    8,
                    NetworkPackets.ConsentRefreshRequest.parser()
            );

    public static final PacketDefinition<NetworkPackets.HeartbeatPacket> HEART_BEAT =
            new PacketDefinition<>(
                    9,
                    NetworkPackets.HeartbeatPacket.parser()
            );

    public static final PacketDefinition<NetworkPackets.TrackPlayerPacket> TRACK_PLAYER =
            new PacketDefinition<>(
                    10,
                    NetworkPackets.TrackPlayerPacket.parser()
            );

}
