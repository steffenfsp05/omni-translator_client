package org.pytenix.data;

import com.destroystokyo.paper.ClientOption;
import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class PlayerServerConnectListener implements Listener {

    private static final TextColor ERROR = TextColor.color(0xE05C5C);

    private final Map<UUID, CompletableFuture<Boolean>> awaitingResponse = new ConcurrentHashMap<>();
    private final Map<UUID, String> activeLanguages = new ConcurrentHashMap<>();


    private final GDPRService gdprService;

    @Inject
    public PlayerServerConnectListener(GDPRService gdprService)
    {
        this.gdprService = gdprService;
    }

    @EventHandler
    public void onPlayerConfigure(AsyncPlayerConnectionConfigureEvent event) {
        PlayerConfigurationConnection connection = event.getConnection();
        UUID uniqueId = connection.getProfile().getId();
        if (uniqueId == null) return;

        String rawLocale = connection.getClientOption(ClientOption.LOCALE);
        String locale = (rawLocale != null) ? rawLocale.toLowerCase() : "en_us";
        String playerLanguage = activeLanguages.getOrDefault(uniqueId, locale.split("_")[0]);

        boolean needScreen = gdprService.needGDPRScreen(uniqueId).join();

        if (!needScreen) return;

        Dialog dialog = RegistryAccess.registryAccess().getRegistry(RegistryKey.DIALOG).get(GDPRBootstrap.getDialogKey(playerLanguage));
        if (dialog == null) return;

        connection.getAudience().showDialog(dialog);

        CompletableFuture<Boolean> response = new CompletableFuture<>();
        response.completeOnTimeout(false, 5, TimeUnit.MINUTES);
        awaitingResponse.put(uniqueId, response);

        if (!response.join()) {
            connection.disconnect(Component.text("Timeout\nYou must respond to the privacy dialog to continue.", ERROR));
        }

        awaitingResponse.remove(uniqueId);
        activeLanguages.remove(uniqueId);
    }

    @EventHandler
    public void onHandleDialog(PlayerCustomClickEvent event) {
        if (!(event.getCommonConnection() instanceof PlayerConfigurationConnection configurationConnection)) return;

        UUID uniqueId = configurationConnection.getProfile().getId();
        if (uniqueId == null) return;

        Key key = event.getIdentifier();

        boolean acceptTranslation = false;
        boolean acceptTracking = false;
        boolean isGdprAction = false;

        if (key.equals(Key.key("omni:gdpr/skip"))) {
            isGdprAction = true;

        } else if (key.equals(Key.key("omni:gdpr/submit"))) {
            isGdprAction = true;
            DialogResponseView view = event.getDialogResponseView();
            acceptTranslation = view.getBoolean("accept_translation");
            acceptTracking = view.getBoolean("accept_tracking");

        } else if (key.equals(Key.key("omni:gdpr/submit_all"))) {
            isGdprAction = true;
            acceptTranslation = true;
            acceptTracking = true;
        }

        if (!isGdprAction) return;

        gdprService.setConsents(uniqueId, acceptTranslation, acceptTracking)
                .whenComplete((unused, throwable) -> {
                    if (throwable != null) {
                        throwable.printStackTrace();
                    }
                    setConnectionJoinResult(uniqueId, true);
                });
    }

    @EventHandler
    public void onConnectionClose(PlayerConnectionCloseEvent event) {
        awaitingResponse.remove(event.getPlayerUniqueId());
        activeLanguages.remove(event.getPlayerUniqueId());
    }

    private void setConnectionJoinResult(UUID uniqueId, boolean value) {
        CompletableFuture<Boolean> future = awaitingResponse.get(uniqueId);
        if (future != null) {
            future.completeAsync(() -> value, CompletableFuture.delayedExecutor(250, TimeUnit.MILLISECONDS));
        }
    }
}