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
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.pytenix.TranslatorPlugin;
import org.pytenix.module.hologram.HologramModule;
import org.pytenix.service.TaskScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Singleton
public class EntityPacketListener implements PacketListener, Listener {

    private final Provider<HologramModule> hologramModuleProvider;
    private final TaskScheduler taskScheduler;

    @Inject
    public EntityPacketListener(Provider<HologramModule> hologramModuleProvider, TaskScheduler taskScheduler) {
        this.hologramModuleProvider = hologramModuleProvider;
        this.taskScheduler = taskScheduler;
    }

    private Cache<Component, Component> getHologramCache(String locale) {
        try {
            return hologramModuleProvider.get().getPlayerTranslationCache().get(locale, () -> CacheBuilder.newBuilder()
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
        HologramModule hologramModule = hologramModuleProvider.get();
        if (!hologramModule.isModuleActive()) return;

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);
            processHologram(event, event.getUser(), wrapper.getEntityId(), wrapper.getEntityMetadata(), hologramModule);
        } else if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            // WrapperPlayServerSpawnEntity wrapper = new WrapperPlayServerSpawnEntity(event);
            // TODO:
        } else if (event.getPacketType() == PacketType.Play.Server.SPAWN_LIVING_ENTITY) {
            WrapperPlayServerSpawnLivingEntity wrapper = new WrapperPlayServerSpawnLivingEntity(event);
            processHologram(event, event.getUser(), wrapper.getEntityId(), wrapper.getEntityMetadata(), hologramModule);
        }
    }

    private void processHologram(PacketSendEvent event, User user, int entityId, List<EntityData<?>> dataList, HologramModule hologramModule) {
        if (dataList == null || dataList.isEmpty()) return;

        final UUID playerUuid = user.getUUID();
        final List<EntityData<?>> snapshotDataList = new ArrayList<>(dataList);

        hologramModule.requiresTranslation(event.getUser().getUUID())
                .thenAcceptAsync(requiresTranslation -> {
                    if (!requiresTranslation) return;

                    Player player = Bukkit.getPlayer(playerUuid);
                    if (player == null) return;

                    Cache<Component, Component> personalCache = getHologramCache(hologramModule.getPlayerLocaleProcessor().retrieveLocale(player.getUniqueId()));
                    if (personalCache == null) return;

                    List<EntityData<?>> instantUpdatesToSend = new ArrayList<>();
                    for (EntityData data : snapshotDataList) {
                        Object value = data.getValue();
                        Component originalComponent = null;
                        boolean wasOptional = false;

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
                                Object newValue = wasOptional ? Optional.of(cachedTranslation) : cachedTranslation;
                                instantUpdatesToSend.add(new EntityData(data.getIndex(), data.getType(), newValue));
                            } else {
                                String legacyText = TranslatorPlugin.getLegacyComponentSerializer().serialize(originalComponent);

                                if (!legacyText.trim().isEmpty()) {
                                    final Component keyComponent = originalComponent;
                                    final boolean isOptionalFinal = wasOptional;

                                    translateHologramLine(player, legacyText, hologramModule)
                                            .thenAccept(translatedComponent -> {
                                                if (translatedComponent == null) return;

                                                personalCache.put(keyComponent, translatedComponent);

                                                Object updatedValue = isOptionalFinal ? Optional.of(translatedComponent) : translatedComponent;
                                                EntityData<?> newDataUpdate = new EntityData<>(data.getIndex(), data.getType(), updatedValue);

                                                sendUpdatePacket(user, entityId, List.of(newDataUpdate), hologramModule);
                                            });
                                }
                            }
                        }
                    }

                    if (!instantUpdatesToSend.isEmpty()) {
                        sendUpdatePacket(user, entityId, instantUpdatesToSend, hologramModule);
                    }
                });
    }

    private CompletableFuture<Component> translateHologramLine(Player player, String text, HologramModule hologramModule) {
        if (player == null) return CompletableFuture.completedFuture(null);
        String lang = hologramModule.getPlayerLocaleProcessor().retrieveLocale(player.getUniqueId());

        return hologramModule.translate(text, lang)
                .thenApply(translatedString -> TranslatorPlugin.getLegacyComponentSerializer().deserialize(translatedString));
    }

    private void sendUpdatePacket(User user, int entityId, List<EntityData<?>> newData, HologramModule hologramModule) {
        if (newData.isEmpty()) return;

        Player player = Bukkit.getPlayer(user.getUUID());
        if (player == null) return;

        WrapperPlayServerEntityMetadata updatePacket = new WrapperPlayServerEntityMetadata(entityId, newData);

        taskScheduler.runForEntity(player, () -> {
            PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, updatePacket);
        });
    }
}