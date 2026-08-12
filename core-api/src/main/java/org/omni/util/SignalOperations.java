package org.omni.util;

import org.omni.entity.TranslationModule;
import org.omni.packets.data.CacheInvalidationRequest;

import java.util.function.Predicate;

public final class SignalOperations {

    private SignalOperations() { }

    private static final byte[] HEADER = new byte[] { 'O', 'M', 'N', 'I' };

    public static Predicate<CacheInvalidationRequest.Translation> CACHE_TRANSLATION_INVALIDATION_ALL =
            translation -> translation.text().equals("*") && translation.language().equals("*") && translation.translationModule().equals(TranslationModule.LIVE_CHAT);

    public static final byte SIGNAL_PROFILE_ALL = 1;

    public static final byte[] CACHE_PROFILE_INVALIDATION_ALL = createSignal(SIGNAL_PROFILE_ALL);

    public static byte[] createSignal(byte signal) {
        byte[] packet = new byte[HEADER.length + 1];
        System.arraycopy(HEADER, 0, packet, 0, HEADER.length);
        packet[HEADER.length] = signal;
        return packet;
    }
    public static byte getSignal(byte[] data) {
        if (data == null || data.length < HEADER.length + 1) {
            return -1;
        }

        for (int i = 0; i < HEADER.length; i++) {
            if (data[i] != HEADER[i]) {
                return -1;
            }
        }

        return data[HEADER.length];
    }
    public static boolean isSignal(byte[] data, byte expectedSignal) {
        return getSignal(data) == expectedSignal;
    }
}
