package org.pytenix.network.consumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.omni.event.EventService;
import org.omni.event.register.ConsentUpdateEvent;
import org.omni.packets.MappedPacketReceiveConsumer;
import org.omni.packets.data.ConsentRefreshRequestData;
import org.omni.profile.ProfileService;
import org.omni.proto.generated.Protobuf;
import org.transport.service.PacketContext;

@Singleton
public class ConsentRefreshConsumer extends MappedPacketReceiveConsumer<String, Protobuf.ConsentRefreshRequest, ConsentRefreshRequestData> {

    private final ProfileService profileService;
    private final EventService eventService;

    @Inject
    public ConsentRefreshConsumer(ProfileService profileService, EventService eventService) {
        this.profileService = profileService;
        this.eventService = eventService;
    }

    @Override
    public void handle(PacketContext<String> context, ConsentRefreshRequestData javaPacket) {
        profileService.cacheProvider().invalidate(javaPacket.playerId());
        if (Bukkit.getPlayer(javaPacket.playerId()) != null) {
            eventService.callEvent(new ConsentUpdateEvent(javaPacket));
        }
    }
}