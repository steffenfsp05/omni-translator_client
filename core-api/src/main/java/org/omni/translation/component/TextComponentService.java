package org.omni.translation.component;


import net.kyori.adventure.text.Component;
import org.omni.cache.CacheProvider;
import org.omni.entity.TranslationModule;

import java.util.concurrent.CompletableFuture;

public interface TextComponentService extends CacheProvider<TextComponentService.TranslationKey, Component> {

    CompletableFuture<Component> translateComplexMessage(Component originalComponent, String lang, TranslationModule translationModule);

    String sanitizeLegacyText(String text);

    record TranslationKey(Component component, String lang, TranslationModule translationModule) {
    }


}
