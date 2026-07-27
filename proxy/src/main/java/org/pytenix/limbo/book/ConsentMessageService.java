package org.pytenix.limbo.book;

import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.omni.entity.TranslationModule;
import org.omni.translation.component.TextComponentService;
import org.omni.translation.locale.PlayerLocaleProcessor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ConsentMessageService {

    TextComponentService textComponentService;
    PlayerLocaleProcessor playerLocaleProcessor;

    @Inject
    public ConsentMessageService(TextComponentService textComponentService, PlayerLocaleProcessor playerLocaleProcessor)
    {
        this.textComponentService = textComponentService;
        this.playerLocaleProcessor = playerLocaleProcessor;
    }

    private static final String CONSENT_MESSAGE = """
            
            <gold><strikethrough>                                        </strikethrough></gold>
            <yellow><bold>OmniTranslator Datenschutz</bold></yellow>
            <gray>Bitte wähle deine Einstellungen für diesen Server:</gray>
            
            <dark_gray>1.</dark_gray> <white><b>Chat-Übersetzung</b></white> <gray><i>(Chat/GUIs)</i></gray>
               <click:run_command:'/translate translation accept'><hover:show_text:'<green>Aktivieren</green>'><green><b>[✔ Erlauben]</b></green></hover></click>   <click:run_command:'/translate translation decline'><hover:show_text:'<red>Deaktivieren</red>'><red><b>[✖ Ablehnen]</b></red></hover></click>
            
            <dark_gray>2.</dark_gray> <white><b>Analytics & Tracking</b></white> <gray><i>(Anonym)</i></gray>
               <click:run_command:'/translate analytics accept'><hover:show_text:'<green>Aktivieren</green>'><green><b>[✔ Erlauben]</b></green></hover></click>   <click:run_command:'/translate analytics decline'><hover:show_text:'<red>Deaktivieren</red>'><red><b>[✖ Ablehnen]</b></red></hover></click>
            
            <gray>Schnellauswahl:</gray>
            <click:run_command:'/translate all accept'><hover:show_text:'<green>Alles akzeptieren und spielen</green>'><dark_green><b>[ ALLES AKZEPTIEREN ]</b></dark_green></hover></click>  <click:run_command:'/translate all decline'><hover:show_text:'<red>Alles ablehnen und spielen</red>'><dark_red><b>[ ALLES ABLEHNEN ]</b></dark_red></hover></click>
            <gold><strikethrough>                                        </strikethrough></gold>
            
            """;

    public CompletableFuture<Component> getConsentMessage(UUID uuid) {

        Component component = MiniMessage.miniMessage().deserialize(CONSENT_MESSAGE);

        return playerLocaleProcessor.retrieveLocale(uuid).thenCompose(locale ->
                textComponentService.translateComplexMessage(component, locale, TranslationModule.PLUGIN_CHAT)
        );


    }
}
