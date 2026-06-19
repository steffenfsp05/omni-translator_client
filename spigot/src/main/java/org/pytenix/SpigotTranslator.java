package org.pytenix;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.pytenix.cache.impl.CaffeineCacheProvider;
import org.pytenix.config.ConfigService;
import org.pytenix.config.ConfigurationFile;
import org.pytenix.entity.ServerConfiguration;
import org.pytenix.listener.JoinQuitListener;
import org.pytenix.listener.LocaleChangeEvent;
import org.pytenix.module.ModuleService;
import org.pytenix.pluginmessage.SpigotTransport;
import org.pytenix.pluginmessage.VelocitySecretReader;
import org.pytenix.translation.TranslatorService;
import org.pytenix.translation.impl.DefaultTranslationService;
import org.pytenix.util.TaskScheduler;
import org.pytenix.util.TextComponentUtil;

import java.io.File;
import java.io.IOException;


@Getter
public class SpigotTranslator extends JavaPlugin {

    public String pluginMessagingChannel;
    @Getter
    TextComponentUtil textComponentUtil;
    CaffeineCacheProvider caffeineCache;
    LegacyComponentSerializer legacyComponentSerializer = LegacyComponentSerializer.builder()
            .character('§')
            .extractUrls()
            .hexColors()
            .flattener(ComponentFlattener.basic())
            .build();
    ConfigService configService;
    ConfigurationFile configurationFile;
    private String serverName;
    private TranslatorService translatorService;
    private ModuleService moduleService;
    private TaskScheduler taskScheduler;
    private File configFile;
    private SpigotTransport spigotTransport;
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public void onEnable() {

        this.pluginMessagingChannel = "translator:main";

        this.caffeineCache = new CaffeineCacheProvider();

        this.configService = new ConfigService();


        if (!configService.exists("config.json")) {
            configService.saveConfig("config.json", new ConfigurationFile("DEIN-LIZENZ-SCHLÜSSEL"));
            System.out.println("[AITranslator] Please check in config.json for the license key!");
        }
        this.configurationFile = configService.loadConfig("config.json", ConfigurationFile.class);


        this.serverName = this.getServer().getName();
        this.taskScheduler = new TaskScheduler(this);
        this.configFile = new File(getDataFolder(), "proxy_sync_config.json");


        this.translatorService = new DefaultTranslationService((id, text, targetLang, module) ->
                getSpigotTransport().translate(id, text, targetLang, module));


        final VelocitySecretReader secretReader = new VelocitySecretReader();
        final String secret = secretReader.loadVelocitySecret();

        if (secret == null || secret.equals("")) {
            System.out.println("Cant read Velocity secret from Paper/Spigot config!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }


        this.spigotTransport = new SpigotTransport(this, secret, pluginMessagingChannel);
        Bukkit.getPluginManager().registerEvents(spigotTransport, this);

        loadConfigFromDisk();

        this.textComponentUtil = new TextComponentUtil(translatorService);


        Bukkit.getPluginManager().registerEvents(new JoinQuitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LocaleChangeEvent(this), this);

        moduleService = new ModuleService(this);

        getServer().getCommandMap().register("translator", new org.bukkit.command.Command("testmsg") {

            private final TestMessageCommand executor = new TestMessageCommand();

            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                return executor.onCommand(sender, this, commandLabel, args);
            }
        });

        getLogger().info("AITranslator Test-Modul geladen!");

    }


    private void loadConfigFromDisk() {
        if (!configFile.exists()) {

            getLogger().info("Keine lokale Config gefunden. Nutze Default bis Proxy sendet.");
            resetConfiguration();
            return;
        }

        try {
            translatorService.setTranslationConfiguration(mapper.readValue(configFile, ServerConfiguration.class));
        } catch (IOException e) {
            getLogger().severe("Konnte lokale Config nicht laden: " + e.getMessage());
            resetConfiguration();
        }
    }

    private void resetConfiguration() {
        translatorService.setTranslationConfiguration(ServerConfiguration.createDefault("DEIN-LIZENZ-SCHLÜSSEL"));


    }


    @Override
    public void onDisable() {
        spigotTransport.getTransportService().close();
    }
}