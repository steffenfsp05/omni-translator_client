package org.pytenix.module;

import lombok.Getter;
import org.pytenix.TranslatorPlugin;
import org.pytenix.entity.ServerConfiguration;
import org.pytenix.service.PlayerLocaleService;
import org.pytenix.translation.TranslatorService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public abstract class AbstractTranslatorModule {


    final TranslatorPlugin translatorPlugin;

    final String moduleName;


    final TranslatorService translatorService;

    public AbstractTranslatorModule(TranslatorPlugin translatorPlugin, String moduleName) {

        this.translatorPlugin = translatorPlugin;
        this.moduleName = moduleName;

        this.translatorService = translatorPlugin.getTranslatorService();
    }


    public boolean isActive() {
        return getServerConfiguration().getModules().getOrDefault(moduleName, true);
    }

    public boolean checkIfNeed(UUID playerUUID) {

        if (getServerConfiguration() == null || getServerConfiguration().getDefaultLanguage() == null)
            return true;

        return !PlayerLocaleService.getPlayerLocale(playerUUID).startsWith(getServerConfiguration().getDefaultLanguage());
    }

    public ServerConfiguration getServerConfiguration() {
        return translatorPlugin.getTranslatorService().getTranslationConfiguration();
    }


    private String generateKey(String text, String lang) {
        return text + ":" + lang;
    }


    public CompletableFuture<String> translate(String text, String locale) {


        String cached = translatorPlugin.getCaffeineCache().get(generateKey(text, locale));

        if (cached != null)
            return CompletableFuture.completedFuture(cached);

        return translatorService.translate(text, locale, this.moduleName).whenComplete((result, throwable) -> {

            if (throwable == null && result != null) {
                translatorPlugin.getCaffeineCache().set(generateKey(text, locale), result);
            }

        });
    }


}
