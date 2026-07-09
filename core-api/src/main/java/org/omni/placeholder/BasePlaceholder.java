package org.omni.placeholder;

import com.google.common.cache.Cache;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public interface BasePlaceholder {

    Pattern getPattern();

    String placeholder();

    Cache<UUID, Map<Integer, String>> cachedValues();
}
