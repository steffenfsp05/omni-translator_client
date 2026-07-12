package org.omni.packets.impl;

import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.omni.entity.ServerConfiguration;
import org.omni.entity.ServerConsentMode;
import org.omni.entity.TranslationModule;
import org.omni.packets.AbstractPacketMapper;
import org.omni.proto.generated.Protobuf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Singleton
public class DefaultServerConfigMapper extends AbstractPacketMapper<Protobuf.ServerConfiguration, ServerConfiguration> {


    public DefaultServerConfigMapper() {
        super(Protobuf.ServerConfiguration.class, ServerConfiguration.class);
    }

    private static @NotNull HashMap<TranslationModule, Boolean> getMappedModules(Protobuf.ServerConfiguration serverConfiguration) {
        HashMap<TranslationModule, Boolean> mappedModules = new HashMap<>();

        for (TranslationModule module : TranslationModule.values()) {
            mappedModules.put(module, false);
        }

        for (Protobuf.Module protoModule : serverConfiguration.getActiveModulesList()) {
            if (protoModule != Protobuf.Module.MODULE_UNKNOWN && protoModule != Protobuf.Module.UNRECOGNIZED) {
                String javaModuleName = protoModule.name().replace("MODULE_", "").toLowerCase();
                mappedModules.put(TranslationModule.getModule(javaModuleName), true);
            }
        }
        return mappedModules;
    }

    @Override
    public Protobuf.ServerConfiguration to(ServerConfiguration javaConfig) {
        Protobuf.ServerConfiguration.Builder builder = Protobuf.ServerConfiguration.newBuilder();

        if (javaConfig.getModules() != null) {
            for (Map.Entry<TranslationModule, Boolean> entry : javaConfig.getModules().entrySet()) {
                if (entry.getValue() != null && entry.getValue()) {
                    try {
                        String enumName = "MODULE_" + entry.getKey().getModuleName().toUpperCase();
                        Protobuf.Module protoModule = Protobuf.Module.valueOf(enumName);
                        builder.addActiveModules(protoModule);
                    } catch (IllegalArgumentException e) {
                        System.err.println("Unbekanntes Modul beim Serialisieren ignoriert: " + entry.getKey());
                    }
                }
            }
        }

        builder.setConsentMode(Protobuf.ConsentMode.valueOf("CONSENT_" + javaConfig.getConsentMode()));

        if (javaConfig.getBlacklistedWords() != null) {
            builder.addAllWords(javaConfig.getBlacklistedWords());
        }

        if (javaConfig.getDefaultLanguage() != null) {
            builder.setDefaultLanguage(javaConfig.getDefaultLanguage());
        }


        if (javaConfig.getLicenseKey() != null) builder.setLicenseKey(javaConfig.getLicenseKey());

        return builder.build();
    }

    @Override
    public ServerConfiguration from(Protobuf.ServerConfiguration serverConfiguration) {
        ServerConfiguration update = new ServerConfiguration();

        HashMap<TranslationModule, Boolean> mappedModules = getMappedModules(serverConfiguration);

        update.setConsentMode(ServerConsentMode.getConsentMode(serverConfiguration.getConsentMode().name().replace("CONSENT_", "")));
        update.setModules(mappedModules);
        update.setBlacklistedWords(new HashSet<>(serverConfiguration.getWordsList()));
        update.setDefaultLanguage(serverConfiguration.getDefaultLanguage());
        update.setLicenseKey(serverConfiguration.getLicenseKey());

        return update;
    }
}
