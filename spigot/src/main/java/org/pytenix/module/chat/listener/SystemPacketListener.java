package org.pytenix.module.chat.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.pytenix.service.PlayerLocaleService;
import org.pytenix.module.chat.MessageSequencer;
import org.pytenix.module.chat.PluginChatModule;
import org.pytenix.util.TextComponentUtil;

import java.util.UUID;

public class SystemPacketListener implements PacketListener {

/*
    final PluginChatModule pluginChatModule;
    final MessageSequencer messageSequencer;

    final TextComponentUtil textComponentUtil;

    public SystemPacketListener(PluginChatModule pluginChatModule) {
        this.pluginChatModule = pluginChatModule;
        this.messageSequencer = pluginChatModule.getMessageSequencer();
        this.textComponentUtil = new TextComponentUtil(pluginChatModule.getTranslatorService());

    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.SYSTEM_CHAT_MESSAGE) {
            if (event.isCancelled() || !pluginChatModule.isActive()) return;

            Player player = org.bukkit.Bukkit.getPlayer(event.getUser().getUUID());
            if (player == null) return;

            final UUID uuid = player.getUniqueId();
            if (!pluginChatModule.checkIfNeed(uuid)) return;

            WrapperPlayServerSystemChatMessage packet = new WrapperPlayServerSystemChatMessage(event);
            boolean isOverlay = packet.isOverlay();

            if (isOverlay) return;

            Component messageComponent = packet.getMessage();
            String rawText = LegacyComponentSerializer.legacySection().serialize(messageComponent);

            if (rawText.contains("Can't deliver chat message") || rawText.contains("kann nicht zugestellt werden") || rawText.contains("multiplayer.message_not_delivered")) {
                event.setCancelled(true);
                return;
            }

            if (messageSequencer.isIgnored(uuid, messageComponent)) {
                return;
            }

            event.setCancelled(true);
            messageSequencer.translateWithOrder(uuid, messageComponent, rawText, PlayerLocaleService.getPlayerLocale(player.getUniqueId()), isOverlay);
        }
    }
     */
}