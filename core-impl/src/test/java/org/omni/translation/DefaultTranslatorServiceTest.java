package org.omni.translation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omni.entity.ServerConfiguration;
import org.omni.entity.ServerConsentMode;
import org.omni.entity.TranslationModule;
import org.omni.event.EventService;
import org.omni.packets.data.ProfileResultData;
import org.omni.packets.data.TranslationRequestData;
import org.omni.placeholder.gradient.ExtractionResult;
import org.omni.placeholder.gradient.GradientService;
import org.omni.placeholder.service.PlaceholderService;
import org.omni.proto.generated.Protobuf;
import org.omni.translation.locale.PlayerLocaleProcessor;
import org.omni.transport.endpoint.ProfileEndpoint;
import org.omni.transport.endpoint.TranslationEndpoint;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultTranslatorServiceTest {

    @Mock private TranslationEndpoint translationEndpoint;
    @Mock private ProfileEndpoint profileEndpoint;
    @Mock private PlaceholderService placeholderService;
    @Mock private GradientService gradientService;
    @Mock private EventService eventService;
    @Mock private PlayerLocaleProcessor localeProcessor;

    private DefaultTranslatorService translatorService;
    private final UUID testPlayerUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        translatorService = new DefaultTranslatorService(
                translationEndpoint, profileEndpoint, placeholderService,
                gradientService, eventService, localeProcessor
        );
    }

    @Test
    void testTranslate_NullOrBlank() throws Exception {
        assertEquals("", translatorService.translate("", "en", TranslationModule.LIVE_CHAT).get());
        assertNull(translatorService.translate(null, "en", TranslationModule.LIVE_CHAT).get());

        verifyNoInteractions(translationEndpoint);
    }

    @Test
    void testRequiresTranslation_NoConfig() throws Exception {
        translatorService.setTranslationConfiguration(null);
        assertTrue(translatorService.requiresTranslation(testPlayerUuid).get());
    }

    @Test
    void testRequiresTranslation_SameLocale() throws Exception {
        ServerConfiguration config = mock(ServerConfiguration.class);
        when(config.getDefaultLanguage()).thenReturn("de");
        translatorService.setTranslationConfiguration(config);

        when(localeProcessor.retrieveLocale(testPlayerUuid)).thenReturn("de_de");

        assertFalse(translatorService.requiresTranslation(testPlayerUuid).get());
    }

    @Test
    void testRequiresTranslation_ConsentDeclined() throws Exception {
        ServerConfiguration config = mock(ServerConfiguration.class);
        when(config.getDefaultLanguage()).thenReturn("en");
        when(config.getConsentMode()).thenReturn(ServerConsentMode.STRICT);
        translatorService.setTranslationConfiguration(config);

        when(localeProcessor.retrieveLocale(testPlayerUuid)).thenReturn("de_de");

        ProfileResultData mockProfile = mock(ProfileResultData.class);
        when(mockProfile.consentType()).thenReturn(Protobuf.ConsentType.DECLINED);

        when(profileEndpoint.sendRequest(testPlayerUuid)).thenReturn(CompletableFuture.completedFuture(mockProfile));

        assertFalse(translatorService.requiresTranslation(testPlayerUuid).get());
    }

    @Test
    void testRequiresTranslation_AutoOpt_Enabled() throws Exception {
        ServerConfiguration config = mock(ServerConfiguration.class);
        when(config.getDefaultLanguage()).thenReturn("en");
        when(config.getConsentMode()).thenReturn(ServerConsentMode.AUTO_OPT);
        translatorService.setTranslationConfiguration(config);

        when(localeProcessor.retrieveLocale(testPlayerUuid)).thenReturn("de_de");

        ProfileResultData mockProfile = mock(ProfileResultData.class);
        // Bei AUTO_OPT und Typ AUTO muss Übersetzung aktiv sein
        when(mockProfile.consentType()).thenReturn(Protobuf.ConsentType.AUTO);

        when(profileEndpoint.sendRequest(testPlayerUuid)).thenReturn(CompletableFuture.completedFuture(mockProfile));

        assertTrue(translatorService.requiresTranslation(testPlayerUuid).get());
    }

    @Test
    void testTranslate_ApiFailure_PropagatesException() {
        when(translationEndpoint.sendRequest(any(TranslationRequestData.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API Timeout")));

        CompletableFuture<String> future = translatorService.translate("Text", "en", TranslationModule.LIVE_CHAT);

        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    void testPipeline_GradientRestorationFlow() throws Exception {
        String inputText = "§aTest";

        ExtractionResult mockExtraction = new ExtractionResult("Test", Collections.singletonMap("G0", mock(org.omni.placeholder.gradient.GradientData.class)));
        when(gradientService.stripAndAnalyze(inputText)).thenReturn(mockExtraction);

        when(translationEndpoint.sendRequest(any(TranslationRequestData.class)))
                .thenReturn(CompletableFuture.completedFuture("Test"));

        when(gradientService.getCachedGradient(any(UUID.class)))
                .thenReturn(Collections.singletonMap("G0", mock(org.omni.placeholder.gradient.GradientData.class)));

        translatorService.translate(inputText, "en", TranslationModule.LIVE_CHAT).get();

        verify(gradientService).cacheGradient(any(UUID.class), anyMap());
        verify(gradientService).restoreGradients(any(UUID.class), eq("Test"));
        verify(gradientService).invalidCachedGradient(any(UUID.class));
    }

    @Test
    void testPipelineIntegration() throws Exception {
        String inputText = "Line1\nLine2";
        String translatedText = "Translated1\nTranslated2";

        when(gradientService.stripAndAnalyze(anyString()))
                .thenAnswer(inv -> new ExtractionResult(inv.getArgument(0), Collections.emptyMap()));

        when(placeholderService.toPlaceholders(any(UUID.class), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));

        when(placeholderService.fromPlaceholders(any(UUID.class), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));

        when(translationEndpoint.sendRequest(any(TranslationRequestData.class)))
                .thenReturn(CompletableFuture.completedFuture(translatedText));

        String result = translatorService.translate(inputText, "en", TranslationModule.LIVE_CHAT).get();

        assertEquals(translatedText, result);

        verify(placeholderService, times(2)).toPlaceholders(any(UUID.class), anyString());
        verify(placeholderService, times(2)).fromPlaceholders(any(UUID.class), anyString());
    }

    // ==================== NEUE TESTS ====================

    @Test
    void testRequiresTranslation_ConfigPresent_ButNoDefaultLanguage() throws Exception {
        ServerConfiguration config = mock(ServerConfiguration.class);
        when(config.getDefaultLanguage()).thenReturn(null);
        translatorService.setTranslationConfiguration(config);

        assertTrue(translatorService.requiresTranslation(testPlayerUuid).get());
        // Da die Sprache fehlt, darf gar nicht erst das Profil abgefragt werden
        verifyNoInteractions(profileEndpoint);
    }

    @Test
    void testRequiresTranslation_StrictMode_NonDeclinedConsent_ReturnsTrue() throws Exception {
        ServerConfiguration config = mock(ServerConfiguration.class);
        when(config.getDefaultLanguage()).thenReturn("en");
        when(config.getConsentMode()).thenReturn(ServerConsentMode.STRICT);
        translatorService.setTranslationConfiguration(config);

        when(localeProcessor.retrieveLocale(testPlayerUuid)).thenReturn("de_de");

        ProfileResultData mockProfile = mock(ProfileResultData.class);
        // Im STRICT Modus greift die AUTO_OPT Sonderregel nicht, aber "nicht abgelehnt" reicht
        when(mockProfile.consentType()).thenReturn(Protobuf.ConsentType.AUTO);
        when(profileEndpoint.sendRequest(testPlayerUuid)).thenReturn(CompletableFuture.completedFuture(mockProfile));

        assertTrue(translatorService.requiresTranslation(testPlayerUuid).get());
    }

    @Test
    void testRequiresTranslation_AutoOpt_DeclinedConsent_ReturnsFalse() throws Exception {
        ServerConfiguration config = mock(ServerConfiguration.class);
        when(config.getDefaultLanguage()).thenReturn("en");
        when(config.getConsentMode()).thenReturn(ServerConsentMode.AUTO_OPT);
        translatorService.setTranslationConfiguration(config);

        when(localeProcessor.retrieveLocale(testPlayerUuid)).thenReturn("de_de");

        ProfileResultData mockProfile = mock(ProfileResultData.class);
        when(mockProfile.consentType()).thenReturn(Protobuf.ConsentType.DECLINED);
        when(profileEndpoint.sendRequest(testPlayerUuid)).thenReturn(CompletableFuture.completedFuture(mockProfile));

        assertFalse(translatorService.requiresTranslation(testPlayerUuid).get());
    }

    @Test
    void testRequiresTranslation_LocaleIsNull_FallsBackToProfileCheck() throws Exception {
        ServerConfiguration config = mock(ServerConfiguration.class);
        when(config.getDefaultLanguage()).thenReturn("en");
        when(config.getConsentMode()).thenReturn(ServerConsentMode.STRICT);
        translatorService.setTranslationConfiguration(config);

        // Spieler hat (noch) keine bekannte Locale
        when(localeProcessor.retrieveLocale(testPlayerUuid)).thenReturn(null);

        ProfileResultData mockProfile = mock(ProfileResultData.class);
        when(mockProfile.consentType()).thenReturn(Protobuf.ConsentType.DECLINED);
        when(profileEndpoint.sendRequest(testPlayerUuid)).thenReturn(CompletableFuture.completedFuture(mockProfile));

        assertFalse(translatorService.requiresTranslation(testPlayerUuid).get());
    }

    @Test
    void testHandleGradient_NullGradientsMap_ReturnsOriginalTextUnchanged() {
        ExtractionResult mockExtraction = mock(ExtractionResult.class);
        when(mockExtraction.gradients()).thenReturn(null);
        when(gradientService.stripAndAnalyze("Text")).thenReturn(mockExtraction);

        String result = translatorService.handleGradient(testPlayerUuid, "Text");

        assertEquals("Text", result);
        verify(gradientService, never()).cacheGradient(any(UUID.class), anyMap());
    }

    @Test
    void testHandleGradient_EmptyGradientsMap_StillCachesAndReturnsCleanText() {
        ExtractionResult mockExtraction = mock(ExtractionResult.class);
        when(mockExtraction.gradients()).thenReturn(Collections.emptyMap());
        when(mockExtraction.cleanText()).thenReturn("CleanedText");
        when(gradientService.stripAndAnalyze("Text")).thenReturn(mockExtraction);

        String result = translatorService.handleGradient(testPlayerUuid, "Text");

        // Da gradients() != null ist (auch wenn leer), wird trotzdem gecacht
        assertEquals("CleanedText", result);
        verify(gradientService).cacheGradient(testPlayerUuid, Collections.emptyMap());
    }

    @Test
    void testProcess_DelegatesDirectlyToTranslationEndpoint() throws Exception {
        UUID id = UUID.randomUUID();
        when(translationEndpoint.sendRequest(any(TranslationRequestData.class)))
                .thenReturn(CompletableFuture.completedFuture("Ergebnis"));

        String result = translatorService.process(id, "Text", "en", TranslationModule.LIVE_CHAT).get();

        assertEquals("Ergebnis", result);
    }

    @Test
    void testTranslate_MultiLine_AllLinesHaveGradients() throws Exception {
        String inputText = "§aLine1\n§bLine2\n§cLine3";

        when(gradientService.stripAndAnalyze(anyString()))
                .thenAnswer(inv -> new ExtractionResult(inv.getArgument(0),
                        Collections.singletonMap("G0", mock(org.omni.placeholder.gradient.GradientData.class))));

        when(placeholderService.toPlaceholders(any(UUID.class), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));
        when(placeholderService.fromPlaceholders(any(UUID.class), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));

        when(translationEndpoint.sendRequest(any(TranslationRequestData.class)))
                .thenReturn(CompletableFuture.completedFuture("Line1\nLine2\nLine3"));

        when(gradientService.getCachedGradient(any(UUID.class)))
                .thenReturn(Collections.singletonMap("G0", mock(org.omni.placeholder.gradient.GradientData.class)));

        String result = translatorService.translate(inputText, "en", TranslationModule.LIVE_CHAT).get();

        assertEquals("Line1\nLine2\nLine3", result);
        verify(gradientService, times(3)).cacheGradient(any(UUID.class), anyMap());
        verify(gradientService, times(3)).restoreGradients(any(UUID.class), anyString());
        verify(gradientService, times(3)).invalidCachedGradient(any(UUID.class));
    }

    @Test
    void testTranslate_TranslatedResultHasFewerLines_MissingLinesAreEmpty() throws Exception {
        String inputText = "Line1\nLine2\nLine3";

        when(gradientService.stripAndAnalyze(anyString()))
                .thenAnswer(inv -> new ExtractionResult(inv.getArgument(0), Collections.emptyMap()));
        when(placeholderService.toPlaceholders(any(UUID.class), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));
        when(placeholderService.fromPlaceholders(any(UUID.class), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));

        // Übersetzungsdienst liefert (fehlerhaft) nur eine Zeile statt drei zurück
        when(translationEndpoint.sendRequest(any(TranslationRequestData.class)))
                .thenReturn(CompletableFuture.completedFuture("OnlyOneLine"));

        String result = translatorService.translate(inputText, "en", TranslationModule.LIVE_CHAT).get();

        // Fehlende Zeilen werden als Leerstrings aufgefüllt statt einer Exception
        assertEquals("OnlyOneLine\n\n", result);
    }

    @Test
    void testTranslate_TranslatedResultHasMoreLines_ExtraLinesAreIgnored() throws Exception {
        String inputText = "Line1\nLine2";

        when(gradientService.stripAndAnalyze(anyString()))
                .thenAnswer(inv -> new ExtractionResult(inv.getArgument(0), Collections.emptyMap()));
        when(placeholderService.toPlaceholders(any(UUID.class), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));
        when(placeholderService.fromPlaceholders(any(UUID.class), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));

        when(translationEndpoint.sendRequest(any(TranslationRequestData.class)))
                .thenReturn(CompletableFuture.completedFuture("Trans1\nTrans2\nUnexpectedExtra"));

        String result = translatorService.translate(inputText, "en", TranslationModule.LIVE_CHAT).get();

        // Es werden nur so viele Zeilen zurückgegeben, wie ursprünglich vorhanden waren
        assertEquals("Trans1\nTrans2", result);
    }

    @Test
    void testTranslate_PreservesEmptyLinesInMultiLineText() throws Exception {
        String inputText = "Line1\n\nLine3";

        when(gradientService.stripAndAnalyze(anyString()))
                .thenAnswer(inv -> new ExtractionResult(inv.getArgument(0), Collections.emptyMap()));
        when(placeholderService.toPlaceholders(any(UUID.class), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));
        when(placeholderService.fromPlaceholders(any(UUID.class), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));

        when(translationEndpoint.sendRequest(any(TranslationRequestData.class)))
                .thenReturn(CompletableFuture.completedFuture("Line1\n\nLine3"));

        String result = translatorService.translate(inputText, "en", TranslationModule.LIVE_CHAT).get();

        assertEquals(inputText, result);
        verify(placeholderService, times(3)).toPlaceholders(any(UUID.class), anyString());
    }
}