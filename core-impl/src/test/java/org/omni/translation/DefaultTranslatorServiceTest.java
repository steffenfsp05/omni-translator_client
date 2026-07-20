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
import org.omni.placeholder.pipeline.impl.DefaultTranslationPipeline;
import org.omni.placeholder.service.PlaceholderService;
import org.omni.proto.generated.Protobuf;
import org.omni.translation.locale.PlayerLocaleProcessor;
import org.omni.transport.endpoint.ProfileEndpoint;
import org.omni.transport.endpoint.TranslationEndpoint;

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
    @Mock private EventService eventService;
    @Mock private PlayerLocaleProcessor localeProcessor;
    @Mock private DefaultTranslationPipeline defaultTranslationPipeline;

    @Mock private PlaceholderService placeholderService;
    private DefaultTranslatorService translatorService;
    private final UUID testPlayerUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        translatorService = new DefaultTranslatorService(
                translationEndpoint, defaultTranslationPipeline, profileEndpoint,
                 eventService, localeProcessor,placeholderService
        );
    }

    @Test
    void testRequiresTranslation_ProfileEndpointThrowsException_DefaultsToFalse() throws Exception {
        ServerConfiguration config = mock(ServerConfiguration.class);
        when(config.getDefaultLanguage()).thenReturn("en");
        translatorService.setTranslationConfiguration(config);

        when(localeProcessor.retrieveLocale(testPlayerUuid)).thenReturn("de_de");

        when(profileEndpoint.sendRequest(testPlayerUuid))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Database offline")));

        CompletableFuture<Boolean> future = translatorService.requiresTranslation(testPlayerUuid);
        assertFalse(future.get());
    }

    @Test
    void testTranslate_NullModule_IsHandledGracefully() {
        assertThrows(IllegalArgumentException.class, () -> {
            translatorService.translate("Text", "en", null);
        }, "Sollte eine Exception werfen, wenn das Modul null ist.");
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
    void testProcess_DelegatesDirectlyToTranslationEndpoint() throws Exception {
        UUID id = UUID.randomUUID();
        when(translationEndpoint.sendRequest(any(TranslationRequestData.class)))
                .thenReturn(CompletableFuture.completedFuture("Ergebnis"));

        String result = translatorService.process(id, "Text", "en", TranslationModule.LIVE_CHAT).get();

        assertEquals("Ergebnis", result);
    }


}