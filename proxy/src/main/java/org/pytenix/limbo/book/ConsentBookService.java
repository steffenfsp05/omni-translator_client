package org.pytenix.limbo.book;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.omni.entity.TranslationModule;
import org.omni.translation.component.TextComponentService;
import org.omni.translation.locale.PlayerLocaleProcessor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class ConsentBookService {


    TextComponentService textComponentService;
    PlayerLocaleProcessor playerLocaleProcessor;

    @Inject
    public ConsentBookService(TextComponentService textComponentService, PlayerLocaleProcessor playerLocaleProcessor)
    {
        this.textComponentService = textComponentService;
        this.playerLocaleProcessor = playerLocaleProcessor;
    }


    private static final String SINGLE_PAGE_CONSENT = """
            <bold><dark_red>OmniTranslator</dark_red></bold>
            <dark_gray>Bitte wähle deine Option:</dark_gray>

            <black><b>1. Chat-Übersetzung</b></black>
            <click:run_command:'/translate translation accept'><hover:show_text:'<green>Erlauben</green>'><dark_green><b>[✔]</b></dark_green></hover></click> <click:run_command:'/translate translation decline'><hover:show_text:'<red>Ablehnen</red>'><dark_red><b>[✖]</b></dark_red></hover></click> <gray><i>(Chat/GUIs)</i></gray>

            <black><b>2. Analytics & ROI</b></black>
            <click:run_command:'/translate analytics accept'><hover:show_text:'<green>Erlauben</green>'><dark_green><b>[✔]</b></dark_green></hover></click> <click:run_command:'/translate analytics decline'><hover:show_text:'<red>Ablehnen</red>'><dark_red><b>[✖]</b></dark_red></hover></click> <gray><i>(Anonym)</i></gray>

            <dark_gray>───────────────────</dark_gray>
            <click:run_command:'/translate all accept'><hover:show_text:'<green>Alles erlauben und verbinden</green>'><dark_green><b>[ Alle Akzeptieren ]</b></dark_green></hover></click>

            <click:run_command:'/translate all decline'><hover:show_text:'<red>Alles ablehnen und verbinden</red>'><dark_red><b>[ Alle Ablehnen ]</b></dark_red></hover></click>
            """;

    public CompletableFuture<Book> buildBook(UUID uuid) {
        Component page = MiniMessage.miniMessage().deserialize(SINGLE_PAGE_CONSENT);

        //final String locale = playerLocaleProcessor.retrieveLocale(uuid);
       /* return textComponentService.translateComplexMessage(page, locale, TranslationModule.PLUGIN_CHAT).thenApply(component ->
                Book.builder()
                        .title(Component.text("Datenschutz"))
                        .author(Component.text("OmniTranslator"))
                        .addPage(page)
                        .build());


        */

        CompletableFuture<Book> completableFuture = new CompletableFuture<>();

        completableFuture.complete(Book.builder()
                .title(Component.text("Datenschutz"))
                .author(Component.text("OmniTranslator"))
                .addPage(page)
                .build());

        return completableFuture;

    }
}