package org.pytenix.limbo;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LimboDownloadService {

    static final String FILE_NAME = "limbo.jar";
    private static final String DOWNLOAD_URL = "https://github.com/Nan1t/NanoLimbo/releases/download/v1.12.0/NanoLimbo.jar";

    public static boolean checkAndDownload(String path) {

        File limboDir = new File(path);
        if (!limboDir.exists()) {
            limboDir.mkdirs();
        }

        Path pluginsFolder = Paths.get(path);

        Path limboFile = pluginsFolder.resolve(FILE_NAME);

        if (Files.exists(limboFile)) {
            return true;
        }

        System.out.println("=================================================");
        System.out.println("NanoLimbo wurde nicht gefunden! Starte Download...");
        System.out.println("=================================================");

        try {
            URL url = new URL(DOWNLOAD_URL);
            try (InputStream in = url.openStream()) {
                Files.copy(in, limboFile, StandardCopyOption.REPLACE_EXISTING);
            }

            System.out.println("=================================================");
            System.out.println("Download erfolgreich: " + FILE_NAME);
            System.out.println("=================================================");

            return false;

        } catch (Exception e) {
            System.out.println("Fehler beim Herunterladen der LimboAPI!" + e.getMessage());
            return false;
        }
    }

}
