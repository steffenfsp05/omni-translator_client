package org.pytenix.data;

import com.destroystokyo.paper.ClientOption;
import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
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

    @EventHandler
    public void onPlayerConfigure(AsyncPlayerConnectionConfigureEvent event) {
        PlayerConfigurationConnection connection = event.getConnection();
        UUID uniqueId = connection.getProfile().getId();
        if (uniqueId == null) return;


        String locale = connection.getClientOption(ClientOption.LOCALE).toLowerCase();
        System.out.println("LOCALE: " + locale);
        String playerLanguage = activeLanguages.getOrDefault(uniqueId, locale.split("_")[0]);

        boolean hasAcceptedDSGVO = false;
        if (hasAcceptedDSGVO) return;


        Dialog dialog = RegistryAccess.registryAccess().getRegistry(RegistryKey.DIALOG).get(GDPRBootstrap.getDialogKey(playerLanguage));

        if(dialog == null)
            return;

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

        if (key.equals(Key.key("omni:gdpr/skip"))) {
            setConnectionJoinResult(uniqueId, true);

        } else if (key.equals(Key.key("omni:gdpr/submit"))) {
            DialogResponseView view = event.getDialogResponseView();

            boolean acceptTranslation = view.getBoolean("accept_translation");
            boolean acceptTracking = view.getBoolean("accept_tracking");


            setConnectionJoinResult(uniqueId, true);
        }
        else if (key.equals(Key.key("omni:gdpr/submit_all"))) {
            DialogResponseView view = event.getDialogResponseView();

            boolean acceptTranslation = true;
            boolean acceptTracking = true;


            setConnectionJoinResult(uniqueId, true);
        }
    }

    @EventHandler
    public void onConnectionClose(PlayerConnectionCloseEvent event) {
        awaitingResponse.remove(event.getPlayerUniqueId());
        activeLanguages.remove(event.getPlayerUniqueId());
    }

    private void setConnectionJoinResult(UUID uniqueId, boolean value) {
        CompletableFuture<Boolean> future = awaitingResponse.get(uniqueId);
        if (future != null) {
            future.complete(value);
        }
    }
}