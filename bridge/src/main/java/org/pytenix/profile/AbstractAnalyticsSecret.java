package org.pytenix.profile;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.pytenix.packets.impl.ProfileMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractAnalyticsSecret {
    private String serverSalt;

    private final Cache<AnalyticsKey, UUID> reverseLookupMap = Caffeine.newBuilder()
            .maximumSize(10000)
            .build();

    protected abstract String getStoredSalt();
    protected abstract void saveSaltToConfig(String salt);
    protected abstract void logInfo(String message);
    protected abstract void logError(String message, Throwable throwable);

    public AnalyticsKey getAnalyticsKey(UUID uuid) {
        if (uuid == null) return null;

        byte[] bytes = getAnalyticsByteId(uuid);
        if (bytes == null) return null;

        AnalyticsKey key = new AnalyticsKey(bytes);

        // Hier merken wir uns den Rückweg!
        reverseLookupMap.put(key, uuid);

        return key;
    }


    public UUID getUuidFromAnalyticsKey(AnalyticsKey key) {
        if (key == null) return null;
        return reverseLookupMap.getIfPresent(key);
    }

    private String getOrCreateSalt() {
        String existingSalt = getStoredSalt();

        if (existingSalt != null && !existingSalt.isEmpty()) {
            return existingSalt;
        }

        SecureRandom random = new SecureRandom();
        byte[] saltBytes = new byte[16];
        random.nextBytes(saltBytes);

        StringBuilder sb = new StringBuilder();
        for (byte b : saltBytes) {
            sb.append(String.format("%02x", b));
        }
        String newSalt = sb.toString();

        saveSaltToConfig(newSalt);
        logInfo("Ein neuer, plattformübergreifender Analytics-Salt wurde generiert.");

        return newSalt;
    }

    public byte[] getAnalyticsByteId(UUID playerUuid) {
        if (playerUuid == null) return null;

        if (this.serverSalt == null) {
            this.serverSalt = getOrCreateSalt();
        }

        String rawInput = playerUuid.toString() + this.serverSalt;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(rawInput.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            logError("SHA-256 Algorithmus fehlt im System!", e);
            return null;
        }
    }
}