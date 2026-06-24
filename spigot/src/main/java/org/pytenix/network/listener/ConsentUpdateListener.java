package org.pytenix.network.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.pytenix.TranslatorPlugin;
import org.pytenix.event.annotation.OmniSubscribe;
import org.pytenix.event.register.ConsentUpdateEvent;
import org.pytenix.network.SpigotTransport;

public class ConsentUpdateListener {

    final TranslatorPlugin translatorPlugin;
    final SpigotTransport spigotTransport;

    public ConsentUpdateListener(TranslatorPlugin translatorPlugin) {
        this.translatorPlugin = translatorPlugin;
        this.spigotTransport = translatorPlugin.getSpigotTransport();
    }

    @OmniSubscribe(priority = 90)
    public void onConsentUpdate(ConsentUpdateEvent event) {
        Player player = Bukkit.getPlayer(event.profilePacket().playerId());

        final Location originalLocation = player.getLocation().clone();


        translatorPlugin.getTaskScheduler().runForEntity(player, () -> {

            Location refreshLocation = originalLocation.clone().add(0, 0, 200);
            double hearts = player.getHealth();
            player.teleport(refreshLocation);

            translatorPlugin.getTaskScheduler().runSyncLater(() -> {

                player.teleport(originalLocation);
                player.setHealth(hearts);
                player.sendMessage("§a[Omni] §7Consent updated!");

            }, 3);
        });
    }

}
