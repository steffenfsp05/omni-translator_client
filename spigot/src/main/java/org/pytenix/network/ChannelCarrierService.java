package org.pytenix.network;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.player.PlayerUnregisterChannelEvent;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelCarrierService implements Listener {

    private final String channel;
    private final Set<UUID> availableCarriers = ConcurrentHashMap.newKeySet();

    public ChannelCarrierService(String channel) {
        this.channel = channel;
    }

    public Optional<Player> getRandomCarrier() {
        return availableCarriers.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .findAny();
    }

    @EventHandler
    public void onChannelRegister(PlayerRegisterChannelEvent event) {
        if (event.getChannel().equalsIgnoreCase(channel)) {
            availableCarriers.add(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onChannelUnregister(PlayerUnregisterChannelEvent event) {
        if (event.getChannel().equalsIgnoreCase(channel)) {
            availableCarriers.remove(event.getPlayer().getUniqueId());
        }
    }

    public boolean isEmpty() { return availableCarriers.isEmpty(); }

}
