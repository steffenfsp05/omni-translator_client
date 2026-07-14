package org.omni.translation;


import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.omni.entity.ServerConfiguration;
import org.omni.entity.TranslationModule;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface TranslatorService {

    Key OMNI_WATERMARK = Key.key("omni:translated");

    CompletableFuture<Boolean> requiresTranslation(UUID playerUUID);

    CompletableFuture<String> translate(String text, String lang, TranslationModule translationModule);

    ServerConfiguration getTranslationConfiguration();

    void setTranslationConfiguration(ServerConfiguration serverConfiguration);


    default boolean isWaterMarked(Component component) {
        if (component.style().font() != null &&
                component.style().font().equals(OMNI_WATERMARK)) {
            return true;
        }
        for (Component child : component.children()) {
            if (isWaterMarked(child)) return true;
        }
        return false;
    }

    default Component setMarked(Component component) {
        return component.append(
                Component.text("").font(OMNI_WATERMARK)
        );
    }

}
