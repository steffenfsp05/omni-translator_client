package org.omni.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Singleton;
import lombok.SneakyThrows;

import java.io.*;

@Singleton
public class DefaultConfigService implements ConfigService {
    private final Gson gson;

    public DefaultConfigService() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }


    @SneakyThrows
    public void saveConfig(String fileName, Object config) {

        File file = new File("plugins/AITranslator/" + fileName);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(config, writer);
            System.out.println("Config erfolgreich gespeichert: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Fehler beim Speichern der Config: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public <T> T loadConfig(String fileName, Class<T> clazz) {
        File file = new File("plugins/AITranslator/" + fileName);
        if (!file.exists()) {
            System.out.println("Config-Datei nicht gefunden, erstelle neue Standard-Config.");
            return createInstance(clazz);
        }

        try (Reader reader = new FileReader(file)) {
            T config = gson.fromJson(reader, clazz);
            if (config == null) {
                return createInstance(clazz);
            }
            System.out.println("Config erfolgreich geladen.");
            return config;
        } catch (IOException e) {
            System.err.println("Fehler beim Laden der Config: " + e.getMessage());
            return createInstance(clazz);
        }
    }


    public boolean exists(String fileName) {
        return new File("plugins/AITranslator/" + fileName).exists();
    }


    private <T> T createInstance(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Konnte keine neue Instanz von " + clazz.getName() + " erstellen. Hat sie einen leeren Konstruktor?", e);
        }
    }
}
