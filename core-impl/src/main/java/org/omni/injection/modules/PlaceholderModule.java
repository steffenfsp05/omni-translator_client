package org.omni.injection.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import org.omni.placeholder.gradient.GradientService;
import org.omni.placeholder.impl.DefaultGradientService;
import org.omni.placeholder.impl.DefaultPlaceholderNormalizer;
import org.omni.placeholder.impl.DefaultPlaceholderService;
import org.omni.placeholder.normalizer.PlaceholderNormalizer;
import org.omni.placeholder.protect.impl.DefaultPlayerNameProtector;
import org.omni.placeholder.protect.impl.DefaultWordProtector;
import org.omni.placeholder.protector.PlayerNameProtector;
import org.omni.placeholder.protector.WordProtector;
import org.omni.placeholder.service.PlaceholderService;
import org.omni.translation.DefaultTranslatorService;
import org.omni.translation.TranslatorService;

public class PlaceholderModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(PlaceholderService.class).to(DefaultPlaceholderService.class).in(Scopes.SINGLETON);
        bind(GradientService.class).to(DefaultGradientService.class).in(Scopes.SINGLETON);
        bind(PlaceholderNormalizer.class).to(DefaultPlaceholderNormalizer.class).in(Scopes.SINGLETON);
        bind(PlayerNameProtector.class).to(DefaultPlayerNameProtector.class).in(Scopes.SINGLETON);
        bind(WordProtector.class).to(DefaultWordProtector.class).in(Scopes.SINGLETON);

    }
}