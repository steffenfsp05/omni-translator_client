package org.pytenix.chat;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import lombok.Getter;
import org.pytenix.TranslatorPlugin;
import org.pytenix.chat.listener.PlayerDisconnectListener;
import org.pytenix.chat.listener.SystemChatPacketListener;
import org.pytenix.entity.ServerConfiguration;
import org.pytenix.translation.AbstractTranslatorModule;
import org.pytenix.translation.TranslatorService;
import org.pytenix.translation.locale.PlayerLocaleProcessor;
import org.pytenix.util.TextComponentUtil;


@Getter
public class SystemChatModule extends AbstractTranslatorModule {

    final TranslatorPlugin translatorPlugin;

    final TextComponentUtil textComponentUtil;
    final MessageSequencer messageSequencer;

    public SystemChatModule(
            TranslatorPlugin translatorPlugin,
            TranslatorService translatorService,
            TextComponentUtil textComponentUtil,
            MessageSequencer messageSequencer,
            PlayerLocaleProcessor playerLocaleProcessor
    ) {
        super(translatorService, "plugin_chat", playerLocaleProcessor);
        this.translatorPlugin = translatorPlugin;
        this.textComponentUtil = textComponentUtil;
        this.messageSequencer = messageSequencer;

        registerListener();
    }


    public void registerListener() {
        PacketEvents.getAPI().getEventManager().registerListener(
                new SystemChatPacketListener(this, messageSequencer),
                PacketListenerPriority.HIGHEST
        );
        translatorPlugin.getProxyServer().getEventManager().register(translatorPlugin, new PlayerDisconnectListener(translatorPlugin));
    }


    public boolean isModuleActive() {
        return translatorPlugin.getTranslatorService().getTranslationConfiguration().getModules().get(ServerConfiguration.Module.PLUGIN_CHAT
                .getModuleName());
    }

}
