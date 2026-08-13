package org.pytenix.modules.disconnect.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.player.OmniAsyncPlayerPreDisconnectEvent;
import org.omni.translation.TranslatorService;
import org.omni.translation.locale.PlayerLocaleProcessor;
import org.pytenix.modules.disconnect.DisconnectConnectionModule;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class OmniPlayerPreDisconnectListener {


    private final DisconnectConnectionModule disconnectConnectionModule;
    private final TranslatorService translatorService;
    private final PlayerLocaleProcessor playerLocaleProcessor;

    @Inject
    public OmniPlayerPreDisconnectListener(TranslatorService translatorService, DisconnectConnectionModule disconnectConnectionModule, PlayerLocaleProcessor playerLocaleProcessor) {
        this.translatorService = translatorService;
        this.disconnectConnectionModule = disconnectConnectionModule;
        this.playerLocaleProcessor = playerLocaleProcessor;
    }

    @OmniSubscribe(priority = 10)
    public CompletableFuture<Void> onPlayerDisconnect(OmniAsyncPlayerPreDisconnectEvent event) {

        System.out.println("CALLED!!!!");
        // NEED RESET OF THE OLD CONFIG IN REDIS!
   //     if (!disconnectConnectionModule.isModuleActive()) {
     //       return CompletableFuture.completedFuture(null);
     //   }
        System.out.println("CALLED!!!! 2");
        final UUID playerId = event.getPlayerId();
        System.out.println("CALLED!!!! 3");

        return translatorService.requiresTranslation(playerId).thenCompose(requiresTranslation -> {
            System.out.println("CALLED!!!! 4");
            if (requiresTranslation) {
                System.out.println("CALLED!!!! 5");
                return playerLocaleProcessor.retrieveLocale(playerId).thenCompose(locale ->
                        translatorService.translate(event.getReason(), locale, disconnectConnectionModule.getTranslationModule())
                                .thenAccept(s ->
                                {
                                    System.out.println("CALLED!!!! 67");
                                    System.out.println("REASON-: " + s);
                                    event.setReason(s);
                                }));
            }
            System.out.println("CALLED!!!! 6");
            return CompletableFuture.completedFuture(null);
        });

    }

}


