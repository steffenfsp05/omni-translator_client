package org.pytenix;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.pytenix.cache.CacheProvider;
import org.pytenix.cache.impl.CaffeineCacheProvider;
import org.pytenix.config.ConfigService;
import org.pytenix.config.ConfigurationFile;
import org.pytenix.entity.ServerConfiguration;
import org.pytenix.event.EventService;
import org.pytenix.event.impl.DefaultEventService;
import org.pytenix.listener.PlayerJoinQuitListener;
import org.pytenix.listener.PlayerLocaleChangeListener;
import org.pytenix.placeholder.GradientService;
import org.pytenix.placeholder.PlaceholderService;
import org.pytenix.placeholder.impl.DefaultGradientService;
import org.pytenix.placeholder.impl.DefaultPlaceholderService;
import org.pytenix.service.ModuleService;
import org.pytenix.network.SpigotTransport;
import org.pytenix.network.VelocitySecretReader;
import org.pytenix.translation.TranslationProcessor;
import org.pytenix.translation.TranslatorService;
import org.pytenix.translation.impl.DefaultTranslationService;
import org.pytenix.service.TaskScheduler;
import org.pytenix.util.TextComponentUtil;

import java.io.File;
import java.io.IOException;


@Getter
public class TranslatorPlugin extends JavaPlugin {

    public String pluginMessagingChannel;
    @Getter
    TextComponentUtil textComponentUtil;
    CacheProvider<String,String> caffeineCache;

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


    private TranslationProcessor translationProcessor;
    private PlaceholderService placeholderService;
    private GradientService gradientService;
    private EventService eventService;

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

        this.translationProcessor = (id, text, targetLang, module) -> getSpigotTransport().translate(id, text, targetLang, module);
        this.placeholderService = new DefaultPlaceholderService();
        this.gradientService = new DefaultGradientService();
        this.eventService = new DefaultEventService();

        this.translatorService = new DefaultTranslationService(translationProcessor, placeholderService,gradientService,eventService);


        final VelocitySecretReader secretReader = new VelocitySecretReader();
        final String secret = secretReader.loadVelocitySecret();

        if (secret == null || secret.equals("")) {
            System.out.println("Cant read Velocity secret from Paper/Spigot config!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        System.out.println("READING SECRET: " + secret);


        this.spigotTransport = new SpigotTransport(this, secret, pluginMessagingChannel);

        loadConfigFromDisk();

        this.textComponentUtil = new TextComponentUtil(translatorService);


        Bukkit.getPluginManager().registerEvents(new PlayerJoinQuitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerLocaleChangeListener(this), this);

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