package org.omni.translation.component;


import net.kyori.adventure.text.Component;

import java.util.concurrent.CompletableFuture;

public interface TextComponentService {

    CompletableFuture<Component> translateComplexMessage(Component originalComponent, String lang, String module);

    String sanitizeLegacyText(String text);
}
