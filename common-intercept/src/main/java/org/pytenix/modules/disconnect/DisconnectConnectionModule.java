package org.pytenix.modules.disconnect;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.entity.TranslationModule;
import org.omni.translation.TranslatorService;
import org.omni.translation.locale.PlayerLocaleProcessor;
import org.omni.translation.module.AbstractTranslatorModule;

@Singleton
public class DisconnectConnectionModule extends AbstractTranslatorModule {

    @Inject
    public DisconnectConnectionModule(TranslatorService translatorService, PlayerLocaleProcessor playerLocaleProcessor)
    {
        super(translatorService, playerLocaleProcessor, TranslationModule.KICK_BAN);
    }
}
