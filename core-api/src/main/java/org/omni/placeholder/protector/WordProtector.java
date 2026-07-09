package org.omni.placeholder.protector;

import java.util.Map;
import java.util.Set;

public interface WordProtector {

    void build(Set<String> words);

    ProtectionResult protect(String text);

    String restore(String text, Map<String, String> replacements);


}