package org.pytenix.placeholder;

import org.pytenix.placeholder.impl.ExtendedPlaceholder;
import org.pytenix.placeholder.protect.PlayerNameProtector;

import java.util.Set;
import java.util.UUID;

public interface PlaceholderService {


    boolean registerPlaceholder(int priority, ExtendedPlaceholder placeholder);


    void updateProtectedWords(Set<String> words);

    void rebuildAtomicPattern();

    String toPlaceholders(UUID id, String text);

    String fromPlaceholders(UUID id, String text);


    PlaceholderNormalizer getPlaceholderNormalizer();

    PlayerNameProtector getPlayerNameProtector();
}
