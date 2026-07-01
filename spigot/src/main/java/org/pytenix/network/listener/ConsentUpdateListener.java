package org.pytenix.network.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.pytenix.TranslatorPlugin;
import org.pytenix.event.annotation.OmniSubscribe;
import org.pytenix.event.register.ConsentUpdateEvent;
import org.pytenix.network.SpigotTransport;
import org.pytenix.packets.impl.ConsentRefreshRequestMapper;
import org.pytenix.packets.impl.ProfileMapper;
import org.pytenix.proto.generated.NetworkPackets;

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

        final ConsentRefreshRequestMapper.Data profileData = event.profilePacket();
        final Location originalLocation = player.getLocation().clone();


        translatorPlugin.getTaskScheduler().runForEntity(player, () -> {

            Location refreshLocation = originalLocation.clone().add(0, 0, 200);
            final double hearts = player.getHealth();
            player.teleport(refreshLocation);

            translatorPlugin.getTaskScheduler().runSyncLater(() -> {

                player.teleport(originalLocation);
                player.setHealth(hearts);
                ComponentLike component = Component.text("§cUnknown value");

                if (profileData.consentType().equals(NetworkPackets.ProfilePacket.ConsentType.EXPLICIT))
                    component = Component.text("§aYou turned translations on");


                if (profileData.consentType().equals(NetworkPackets.ProfilePacket.ConsentType.DECLINED))
                    component = Component.text("§cYou turned translations off");



                player.sendMessage(component);
               //player.sendMessage("§a[Omni] §7Consent updated!");

            }, 5);
        });
    }

}
