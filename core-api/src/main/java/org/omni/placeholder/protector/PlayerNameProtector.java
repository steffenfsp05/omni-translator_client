package org.omni.placeholder.protector;


import java.util.Map;

public interface PlayerNameProtector {

    void addPlayer(String name);
    void removePlayer(String name);

    ProtectionResult maskNames(String text);

    String restoreNames(String translatedText, Map<String, String> replacements);

}
