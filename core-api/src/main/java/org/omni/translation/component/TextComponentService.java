package org.omni.translation.component;


import net.kyori.adventure.text.Component;
import org.omni.entity.TranslationModule;

import java.util.concurrent.CompletableFuture;

public interface TextComponentService {

    CompletableFuture<Component> translateComplexMessage(Component originalComponent, String lang, TranslationModule translationModule);

    String sanitizeLegacyText(String text);
}
