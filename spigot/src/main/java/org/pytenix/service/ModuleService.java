package org.pytenix.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import org.omni.translation.module.AbstractTranslatorModule;
import org.pytenix.module.gui.InventoryModule;
import org.pytenix.module.hologram.HologramModule;
import org.pytenix.module.player.LiveChatModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@Singleton
public class ModuleService {

    private final List<AbstractTranslatorModule> modules = new ArrayList<>();

    @Inject
    public ModuleService(Set<AbstractTranslatorModule> modules) {
        for (AbstractTranslatorModule module : modules) {
            registerModule(module);
        }
    }

    public void registerModule(AbstractTranslatorModule module) {
        System.out.println("ADDED MODULE: " + module.getTranslationModule());
        modules.add(module);
        module.init();
    }

}