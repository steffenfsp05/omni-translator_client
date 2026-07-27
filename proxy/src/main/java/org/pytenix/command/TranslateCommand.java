package org.pytenix.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import org.omni.proto.generated.Protobuf;
import org.omni.transport.endpoint.ProfileEndpoint;
import org.pytenix.TranslatorPlugin;
import org.pytenix.limbo.book.ConsentBookService;

import java.time.Duration;

@Singleton
public class TranslateCommand implements SimpleCommand {

    private final TranslatorPlugin translatorPlugin;
    private final ConsentBookService consentBookService;
    private final ProfileEndpoint profileEndpoint;

    @Inject
    public TranslateCommand(TranslatorPlugin translatorPlugin, ProfileEndpoint profileEndpoint, ConsentBookService consentBookService) {
        this.translatorPlugin = translatorPlugin;
        this.profileEndpoint = profileEndpoint;
        this.consentBookService = consentBookService;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) return;

        String[] args = invocation.arguments();

        profileEndpoint.sendRequest(player.getUniqueId())
                .thenAcceptAsync(profileData -> {

                    Protobuf.ConsentType translationConsent = profileData.translationConsent();
                    Protobuf.ConsentType analyticsConsent = profileData.analyticConsent();

                    if (args.length == 0) {
                        openBook(player);
                        return;
                    }

                    if (args.length == 1) {
                        if (args[0].equalsIgnoreCase("accept")) {
                            translationConsent = Protobuf.ConsentType.EXPLICIT;
                            analyticsConsent = Protobuf.ConsentType.EXPLICIT;
                        } else if (args[0].equalsIgnoreCase("decline")) {
                            translationConsent = Protobuf.ConsentType.DECLINED;
                            analyticsConsent = Protobuf.ConsentType.DECLINED;
                        }
                    }

                    if (args.length == 2 && args[0].equalsIgnoreCase("all")) {
                        if (args[1].equalsIgnoreCase("accept")) {
                            translationConsent = Protobuf.ConsentType.EXPLICIT;
                            analyticsConsent = Protobuf.ConsentType.EXPLICIT;
                        } else if (args[1].equalsIgnoreCase("decline")) {
                            translationConsent = Protobuf.ConsentType.DECLINED;
                            analyticsConsent = Protobuf.ConsentType.DECLINED;
                        }
                    }

                    if (args.length == 2 && args[0].equalsIgnoreCase("translation")) {
                        if (args[1].equalsIgnoreCase("accept")) {
                            translationConsent = Protobuf.ConsentType.EXPLICIT;
                        } else if (args[1].equalsIgnoreCase("decline")) {
                            translationConsent = Protobuf.ConsentType.DECLINED;
                        }
                    }

                    if (args.length == 2 && args[0].equalsIgnoreCase("analytics")) {
                        if (args[1].equalsIgnoreCase("accept")) {
                            analyticsConsent = Protobuf.ConsentType.EXPLICIT;
                        } else if (args[1].equalsIgnoreCase("decline")) {
                            analyticsConsent = Protobuf.ConsentType.DECLINED;
                        }
                    }

                    var updatedProfile = profileData
                            .withTranslationConsentType(translationConsent)
                            .withAnalyticConsentType(analyticsConsent);

                    profileEndpoint.update(updatedProfile);

                    boolean hasTranslation = translationConsent != Protobuf.ConsentType.UNKNOWN;
                    boolean hasAnalytics = analyticsConsent != Protobuf.ConsentType.UNKNOWN;

                    if (hasTranslation && hasAnalytics) {
                        player.sendMessage(Component.text("§aDeine Einstellungen wurden gespeichert!"));
                        sendToLobby(player);
                    } else {
                        player.sendMessage(Component.text("§eEinstellung gespeichert. Bitte wähle noch die verbleibende Option!"));
                        openBook(player);
                    }
                });
    }

    private void openBook(Player player) {
       consentBookService.buildBook(player.getUniqueId()).thenAccept(book ->
        {
            translatorPlugin.getProxyServer().getScheduler()
                    .buildTask(translatorPlugin, () -> player.openBook(book))
                    .delay(Duration.ofMillis(200))
                    .schedule();
        });

    }

    private void sendToLobby(Player player) {
        translatorPlugin.getProxyServer().getServer("lobby").ifPresent(lobbyServer ->
                player.createConnectionRequest(lobbyServer).fireAndForget());
    }
}