package org.pytenix.module.chat.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.omni.translation.TranslatorService;
import org.pytenix.module.chat.MessageSequencer;
import org.pytenix.module.chat.SystemChatModule;

import java.util.UUID;

@Singleton
public class SystemChatPacketListener implements PacketListener {

    private final TranslatorService translatorService;
    private final SystemChatModule systemChatService;
    private final MessageSequencer messageSequencer;
    private final ProxyServer proxyServer;

    @Inject
    public SystemChatPacketListener(
            TranslatorService translatorService,
            SystemChatModule systemChatService,
            MessageSequencer messageSequencer,
            ProxyServer proxyServer) {
        this.translatorService = translatorService;
        this.systemChatService = systemChatService;
        this.messageSequencer = messageSequencer;
        this.proxyServer = proxyServer;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.SYSTEM_CHAT_MESSAGE) return;
        if (event.isCancelled() || !systemChatService.isModuleActive()) return;

        UUID uuid = event.getUser().getUUID();
        if (uuid == null) return;

        Player player = proxyServer.getPlayer(uuid).orElse(null);
        if (player == null) return;

        WrapperPlayServerSystemChatMessage packet = new WrapperPlayServerSystemChatMessage(event);
        boolean isOverlay = packet.isOverlay();

        if (isOverlay) return;

        Component messageComponent = packet.getMessage();

        if (translatorService.isWaterMarked(messageComponent))
            return;


        String rawText = LegacyComponentSerializer.legacySection().serialize(messageComponent);

        if (rawText.contains("Can't deliver chat message") ||
                rawText.contains("kann nicht zugestellt werden") ||
                rawText.contains("multiplayer.message_not_delivered")
        ) {
            event.setCancelled(true);
            return;
        }


        event.setCancelled(true);

        systemChatService.requiresTranslation(uuid).thenAcceptAsync(requiresTranslation ->
        {
            if (!requiresTranslation) {

                player.sendMessage(
                        translatorService.setMarked(messageComponent)
                );
                return;
            }

            systemChatService.getPlayerLocaleProcessor().retrieveLocale(uuid).thenAccept(locale ->
                    messageSequencer.translateWithOrder(
                            uuid,
                            messageComponent,
                            locale,
                            isOverlay
                    ));

        });


    }
}