package org.omni.config;

import lombok.SneakyThrows;

public interface ConfigService {

    @SneakyThrows
    void saveConfig(String fileName, Object config);

    <T> T loadConfig(String fileName, Class<T> clazz);

    boolean exists(String fileName);
}
