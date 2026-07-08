package org.pytenix.profile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

public abstract class AbstractAnalyticsSecret {
    private final String serverSalt;

    public AbstractAnalyticsSecret() {
        this.serverSalt = getOrCreateSalt();
    }

    protected abstract String getStoredSalt();
    protected abstract void saveSaltToConfig(String salt);
    protected abstract void logInfo(String message);
    protected abstract void logError(String message, Throwable throwable);

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

    public String getAnalyticsId(UUID playerUuid) {
        if (playerUuid == null) return null;

        String rawInput = playerUuid.toString() + this.serverSalt;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawInput.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            logError("SHA-256 Algorithmus fehlt im System!", e);
            return null;
        }
    }
}
