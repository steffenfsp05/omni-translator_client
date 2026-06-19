package org.pytenix.placeholder.protect;

import org.pytenix.placeholder.protect.impl.DefaultPlayerNameProtector;

import java.util.Map;

public interface PlayerNameProtector {

    void addPlayer(String name);

    void removePlayer(String name);


    DefaultPlayerNameProtector.ProtectionResult maskNames(String text);


    String restoreNames(String translatedText, Map<String, String> replacements);


    record ProtectionResult(String maskedText, Map<String, String> replacements) {
    }
}
