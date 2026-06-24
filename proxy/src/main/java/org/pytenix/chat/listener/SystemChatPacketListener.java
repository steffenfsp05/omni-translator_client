package org.pytenix.chat.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.pytenix.chat.MessageSequencer;
import org.pytenix.chat.SystemChatModule;
import org.pytenix.proto.generated.NetworkPackets;

import java.util.UUID;

@RequiredArgsConstructor
public class SystemChatPacketListener implements PacketListener {

    private final SystemChatModule systemChatService;
    private final MessageSequencer messageSequencer;


    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.SYSTEM_CHAT_MESSAGE) {
            if (event.isCancelled() || !systemChatService.isModuleActive()) return;

            UUID uuid = event.getUser().getUUID();
            if (uuid == null) return;

            Player player = systemChatService.getTranslatorPlugin().getProxyServer().getPlayer(uuid).orElse(null);
            if (player == null) return;


            if (!systemChatService.checkIfNeed(uuid)) return;

            WrapperPlayServerSystemChatMessage packet = new WrapperPlayServerSystemChatMessage(event);
            boolean isOverlay = packet.isOverlay();

            if (isOverlay) return;

            Component messageComponent = packet.getMessage();
            String rawText = LegacyComponentSerializer.legacySection().serialize(messageComponent);

            if (rawText.contains("Can't deliver chat message") ||
                    rawText.contains("kann nicht zugestellt werden") ||
                    rawText.contains("multiplayer.message_not_delivered")
            ) {
                event.setCancelled(true);
                return;
            }

            if (messageSequencer.isIgnored(uuid, messageComponent))
                return;


            //TODO: REFACTOR!!!!

            systemChatService.getTranslatorPlugin().getProfileSocketEndpoint().getProfile(uuid)
                    .thenAcceptAsync(profileData ->
                    {
                        if (profileData.consentType() == NetworkPackets.ProfilePacket.ConsentType.DECLINED)
                            return;

                        event.setCancelled(true);


                        messageSequencer.translateWithOrder(
                                uuid,
                                messageComponent,
                                rawText,
                                systemChatService.getPlayerLocaleProcessor().retrieveLocale(uuid),
                                isOverlay
                        );

                    });


        }
    }
}