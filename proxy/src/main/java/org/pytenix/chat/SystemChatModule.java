package org.pytenix.chat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import org.omni.entity.TranslationModule;
import org.omni.profile.ProfileService;
import org.omni.translation.TranslatorService;
import org.omni.translation.component.TextComponentService;
import org.omni.translation.locale.PlayerLocaleProcessor;
import org.omni.translation.module.AbstractTranslatorModule;


@Getter
@Singleton
public class SystemChatModule extends AbstractTranslatorModule {

    private final TranslatorService translatorService;
    private final TextComponentService textComponentService;
    private final MessageSequencer messageSequencer;

    @Inject
    public SystemChatModule(
            ProfileService profileService,
            TranslatorService translatorService,
            TextComponentService textComponentService,
            MessageSequencer messageSequencer,
            PlayerLocaleProcessor playerLocaleProcessor
    ) {
        super(profileService, translatorService, playerLocaleProcessor, TranslationModule.PLUGIN_CHAT);
        this.translatorService = translatorService;
        this.textComponentService = textComponentService;
        this.messageSequencer = messageSequencer;
    }


}
