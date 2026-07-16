package org.omni.translation.module;


import lombok.Getter;
import org.omni.entity.ServerConfiguration;
import org.omni.entity.TranslationModule;
import org.omni.translation.TranslatorService;
import org.omni.translation.locale.PlayerLocaleProcessor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public abstract class AbstractTranslatorModule {


    final TranslatorService translatorService;
    final PlayerLocaleProcessor playerLocaleProcessor;

    final TranslationModule translationModule;


    public AbstractTranslatorModule(
            TranslatorService translatorService,
            PlayerLocaleProcessor playerLocaleProcessor,
            TranslationModule translationModule
    ) {

        this.translatorService = translatorService;
        this.playerLocaleProcessor = playerLocaleProcessor;
        this.translationModule = translationModule;
    }


    public void init() {
    }

    public boolean isModuleActive() {
        return getServerConfiguration().getModules().getOrDefault(translationModule, true);
    }

    public CompletableFuture<Boolean> requiresTranslation(UUID playerUUID) {
        return translatorService.requiresTranslation(playerUUID);
    }

    public ServerConfiguration getServerConfiguration() {
        return translatorService.getTranslationConfiguration();
    }


    private String generateKey(String text, String lang) {
        return text + ":" + lang;
    }


    public CompletableFuture<String> translate(String text, String locale) {


        //TODO: IMPLEMENT!!!
        //  String cached = translatorPlugin.getCaffeineCache().get(generateKey(text, locale));

        //  if (cached != null)
        //      return CompletableFuture.completedFuture(cached);

        return translatorService.translate(text, locale, this.translationModule).whenComplete((result, throwable) -> {

            if (throwable == null && result != null) {
                //translatorPlugin.getCaffeineCache().set(generateKey(text, locale), result);
            }

        });
    }


}
