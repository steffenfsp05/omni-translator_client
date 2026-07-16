package org.pytenix.network.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.ConfigUpdateEvent;
import org.pytenix.service.TaskScheduler;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

@Singleton
public class ConfigUpdateListener {

    private final TaskScheduler taskScheduler;
    private final ObjectMapper mapper;
    private final File configFile;
    private final Logger logger;

    @Inject
    public ConfigUpdateListener(TaskScheduler taskScheduler, ObjectMapper mapper, @Named("configFile") File configFile, Logger logger) {
        this.taskScheduler = taskScheduler;
        this.mapper = mapper;
        this.configFile = configFile;
        this.logger = logger;
    }

    @OmniSubscribe(priority = 90)
    public void onConfigUpdate(ConfigUpdateEvent event) {
        taskScheduler.runAsync(() -> {
            try {
                if (!configFile.getParentFile().exists()) configFile.getParentFile().mkdirs();
                mapper.writeValue(configFile, event.translationConfiguration());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        logger.info("Config-Update empfangen und angewendet.");
    }
}