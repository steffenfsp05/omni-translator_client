package org.pytenix.network.consumer;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.pytenix.packets.MappedPacketReceiveConsumer;
import org.pytenix.packets.impl.ProfileMapper;
import org.pytenix.packets.impl.TranslationRequestMapper;
import org.pytenix.proto.generated.NetworkPackets;
import org.transport.service.PacketContext;

import java.util.concurrent.CompletableFuture;

public class ProfileConsumer implements MappedPacketReceiveConsumer<RegisteredServer, NetworkPackets.ProfilePacket, ProfileMapper.ProfileData> {


    @Override
    public void handle(PacketContext<RegisteredServer> context, ProfileMapper.ProfileData javaPacket) {

    }
}
