package org.pytenix.network.consumer;

import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.pytenix.TranslatorPlugin;
import org.pytenix.entity.ServerConfiguration;
import org.pytenix.event.register.ConfigUpdateEvent;
import org.pytenix.event.register.ConsentUpdateEvent;
import org.pytenix.packets.MappedPacketReceiveConsumer;
import org.pytenix.packets.impl.ConsentRefreshRequestMapper;
import org.pytenix.proto.generated.NetworkPackets;
import org.pytenix.translation.TranslatorService;
import org.transport.service.PacketContext;

@AllArgsConstructor
public class ConsentRefreshConsumer implements MappedPacketReceiveConsumer<String, NetworkPackets.ConsentRefreshRequest, ConsentRefreshRequestMapper.Data> {
    final TranslatorPlugin translatorPlugin;
    final TranslatorService translatorService;

    @Override
    public void handle(PacketContext<String> context, ConsentRefreshRequestMapper.Data javaPacket) {

        if(Bukkit.getPlayer(javaPacket.playerId()) != null)
            translatorService.getEventService().callEvent(new ConsentUpdateEvent(javaPacket));
    }
}
