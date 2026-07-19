package org.omni.injection.modules;

import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import org.omni.placeholder.pipeline.TextProcessor;
import org.omni.placeholder.pipeline.TranslationPipeline;
import org.omni.placeholder.pipeline.impl.*;

import java.util.Arrays;
import java.util.List;

public class TranslationPipelineModule extends AbstractModule {
    @Override
    protected void configure() {

        bind(TranslationPipeline.class).to(DefaultTranslationPipeline.class).in(Scopes.SINGLETON);

    }

    @Provides
    @Singleton
    public List<TextProcessor> provideTranslationPipeline(
            GradientProcessor gradientProcessor,
            NameProtectorProcessor nameProtectorProcessor,
            WordProtectorProcessor wordProtectorProcessor,
            PlaceholderProcessor placeholderProcessor,
            NormalizerProcessor normalizerProcessor
    ) {
        return Arrays.asList(
                gradientProcessor,
                nameProtectorProcessor,
                wordProtectorProcessor,
                placeholderProcessor,
                normalizerProcessor
        );
    }
}