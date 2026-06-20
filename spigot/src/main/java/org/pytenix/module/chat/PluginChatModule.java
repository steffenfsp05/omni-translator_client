package org.pytenix.module.chat;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import lombok.Getter;
import org.pytenix.TranslatorPlugin;
import org.pytenix.module.chat.listener.SystemPacketListener;
import org.pytenix.module.TranslatorModule;

@Getter
public class PluginChatModule extends TranslatorModule {

    MessageSequencer messageSequencer;

    public PluginChatModule(TranslatorPlugin translatorPlugin) {
        super(translatorPlugin, "plugin_chat");

        this.messageSequencer = new MessageSequencer(this);

        PacketEvents.getAPI().getEventManager().registerListener(new SystemPacketListener(this),
                PacketListenerPriority.HIGHEST);

    }


}
