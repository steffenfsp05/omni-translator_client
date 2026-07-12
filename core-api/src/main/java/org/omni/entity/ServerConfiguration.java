package org.omni.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@AllArgsConstructor
public class ServerConfiguration {

    String licenseKey;
    HashMap<TranslationModule, Boolean> modules;
    String defaultLanguage;
    ServerConsentMode consentMode = ServerConsentMode.STRICT;
    Set<String> blacklistedWords;


    public ServerConfiguration() {

    }


    public static ServerConfiguration createDefault(String licenseKey) {
        ServerConfiguration serverConfiguration = new ServerConfiguration();


        HashMap<TranslationModule, Boolean> hash = new HashMap<>();
        for (TranslationModule value : TranslationModule.values()) {
            hash.put(value, true);
        }

        serverConfiguration.setConsentMode(ServerConsentMode.STRICT);
        serverConfiguration.setModules(hash);
        serverConfiguration.setDefaultLanguage("NOT_SET");
        serverConfiguration.setLicenseKey(licenseKey);
        serverConfiguration.setBlacklistedWords(new HashSet<>());

        return serverConfiguration;
    }


}
