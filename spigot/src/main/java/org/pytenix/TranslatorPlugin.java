package org.pytenix;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.*;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import lombok.Getter;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.omni.entity.ServerConfiguration;
import org.omni.event.EventService;
import org.omni.injection.CoreModule;
import org.omni.profile.AbstractAnalyticsSecret;
import org.omni.translation.TranslatorService;
import org.omni.transport.TransportConnector;
import org.pytenix.injection.TranslatorSpigotModule;
import org.pytenix.listener.PlayerJoinQuitListener;
import org.pytenix.listener.PlayerLocaleChangeListener;
import org.pytenix.network.VelocitySecretReader;
import org.pytenix.service.ModuleService;
import org.pytenix.socket.inject.SocketModule;
import org.pytenix.socket.socket.WebSocketService;

import java.io.File;
import java.io.IOException;
import java.util.Set;

@Getter
@Singleton
public class TranslatorPlugin extends JavaPlugin {

    @Getter
    public static final LegacyComponentSerializer legacyComponentSerializer = LegacyComponentSerializer.builder()
            .character('§')
            .extractUrls()
            .hexColors()
            .flattener(ComponentFlattener.basic())
            .build();
    @Inject
    TranslatorService translatorService;


    @Inject
    @Named("configFile")
    private File configFile;

    @Inject
    private ObjectMapper mapper;

    private String serverName;

    @Override
    public void onEnable() {
        this.serverName = this.getServer().getName();

        final VelocitySecretReader secretReader = new VelocitySecretReader();
        final String secret = secretReader.loadVelocitySecret();

        if (secret == null || secret.isEmpty()) {
            System.out.println("Cant read Velocity secret from Paper/Spigot config!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        System.out.println("READING SECRET: " + secret);


        final String remoteAddress = "ws://192.168.178.121:8083/ws/omni";

        Injector injector = Guice.createInjector(
                new CoreModule(),
                new SocketModule(remoteAddress),
                new TranslatorSpigotModule(this, getDataFolder().toPath())
        );

        injector.injectMembers(this);

        loadConfigFromDisk();

        injector.getInstance(ModuleService.class);
        injector.getInstance(AbstractAnalyticsSecret.class);

        injector.getInstance(TransportConnector.class).connect();

        registerListeners(injector);

        registerTestCommand(injector);

        getLogger().info("AITranslator Test-Modul geladen!");
    }

    private void registerListeners(Injector injector) {

        EventService eventService = injector.getInstance(EventService.class);

        Set<Object> omniListeners = injector.getInstance(Key.get(new TypeLiteral<>() {
        }, Names.named("omniListeners")));

        for (Object listener : omniListeners) {
            eventService.register(listener);
        }

        Bukkit.getPluginManager().registerEvents(injector.getInstance(PlayerJoinQuitListener.class), this);
        Bukkit.getPluginManager().registerEvents(injector.getInstance(PlayerLocaleChangeListener.class), this);

    }

    private void registerTestCommand(Injector injector) {
        getServer().getCommandMap().register("translator", new org.bukkit.command.Command("testmsg") {
            private final TestMessageCommand executor = injector.getInstance(TestMessageCommand.class);

            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                return executor.onCommand(sender, this, commandLabel, args);
            }
        });
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

    }
}