package org.omni.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum TranslationModule {

        LIVE_CHAT("live_chat"),
        GUI("gui"),
        HOLOGRAM("hologram"),
        PLUGIN_CHAT("plugin_chat"),
        SIGNS("signs"),
        MOTD("motd");


        String moduleName;


        public static TranslationModule getModule(String name) {
            return valueOf(name.toUpperCase());
        }

}
