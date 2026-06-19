package org.pytenix.module;

import org.pytenix.TranslatorPlugin;
import org.pytenix.module.modules.chat.PluginChatModule;
import org.pytenix.module.modules.gui.InventoryModule;
import org.pytenix.module.modules.hologram.HologramModule;
import org.pytenix.module.modules.player.LiveChatModule;
import org.pytenix.module.modules.signs.SignsModule;

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
