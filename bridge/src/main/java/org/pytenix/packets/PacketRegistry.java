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


}
