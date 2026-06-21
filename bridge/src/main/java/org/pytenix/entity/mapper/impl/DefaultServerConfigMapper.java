package org.pytenix.entity.mapper.impl;

import org.pytenix.entity.ServerConfiguration;
import org.pytenix.entity.mapper.ServerConfigMapper;
import org.pytenix.proto.generated.NetworkPackets;

import java.util.HashMap;
import java.util.HashSet;

public class DefaultServerConfigMapper implements ServerConfigMapper {



    @Override
    public NetworkPackets.ServerConfiguration to(ServerConfiguration javaConfig) {
        NetworkPackets.ServerConfiguration.Builder builder = NetworkPackets.ServerConfiguration.newBuilder();
        if (javaConfig.getModules() != null) builder.putAllModules(javaConfig.getModules());
        if (javaConfig.getBlacklistedWords() != null) builder.addAllWords(javaConfig.getBlacklistedWords());
        if (javaConfig.getDefaultLanguage() != null) builder.setDefaultLanguage(javaConfig.getDefaultLanguage());
        return builder.build();
    }

    @Override
    public ServerConfiguration from(NetworkPackets.ServerConfiguration serverConfiguration) {
        org.pytenix.entity.ServerConfiguration update = new org.pytenix.entity.ServerConfiguration();

        update.setModules(new HashMap<>(serverConfiguration.getModulesMap()));
        update.setBlacklistedWords(new HashSet<>(serverConfiguration.getWordsList()));
        update.setDefaultLanguage(serverConfiguration.getDefaultLanguage());

        return update;
    }
}
