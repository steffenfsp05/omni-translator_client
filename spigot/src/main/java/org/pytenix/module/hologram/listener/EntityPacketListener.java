package org.pytenix.module.hologram.listener;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.pytenix.TranslatorPlugin;
import org.pytenix.module.hologram.HologramModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class EntityPacketListener implements PacketListener, Listener {


    final HologramModule hologramModule;


    public EntityPacketListener(HologramModule hologramModule) {
        this.hologramModule = hologramModule;


    }


    private Cache<Component, Component> getHologramCache(String locale) {
        try {
            return hologramModule.getPlayerTranslationCache().get(locale, () -> CacheBuilder.newBuilder()
                    .expireAfterWrite(2, TimeUnit.MINUTES)
                    .maximumSize(1000)
                    .build());
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public void onPacketSend(PacketSendEvent event) {

        if (!hologramModule.isActive())
            return;

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);
            processHologram(event, event.getUser(), wrapper.getEntityId(), wrapper.getEntityMetadata());
        } else if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            //   WrapperPlayServerSpawnEntity wrapper = new WrapperPlayServerSpawnEntity(event);
            //TODO:
        } else if (event.getPacketType() == PacketType.Play.Server.SPAWN_LIVING_ENTITY) {
            WrapperPlayServerSpawnLivingEntity wrapper = new WrapperPlayServerSpawnLivingEntity(event);
            processHologram(event, event.getUser(), wrapper.getEntityId(), wrapper.getEntityMetadata());
        }
    }

    private void processHologram(PacketSendEvent event, com.github.retrooper.packetevents.protocol.player.User user, int entityId, List<EntityData<?>> dataList) {
        if (dataList == null || dataList.isEmpty()) return;

        final UUID playerUuid = user.getUUID();
        final List<EntityData<?>> snapshotDataList = new ArrayList<>(dataList);

        hologramModule.requiresTranslation(event.getUser().getUUID())
                .thenAcceptAsync(aBoolean ->
                {
                    if(!aBoolean)
                        return;

                    Player player = Bukkit.getPlayer(playerUuid);
                    if (player == null) return;

                    Cache<Component, Component> personalCache = getHologramCache(hologramModule.getPlayerLocaleProcessor().retrieveLocale(player.getUniqueId()));
                    if (personalCache == null) return;

                    List<EntityData<?>> instantUpdatesToSend = new ArrayList<>();
                    for (EntityData data : snapshotDataList) {
                        Object value = data.getValue();
                        Component originalComponent = null;
                        boolean wasOptional = false;

                        // Extrahiere die Component
                        if (value instanceof Optional<?> opt) {
                            if (opt.isPresent() && opt.get() instanceof Component comp) {
                                originalComponent = comp;
                                wasOptional = true;
                            }
                        } else if (value instanceof Component comp) {
                            originalComponent = comp;
                            wasOptional = false;
                        }

                        if (originalComponent != null) {
                            Component cachedTranslation = personalCache.getIfPresent(originalComponent);

                            if (cachedTranslation != null) {
                                // 🎯 CACHE HIT: Paket sofort austauschen!
                                Object newValue = wasOptional ? Optional.of(cachedTranslation) : cachedTranslation;
                                instantUpdatesToSend.add(new EntityData(data.getIndex(), data.getType(), newValue));
                            } else {
                                String legacyText = TranslatorPlugin.getLegacyComponentSerializer().serialize(originalComponent);

                                if (!legacyText.trim().isEmpty()) {
                                    final Component keyComponent = originalComponent;
                                    final boolean isOptionalFinal = wasOptional;

                                    translateHologramLine(player, legacyText)
                                            .thenAccept(translatedComponent -> {
                                                if (translatedComponent == null) return;

                                                personalCache.put(keyComponent, translatedComponent);

                                                Object updatedValue = isOptionalFinal ? Optional.of(translatedComponent) : translatedComponent;
                                                EntityData<?> newDataUpdate = new EntityData<>(data.getIndex(), data.getType(), updatedValue);

                                                // Schicke das frische Update-Paket einzeln raus
                                                sendUpdatePacket(user, entityId, List.of(newDataUpdate));
                                            });
                                }
                            }
                        }
                    }

                    if (!instantUpdatesToSend.isEmpty()) {
                        sendUpdatePacket(user, entityId, instantUpdatesToSend);
                    }
                });


    }

    private CompletableFuture<Component> translateHologramLine(Player player, String text) {
        if (player == null) return CompletableFuture.completedFuture(null);
        String lang = hologramModule.getPlayerLocaleProcessor().retrieveLocale(player.getUniqueId());

        return hologramModule.translate(text, lang)
                .thenApply(translatedString -> TranslatorPlugin.getLegacyComponentSerializer().deserialize(translatedString));
    }

    private void sendUpdatePacket(User user, int entityId, List<EntityData<?>> newData) {
        if (newData.isEmpty()) return;


        if (Bukkit.getPlayer(user.getUUID()) == null) return;

        WrapperPlayServerEntityMetadata updatePacket = new WrapperPlayServerEntityMetadata(

                entityId,
                newData
        );


        hologramModule.getTranslatorPlugin().getTaskScheduler().runForEntity(Bukkit.getPlayer(user.getUUID()), () ->
        {
            PacketEvents.getAPI().getPlayerManager().sendPacketSilently(Bukkit.getPlayer(user.getUUID()), updatePacket);
        });


    }


}
