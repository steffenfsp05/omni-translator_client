package org.pytenix.packets.impl;

import org.pytenix.entity.ServerConfiguration;
import org.pytenix.packets.AbstractPacketMapper;
import org.pytenix.proto.generated.NetworkPackets;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


public class DefaultServerConfigMapper extends AbstractPacketMapper<NetworkPackets.ServerConfiguration, ServerConfiguration> {


    public DefaultServerConfigMapper() {
        super(NetworkPackets.ServerConfiguration.class, ServerConfiguration.class);
    }

    @Override
    public NetworkPackets.ServerConfiguration to(ServerConfiguration javaConfig) {
        NetworkPackets.ServerConfiguration.Builder builder = NetworkPackets.ServerConfiguration.newBuilder();

        if (javaConfig.getModules() != null) {
            for (Map.Entry<String, Boolean> entry : javaConfig.getModules().entrySet()) {
                if (entry.getValue() != null && entry.getValue()) {
                    try {
                        String enumName = "MODULE_" + entry.getKey().toUpperCase();
                        NetworkPackets.Module protoModule = NetworkPackets.Module.valueOf(enumName);
                        builder.addActiveModules(protoModule);
                    } catch (IllegalArgumentException e) {
                        System.err.println("Unbekanntes Modul beim Serialisieren ignoriert: " + entry.getKey());
                    }
                }
            }
        }

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
    public ServerConfiguration from(NetworkPackets.ServerConfiguration serverConfiguration) {
        org.pytenix.entity.ServerConfiguration update = new org.pytenix.entity.ServerConfiguration();

        HashMap<String, Boolean> mappedModules = new HashMap<>();

        for (ServerConfiguration.Module module : ServerConfiguration.Module.values()) {
            mappedModules.put(module.name().toLowerCase(), false);
        }

        for (NetworkPackets.Module protoModule : serverConfiguration.getActiveModulesList()) {
            if (protoModule != NetworkPackets.Module.MODULE_UNKNOWN && protoModule != NetworkPackets.Module.UNRECOGNIZED) {
                String javaModuleName = protoModule.name().replace("MODULE_", "").toLowerCase();
                mappedModules.put(javaModuleName, true);
            }
        }

        update.setModules(mappedModules);
        update.setBlacklistedWords(new HashSet<>(serverConfiguration.getWordsList()));
        update.setDefaultLanguage(serverConfiguration.getDefaultLanguage());
        update.setLicenseKey(serverConfiguration.getLicenseKey());

        return update;
    }
}
