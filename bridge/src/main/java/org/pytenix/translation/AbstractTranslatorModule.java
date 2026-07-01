package org.pytenix.translation;

import lombok.Getter;
import org.pytenix.entity.ServerConfiguration;
import org.pytenix.profile.ProfileService;
import org.pytenix.proto.generated.NetworkPackets;
import org.pytenix.translation.locale.PlayerLocaleProcessor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public abstract class AbstractTranslatorModule {


    final ProfileService profileService;
    final TranslatorService translatorService;
    final PlayerLocaleProcessor playerLocaleProcessor;

    final String moduleName;


    public AbstractTranslatorModule(ProfileService profileService, TranslatorService translatorService, String moduleName, PlayerLocaleProcessor playerLocaleProcessor) {

        this.profileService = profileService;
        this.translatorService = translatorService;
        this.playerLocaleProcessor = playerLocaleProcessor;
        this.moduleName = moduleName;
    }


    public boolean isActive() {
        return getServerConfiguration().getModules().getOrDefault(moduleName, true);
    }

    public CompletableFuture<Boolean> requiresTranslation(UUID playerUUID) {

        System.out.println("REQUIRING TRANSLATION");
        if (getServerConfiguration() == null || getServerConfiguration().getDefaultLanguage() == null) {
            return CompletableFuture.completedFuture(true);
        }

        String playerLocale = playerLocaleProcessor.retrieveLocale(playerUUID);
        if (playerLocale != null && playerLocale.startsWith(getServerConfiguration().getDefaultLanguage())) {
            return CompletableFuture.completedFuture(false);
        }

        return profileService.retrieveProfile(playerUUID)
                .thenApply(profileData -> {
                    System.out.println("REQUIRING TRANSLATION - " + profileData.consentType());
                    if(getServerConfiguration().getConsentMode().equals(ServerConfiguration.ConsentMode.AUTO_OPT) &&
                        profileData.consentType().equals(NetworkPackets.ProfilePacket.ConsentType.AUTO))
                        return true;

                    return !profileData.consentType().equals(NetworkPackets.ProfilePacket.ConsentType.DECLINED);
                });
    }

    public ServerConfiguration getServerConfiguration() {
        return translatorService.getTranslationConfiguration();
    }


    private String generateKey(String text, String lang) {
        return text + ":" + lang;
    }


    public CompletableFuture<String> translate(String text, String locale) {


        //TODO: IMPLEMENT!!!
        //  String cached = translatorPlugin.getCaffeineCache().get(generateKey(text, locale));

        //  if (cached != null)
        //      return CompletableFuture.completedFuture(cached);

        return translatorService.translate(text, locale, this.moduleName).whenComplete((result, throwable) -> {

            if (throwable == null && result != null) {
                //translatorPlugin.getCaffeineCache().set(generateKey(text, locale), result);
            }

        });
    }


}
