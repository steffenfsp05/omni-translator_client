package org.pytenix.translation;

import lombok.Getter;
import org.pytenix.entity.ServerConfiguration;
import org.pytenix.translation.locale.PlayerLocaleProcessor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public abstract class AbstractTranslatorModule {


    final TranslatorService translatorService;
    final PlayerLocaleProcessor playerLocaleProcessor;

    final String moduleName;


    public AbstractTranslatorModule(TranslatorService translatorService, String moduleName, PlayerLocaleProcessor playerLocaleProcessor) {

        this.translatorService = translatorService;
        this.playerLocaleProcessor = playerLocaleProcessor;
        this.moduleName = moduleName;
    }


    public boolean isActive() {
        return getServerConfiguration().getModules().getOrDefault(moduleName, true);
    }

    public boolean checkIfNeed(UUID playerUUID) {

        if (getServerConfiguration() == null || getServerConfiguration().getDefaultLanguage() == null)
            return true;

        return !playerLocaleProcessor.retrieveLocale(playerUUID).startsWith(getServerConfiguration().getDefaultLanguage());
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

        return translatorService.translate(text, locale, this.moduleName).whenComplete((result, throwable) -> {

            if (throwable == null && result != null) {
                //translatorPlugin.getCaffeineCache().set(generateKey(text, locale), result);
            }

        });
    }


}
