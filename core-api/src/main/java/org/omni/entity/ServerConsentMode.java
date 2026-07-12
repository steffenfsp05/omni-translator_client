package org.omni.entity;

public enum ServerConsentMode {
    STRICT,
    EXTERNAL,
    AUTO_OPT;

    public static ServerConsentMode getConsentMode(String name) {
        return valueOf(name.toUpperCase());
    }

}
