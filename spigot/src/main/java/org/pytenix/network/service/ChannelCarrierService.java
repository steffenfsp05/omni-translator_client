package org.pytenix.network.service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.player.PlayerUnregisterChannelEvent;
import org.transport.TransportService;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class ChannelCarrierService implements Listener {

    private final String channel;
    private final Provider<TransportService<String>> transportServiceProvider;
    private final Set<UUID> availableCarriers = ConcurrentHashMap.newKeySet();

    @Inject
    public ChannelCarrierService(
            @Named("pluginMessagingChannel") String channel,
            Provider<TransportService<String>> transportServiceProvider
    ) {
        this.channel = channel;
        this.transportServiceProvider = transportServiceProvider;
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
            transportServiceProvider.get().ready(channel);
        }
    }

    @EventHandler
    public void onChannelUnregister(PlayerUnregisterChannelEvent event) {
        if (event.getChannel().equalsIgnoreCase(channel)) {
            availableCarriers.remove(event.getPlayer().getUniqueId());
            if (isEmpty()) {
                TransportService<String> transport = transportServiceProvider.get();
                transport.disconnect(channel);
                transport.connect(channel);
            }
        }
    }

    public boolean isEmpty() {
        return availableCarriers.isEmpty();
    }
}