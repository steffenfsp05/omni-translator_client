package org.omni.placeholder.processor;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Singleton;
import org.omni.placeholder.pipeline.TextProcessor;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Singleton
public class SmallCapsProcessor implements TextProcessor {

    private static final char[] SMALL_CAPS = {
            'ᴀ', 'ʙ', 'ᴄ', 'ᴅ', 'ᴇ', 'ꜰ', 'ɢ', 'ʜ', 'ɪ', 'ᴊ', 'ᴋ', 'ʟ', 'ᴍ',
            'ɴ', 'ᴏ', 'ᴘ', 'ǫ', 'ʀ', 'ꜱ', 'ᴛ', 'ᴜ', 'ᴠ', 'ᴡ', 'x', 'ʏ', 'ᴢ'
    };

    private final Cache<UUID, Boolean> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES).build();

    @Override
    public String process(UUID id, String text) {
        if (text == null || text.isBlank()) return text;

        char[] chars = text.toCharArray();
        boolean modified = false;

        for (int i = 0; i < chars.length; i++) {
            char normal = toNormal(chars[i]);
            if (normal != chars[i]) {
                chars[i] = normal;
                modified = true;
            }
        }

        if (modified) {
            cache.put(id, true);
            return new String(chars);
        }

        return text;
    }

    @Override
    public String restore(UUID id, String text) {
        Boolean hadSmallCaps = cache.getIfPresent(id);
        if (text == null || text.isBlank() || hadSmallCaps == null || !hadSmallCaps) {
            return text;
        }

        cache.invalidate(id);
        char[] chars = text.toCharArray();

        boolean inHtml = false;
        boolean inBracket = false;
        boolean modified = false;

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if ((c == '§' || c == '&') && i + 1 < chars.length) {
                char next = chars[i + 1];
                if (next == '#' && i + 7 < chars.length) {
                    i += 7;
                } else if ((next == 'x' || next == 'X') && i + 13 < chars.length) {
                    i += 13;
                } else {
                    i++;
                }
                continue;
            }

            if (c == '<') { inHtml = true; continue; }
            if (c == '>') { inHtml = false; continue; }
            if (c == '{') { inBracket = true; continue; }
            if (c == '}') { inBracket = false; continue; }

            if (!inHtml && !inBracket) {
                char smallCap = toSmallCap(c);
                if (smallCap != c) {
                    chars[i] = smallCap;
                    modified = true;
                }
            }
        }

        return modified ? new String(chars) : text;
    }

    private char toSmallCap(char c) {
        if (c >= 'a' && c <= 'z') return SMALL_CAPS[c - 'a'];
        if (c >= 'A' && c <= 'Z') return SMALL_CAPS[c - 'A'];
        return c;
    }


    private char toNormal(char c) {
        for (int i = 0; i < SMALL_CAPS.length; i++) {
            if (SMALL_CAPS[i] == c) {
                return (char) ('A' + i);
            }
        }
        return c;
    }
}