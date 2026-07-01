package org.pytenix.module.player;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.pytenix.TranslatorPlugin;
import org.pytenix.module.player.listener.AsyncPlayerChatListener;
import org.pytenix.profile.ProfileService;
import org.pytenix.translation.AbstractTranslatorModule;
import org.pytenix.translation.TranslatorService;
import org.pytenix.translation.locale.PlayerLocaleProcessor;

public class LiveChatModule extends AbstractTranslatorModule {


    public LiveChatModule(
            ProfileService profileService,
            TranslatorPlugin translatorPlugin,
            TranslatorService translatorService,
            PlayerLocaleProcessor playerLocaleProcessor
    ) {
        super(profileService, translatorService, "live_chat", playerLocaleProcessor);


        Bukkit.getPluginManager().registerEvents(new AsyncPlayerChatListener(this, translatorPlugin), translatorPlugin);

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
