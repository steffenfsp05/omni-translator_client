package org.pytenix.service;

import org.pytenix.TranslatorPlugin;
import org.pytenix.module.AbstractTranslatorModule;
import org.pytenix.module.chat.PluginChatModuleAbstract;
import org.pytenix.module.gui.InventoryModuleAbstract;
import org.pytenix.module.hologram.HologramModuleAbstract;
import org.pytenix.module.player.LiveChatModuleAbstract;
import org.pytenix.module.signs.SignsModuleAbstract;

import java.util.ArrayList;
import java.util.List;

public class ModuleService {


    final TranslatorPlugin translatorPlugin;

    final List<AbstractTranslatorModule> modules;

    public ModuleService(TranslatorPlugin translatorPlugin) {
        this.translatorPlugin = translatorPlugin;
        this.modules = new ArrayList<>();

        registerModule(new InventoryModuleAbstract(translatorPlugin));
        registerModule(new PluginChatModuleAbstract(translatorPlugin));
        registerModule(new LiveChatModuleAbstract(translatorPlugin));
        registerModule(new HologramModuleAbstract(translatorPlugin));
        registerModule(new SignsModuleAbstract(translatorPlugin));
    }


    public void registerModule(AbstractTranslatorModule abstractTranslatorModule) {
        modules.add(abstractTranslatorModule);
    }


}
