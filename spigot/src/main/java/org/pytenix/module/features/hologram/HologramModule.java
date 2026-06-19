package org.pytenix.module.features.hologram;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.pytenix.TranslatorPlugin;
import org.pytenix.module.TranslatorModule;

import java.util.concurrent.TimeUnit;

@Getter
public class HologramModule extends TranslatorModule {

    public final Cache<String, Cache<Component, Component>> playerTranslationCache = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();


    public HologramModule(TranslatorPlugin translatorPlugin) {
        super(translatorPlugin, "hologram");

        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(this), getTranslatorPlugin());

        PacketEvents.getAPI().getEventManager().registerListener(new EntityPacketListener(this),
                PacketListenerPriority.HIGHEST);
    }


}
