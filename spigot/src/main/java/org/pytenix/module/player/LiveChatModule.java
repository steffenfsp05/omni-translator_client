package org.pytenix.module.player;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.pytenix.TranslatorPlugin;
import org.pytenix.module.player.listener.AsyncPlayerChatListener;
import org.pytenix.service.TranslatorModule;

public class LiveChatModule extends TranslatorModule {


    public LiveChatModule(TranslatorPlugin translatorPlugin) {
        super(translatorPlugin, "live_chat");


        new AsyncPlayerChatListener(this);

        //   PacketEvents.getAPI().getEventManager().registerListener(new ChatPacketListener(this),
        //        PacketListenerPriority.HIGHEST);
    }


    public void sendSystemMessage(Player player, Component content) {

        WrapperPlayServerSystemChatMessage systemPacket = new WrapperPlayServerSystemChatMessage(
                false,
                content
        );

        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, systemPacket);
    }

}
