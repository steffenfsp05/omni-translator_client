package org.pytenix.limbo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ConsentMessageFactory {
    private static final String CONSENT_TEXT = """
                        
            <gold><b>🌍 Enable Live Translation?</b></gold>
            <gray>To help everyone communicate, our AI translates <white>Chat (players & plugins), Private Messages, GUIs, Holograms, Tab lists, and Scoreboards</white>.</gray>
                        
            <green>🔒 <b>Your Privacy:</b></green>
            <gray>We only process <b>raw text</b>. Your username, UUID, and IP are <red><b>never</b></red> transmitted. You remain completely anonymous.</gray>
                        
            <red>⚠️ <b>Warning:</b></red>
            <gray>Please never share sensitive information (like passwords or real names) anywhere on the server.</gray>
                        
            <gray>Do you agree to anonymous text processing?</gray>
            <dark_gray><i>(You can change this anytime with /translate toggle)</i></dark_gray>
                        
            <click:run_command:'/translate accept'><hover:show_text:'<green>Click to accept translation</green>'><green><b>[ ✔ Accept ]</b></green></hover></click> <dark_gray>|</dark_gray> <click:run_command:'/translate decline'><hover:show_text:'<red>Click to decline translation</red>'><red><b>[ ✖ Decline ]</b></red></hover></click>
                        
            """;


    public static Component build() {
        return MiniMessage.miniMessage().deserialize(CONSENT_TEXT);
    }
}
