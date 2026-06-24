package org.pytenix.service;

import org.pytenix.TranslatorPlugin;
import org.pytenix.translation.AbstractTranslatorModule;
import org.pytenix.module.chat.PluginChatModule;
import org.pytenix.module.gui.InventoryModule;
import org.pytenix.module.hologram.HologramModule;
import org.pytenix.module.player.LiveChatModule;
import org.pytenix.module.signs.SignsModule;
import org.pytenix.translation.TranslatorService;
import org.pytenix.translation.locale.PlayerLocaleProcessor;

import java.util.ArrayList;
import java.util.List;

public class ModuleService {


    final TranslatorService translatorService;
    final PlayerLocaleProcessor playerLocaleProcessor;

    final List<AbstractTranslatorModule> modules;

    public ModuleService(TranslatorPlugin translatorPlugin,TranslatorService translatorService, PlayerLocaleProcessor playerLocaleProcessor) {
        this.translatorService = translatorService;
        this.playerLocaleProcessor = playerLocaleProcessor;
        this.modules = new ArrayList<>();

        registerModule(new InventoryModule(translatorService, playerLocaleProcessor));
        //registerModule(new PluginChatModule(translatorService, playerLocaleProcessor));
        registerModule(new LiveChatModule(translatorPlugin, translatorService, playerLocaleProcessor));
        registerModule(new HologramModule(translatorService, playerLocaleProcessor));
        registerModule(new SignsModule(translatorService, playerLocaleProcessor));
    }


    public void registerModule(AbstractTranslatorModule abstractTranslatorModule) {
        modules.add(abstractTranslatorModule);
    }


}
