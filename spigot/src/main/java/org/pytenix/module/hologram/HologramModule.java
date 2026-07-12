package org.pytenix.module.hologram;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.omni.profile.ProfileService;
import org.omni.translation.TranslatorService;
import org.omni.translation.locale.PlayerLocaleProcessor;
import org.omni.translation.module.AbstractTranslatorModule;
import org.pytenix.TranslatorPlugin;
import org.pytenix.module.hologram.listener.EntityPacketListener;

import java.util.concurrent.TimeUnit;

@Getter
@Singleton
public class HologramModule extends AbstractTranslatorModule {

    private final Cache<String, Cache<Component, Component>> playerTranslationCache;
    private final TranslatorPlugin translatorPlugin;

    final EntityPacketListener entityPacketListener;

    @Inject
    public HologramModule(
            ProfileService profileService,
            TranslatorService translatorService,
            PlayerLocaleProcessor playerLocaleProcessor,
            TranslatorPlugin translatorPlugin,
            EntityPacketListener entityPacketListener
    ) {
        super(profileService, translatorService, playerLocaleProcessor, "hologram");

        this.translatorPlugin = translatorPlugin;
        this.playerTranslationCache = CacheBuilder.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .build();

        this.entityPacketListener = entityPacketListener;
    }

    @Override
    public void init() {
        PacketEvents.getAPI().getEventManager().registerListener(
                entityPacketListener,
                PacketListenerPriority.HIGHEST);
    }
}