package org.pytenix.network.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.ConsentUpdateEvent;
import org.omni.packets.data.ConsentRefreshRequestData;
import org.omni.proto.generated.Protobuf;
import org.pytenix.service.TaskScheduler;

@Singleton
public class ConsentUpdateListener {

    private final TaskScheduler taskScheduler;

    @Inject
    public ConsentUpdateListener(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    @OmniSubscribe(priority = 90)
    public void onConsentUpdate(ConsentUpdateEvent event) {
        Player player = Bukkit.getPlayer(event.refreshRequestData().playerId());
        if (player == null) return;

        final ConsentRefreshRequestData profileData = event.refreshRequestData();
        final Location originalLocation = player.getLocation().clone();

        taskScheduler.runForEntity(player, () -> {
            Location refreshLocation = originalLocation.clone().add(0, 0, 200);
            final double hearts = player.getHealth();
            player.teleport(refreshLocation);

            taskScheduler.runSyncLater(() -> {
                player.teleport(originalLocation);
                player.setHealth(hearts);
                ComponentLike component = Component.text("§cUnknown value");

                if (profileData.consentType().equals(Protobuf.ConsentType.EXPLICIT))
                    component = Component.text("§aYou turned translations on");

                if (profileData.consentType().equals(Protobuf.ConsentType.DECLINED))
                    component = Component.text("§cYou turned translations off");

                player.sendMessage(component);
            }, 5);
        });
    }
}