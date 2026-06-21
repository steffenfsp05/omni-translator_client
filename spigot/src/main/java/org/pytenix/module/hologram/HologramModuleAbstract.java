package org.pytenix.module.hologram;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.pytenix.TranslatorPlugin;
import org.pytenix.module.hologram.listener.EntityPacketListener;
import org.pytenix.module.AbstractTranslatorModule;

import java.util.concurrent.TimeUnit;

@Getter
public class HologramModuleAbstract extends AbstractTranslatorModule {

    public final Cache<String, Cache<Component, Component>> playerTranslationCache = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();


    public HologramModuleAbstract(TranslatorPlugin translatorPlugin) {
        super(translatorPlugin, "hologram");


        PacketEvents.getAPI().getEventManager().registerListener(new EntityPacketListener(this),
                PacketListenerPriority.HIGHEST);
    }


}
