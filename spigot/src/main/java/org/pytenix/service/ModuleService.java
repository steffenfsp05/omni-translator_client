package org.pytenix.service;

import org.pytenix.TranslatorPlugin;
import org.pytenix.module.chat.PluginChatModule;
import org.pytenix.module.gui.InventoryModule;
import org.pytenix.module.hologram.HologramModule;
import org.pytenix.module.player.LiveChatModule;
import org.pytenix.module.signs.SignsModule;

import java.util.ArrayList;
import java.util.List;

public class ModuleService {


    final TranslatorPlugin translatorPlugin;

    final List<TranslatorModule> modules;

    public ModuleService(TranslatorPlugin translatorPlugin) {
        this.translatorPlugin = translatorPlugin;
        this.modules = new ArrayList<>();

        registerModule(new InventoryModule(translatorPlugin));
        registerModule(new PluginChatModule(translatorPlugin));
        registerModule(new LiveChatModule(translatorPlugin));
        registerModule(new HologramModule(translatorPlugin));
        registerModule(new SignsModule(translatorPlugin));
    }


    public void registerModule(TranslatorModule translatorModule) {
        modules.add(translatorModule);
    }


}
