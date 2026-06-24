package org.pytenix.module.chat;

import lombok.Getter;

@Getter
public class PluginChatModule {

    /* WIRD ABGELÖST VOM PROXY
    MessageSequencer messageSequencer;

    public PluginChatModule(TranslatorPlugin translatorPlugin) {
        super(translatorPlugin, "plugin_chat");

        this.messageSequencer = new MessageSequencer(this);

        PacketEvents.getAPI().getEventManager().registerListener(new SystemPacketListener(this),
                PacketListenerPriority.HIGHEST);

    }


     */

}
