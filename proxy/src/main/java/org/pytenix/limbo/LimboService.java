package org.pytenix.limbo;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.pytenix.TranslatorPlugin;
import org.pytenix.limbo.command.TranslateCommand;
import org.pytenix.limbo.listener.ServerPreConnectListener;

import java.io.File;
import java.net.InetSocketAddress;

public class LimboService {

    private Process nanoLimboProcess;

    public LimboService(TranslatorPlugin plugin, ProxyServer proxyServer , int port, String secret)
    {

        proxyServer.getEventManager().register(plugin, new ServerPreConnectListener(plugin));

        CommandMeta meta = proxyServer.getCommandManager().metaBuilder("translate")
                .plugin(plugin)
                .build();

        proxyServer.getCommandManager().register(meta, new TranslateCommand(plugin));

        final String limboDir = "plugins/nanolimbo";
        LimboDownloadService.checkAndDownload(limboDir);

        File configFile = new File(limboDir, "settings.yml");

        try {

            LimboConfigGenerator.generateMinimalConfig(configFile,"127.0.0.1", port, secret);

            ProcessBuilder pb = new ProcessBuilder("java", "-jar", LimboDownloadService.FILE_NAME);
            pb.directory(new File(limboDir));



            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);

            this.nanoLimboProcess = pb.start();
            System.out.println("NanoLimbo wurde im Hintergrund gestartet!");

        } catch (Exception e) {
            System.out.println("Konnte NanoLimbo nicht starten!" +  e.getMessage());
            return;
        }

        ServerInfo limboInfo = new ServerInfo(
                "dynamic-limbo",
                new InetSocketAddress("127.0.0.1", port)
        );
        proxyServer.registerServer(limboInfo);
        System.out.println("Limbo-Server dynamisch in Velocity angebunden!");
    }

    public void shutdown()
    {
        if (nanoLimboProcess != null && nanoLimboProcess.isAlive()) {
            nanoLimboProcess.destroy();
            System.out.println("NanoLimbo-Hintergrundprozess beendet.");
        }
    }
}
