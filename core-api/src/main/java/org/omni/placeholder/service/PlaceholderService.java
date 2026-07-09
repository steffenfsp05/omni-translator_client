package org.omni.placeholder.service;

import org.omni.placeholder.BasePlaceholder;
import org.omni.placeholder.normalizer.PlaceholderNormalizer;
import org.omni.placeholder.protector.PlayerNameProtector;

import java.util.Set;
import java.util.UUID;

public interface PlaceholderService {


    boolean registerPlaceholder(int priority, BasePlaceholder placeholder);

    void updateProtectedWords(Set<String> words);
    void rebuildAtomicPattern();

    String toPlaceholders(UUID id, String text);
    String fromPlaceholders(UUID id, String text);

}
