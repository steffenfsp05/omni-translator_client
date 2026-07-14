package org.pytenix.module.player;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.omni.entity.TranslationModule;
import org.omni.profile.ProfileService;
import org.omni.translation.TranslatorService;
import org.omni.translation.locale.PlayerLocaleProcessor;
import org.omni.translation.module.AbstractTranslatorModule;
import org.pytenix.TranslatorPlugin;
import org.pytenix.module.player.listener.AsyncPlayerChatListener;

@Singleton
public class LiveChatModule extends AbstractTranslatorModule {


    final TranslatorPlugin translatorPlugin;
    final AsyncPlayerChatListener chatListener;

    @Inject
    public LiveChatModule(
            ProfileService profileService,
            TranslatorService translatorService,
            PlayerLocaleProcessor playerLocaleProcessor,
            TranslatorPlugin translatorPlugin,
            AsyncPlayerChatListener chatListener
    ) {

        super(profileService, translatorService, playerLocaleProcessor, TranslationModule.LIVE_CHAT);

        this.translatorPlugin = translatorPlugin;
        this.chatListener = chatListener;


    }

    @Override
    public void init() {
        Bukkit.getPluginManager().registerEvents(chatListener, translatorPlugin);
    }

    public void sendSystemMessage(Player player, Component content) {

        Component markedComponent = getTranslatorService().setMarked(content);

        WrapperPlayServerSystemChatMessage systemPacket = new WrapperPlayServerSystemChatMessage(
                false,
                markedComponent
        );

        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, systemPacket);
    }
}