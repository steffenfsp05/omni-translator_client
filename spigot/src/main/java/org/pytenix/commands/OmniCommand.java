package org.pytenix.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.omni.event.EventService;
import org.omni.event.register.player.ConsentUpdateEvent;
import org.omni.packets.data.ConsentRefreshRequestData;
import org.omni.profile.AbstractAnalyticsSecret;
import org.omni.profile.AnalyticsKey;
import org.omni.proto.generated.Protobuf;
import org.omni.transport.endpoint.DataExportEndpoint;
import org.omni.transport.endpoint.ProfileEndpoint;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Singleton
public class OmniCommand implements BasicCommand {

    private final EventService eventService;
    private final AbstractAnalyticsSecret abstractAnalyticsSecret;
    private final ProfileEndpoint profileEndpoint;
    private final DataExportEndpoint dataExportEndpoint;

    private final MiniMessage mm = MiniMessage.miniMessage();

    @Inject
    public OmniCommand(
            EventService eventService,
            AbstractAnalyticsSecret abstractAnalyticsSecret,
            ProfileEndpoint profileEndpoint,
            DataExportEndpoint dataExportEndpoint
    ) {
        this.eventService = eventService;
        this.abstractAnalyticsSecret = abstractAnalyticsSecret;
        this.profileEndpoint = profileEndpoint;
        this.dataExportEndpoint = dataExportEndpoint;
    }

    @Override
    public void execute(@NotNull CommandSourceStack source, @NotNull String[] args) {
        CommandSender sender = source.getSender();

        if (args.length == 2 && args[0].equalsIgnoreCase("reset") && sender.hasPermission("omni.admin")) {
            resetProfile(sender, args[1]);
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Dieser Befehl ist nur für Spieler verfügbar. Test"));
            return;
        }

        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("info") && player.hasPermission("omni.admin")) {
            handleInfoAboutPlayer(player, args[1]);
            return;
        }


        switch (args[0].toLowerCase()) {
            case "info" -> handlePrivacy(player);
            case "accept" -> handleConsent(player, true, args);
            case "decline" -> handleConsent(player, false, args);
            case "toggle" -> handleToggle(player, args);
            case "export" -> handleExport(player);
            case "delete" -> handleDelete(player);

            default -> sendHelp(player);
        }
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack source, @NotNull String[] args) {
        if (args.length == 0 || args.length == 1) {
            String currentArg = args.length == 0 ? "" : args[0];

            List<String> subCommands = new ArrayList<>(List.of(
                    "info", "accept", "decline", "toggle", "export", "delete"
            ));

            if (source.getSender().hasPermission("omni.admin")) {
                subCommands.add("reset");
            }

            return filterStart(subCommands, currentArg);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("accept") || sub.equals("decline")) {
                return filterStart(List.of("all", "translation", "analytics"), args[1]);
            }
            if (sub.equals("toggle")) {
                return filterStart(List.of("translation", "analytics"), args[1]);
            }
            if ((sub.equals("info") ||sub.equals("reset"))&& source.getSender().hasPermission("omni.admin")) {
                List<String> players = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList();
                return filterStart(players, args[1]);
            }
        }

        return List.of();
    }

    private List<String> filterStart(Collection<String> list, String search) {
        String lower = search.toLowerCase();
        List<String> results = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(lower)) {
                results.add(s);
            }
        }
        return results;
    }

    private String formatAnalyticId(UUID uuid) {
        final AnalyticsKey analyticsKey = abstractAnalyticsSecret.getAnalyticsKey(uuid);
        return Base64.getEncoder().encodeToString(analyticsKey.bytes());
    }

    private String formatConsentStatus(Protobuf.ConsentType consentType) {
        if (consentType == null) return "<white>Unknown";
        return switch (consentType) {
            case EXPLICIT -> "<green>Opt-In";
            case DECLINED -> "<red>Opt-Out";
            case AUTO -> "<yellow>Auto";
            case UNKNOWN, UNRECOGNIZED -> "<white>Unknown";
        };
    }

    private void resetProfile(CommandSender sender, String targetPlayer) {
        @Nullable OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(targetPlayer);
        if (player == null) {
            sender.sendMessage(mm.deserialize("<red>This player does not exist on this server!"));
            return;
        }

        profileEndpoint.sendRequest(player.getUniqueId()).thenAccept(profileResultData -> {
            profileEndpoint.update(profileResultData.withAnalyticConsentType(Protobuf.ConsentType.UNKNOWN).withTranslationConsentType(Protobuf.ConsentType.UNKNOWN));
            sender.sendMessage(mm.deserialize("<red>Profile resetted ["+player.getUniqueId()+"]"));
        });
    }

    private void handleInfoAboutPlayer(Player p, String targetPlayer) {
        @Nullable OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(targetPlayer);
        if (player == null) {
            p.sendMessage(mm.deserialize("<red>This player does not exist on this server!"));
            return;
        }
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss - dd.MM.yyyy");

        profileEndpoint.sendRequest(player.getUniqueId()).thenAccept(profileResultData -> {
            p.sendMessage(mm.deserialize("<gold>--- OmniTranslator Privacy (" + player.getName() + ") ---"));
            p.sendMessage(mm.deserialize("<gray>Analytics Id: <white>" + formatAnalyticId(player.getUniqueId())));
            p.sendMessage(mm.deserialize("<gray>Translation Status: " + formatConsentStatus(profileResultData.translationConsent())));
            p.sendMessage(mm.deserialize("<gray>Analytics Status: " + formatConsentStatus(profileResultData.analyticConsent())));
            p.sendMessage(mm.deserialize("<gray>Analytics Last Opt-In: " + formatMillis(profileResultData.analyticsAcceptedTimestamp())));
            p.sendMessage(mm.deserialize("<gray>Translation Last Opt-In: " + formatMillis(profileResultData.translationsAcceptedTimestamp())));
        });
    }

    private void handlePrivacy(Player p) {
        final UUID playerId = p.getUniqueId();

        profileEndpoint.sendRequest(playerId).thenAccept(profileResultData -> {

            p.sendMessage(mm.deserialize("<gold>--- OmniTranslator Privacy ---"));
            p.sendMessage(mm.deserialize("<gray>Your Analytics Id: <white>" + formatAnalyticId(playerId)));
            p.sendMessage(mm.deserialize("<gray>Translation Status: " + formatConsentStatus(profileResultData.translationConsent())));
            p.sendMessage(mm.deserialize("<gray>Analytics Status: " + formatConsentStatus(profileResultData.analyticConsent())));
            p.sendMessage(mm.deserialize("<gray>Analytics Last Opt-In: " + formatMillis(profileResultData.analyticsAcceptedTimestamp())));
            p.sendMessage(mm.deserialize("<gray>Translation Last Opt-In: " + formatMillis(profileResultData.translationsAcceptedTimestamp())));
        });
    }

    final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss - dd.MM.yyyy");

    public String formatMillis(long millis)
    {
        if(millis <=  0)
            return "<red>No data";

        return dateFormat.format(millis);
    }

    private void handleConsent(Player p, boolean accept, String[] args) {
        p.sendMessage(mm.deserialize("<gray>Verarbeite Anfrage..."));

        String targetModule = (args.length >= 2) ? args[1].toLowerCase() : "all";

        profileEndpoint.sendRequest(p.getUniqueId()).thenAccept(profileResultData -> {
            Protobuf.ConsentType targetConsent = accept ? Protobuf.ConsentType.EXPLICIT : Protobuf.ConsentType.DECLINED;

            Protobuf.ConsentType newTranslation = profileResultData.translationConsent();
            Protobuf.ConsentType newAnalytics = profileResultData.analyticConsent();

            switch (targetModule) {
                case "translation" -> newTranslation = targetConsent;
                case "analytics" -> newAnalytics = targetConsent;
                default -> { // "all" oder nicht angegeben
                    newTranslation = targetConsent;
                    newAnalytics = targetConsent;
                }
            }

            var updatedProfile = profileResultData
                    .withTranslationConsentType(newTranslation)
                    .withAnalyticConsentType(newAnalytics);

            profileEndpoint.update(updatedProfile);

            eventService.callEvent(new ConsentUpdateEvent(
                    new ConsentRefreshRequestData(UUID.randomUUID(), p.getUniqueId(), newTranslation, newAnalytics)
            ));

            String statusMsg = accept ? "<green>erlaubt" : "<red>deaktiviert";
            String targetMsg = switch (targetModule) {
                case "translation" -> "Chat-Übersetzung";
                case "analytics" -> "Analytics-Tracking";
                default -> "Alle Omni Services";
            };

            p.sendMessage(mm.deserialize("<gray>" + targetMsg + " wurden " + statusMsg + "."));
        });
    }

    private void handleToggle(Player p, String[] args) {
        String targetModule = (args.length >= 2) ? args[1].toLowerCase() : "translation";

        profileEndpoint.sendRequest(p.getUniqueId()).thenAccept(profileResultData -> {
            boolean currentChoice;
            if (targetModule.equalsIgnoreCase("analytics")) {
                currentChoice = profileResultData.analyticConsent() == Protobuf.ConsentType.EXPLICIT;
            } else {
                currentChoice = profileResultData.translationConsent() == Protobuf.ConsentType.EXPLICIT;
            }

            handleConsent(p, !currentChoice, args);
        });
    }

    private void handleExport(Player p) {
        p.sendMessage(mm.deserialize("<yellow>Export-Anfrage wird an Backend gesendet..."));
        dataExportEndpoint.sendRequest(p.getUniqueId()).thenAccept(dataId -> {

            //DUMMY ADDRESS; NEEDS TO BE REDO
            String dataUrl = "http://192.168.178.121:8083/api/v1/analytics/data?id="+dataId;

            String message = String.format("<yellow>Du kannst deine Daten einsehen unter:\n" +
                    " <click:open_url:'%s'><underlined><blue>%s</blue></underlined></click>\n<red>Dieser Link ist für 24 Stunden gültig.", dataUrl, dataUrl);
            p.sendMessage(mm.deserialize(message));
        });
    }
    private void handleDelete(Player p) {
        p.sendMessage(mm.deserialize("<red>Lösch-Anfrage für alle deine Daten wurde gesendet."));
    }

    private void sendHelp(Player p) {
        p.sendMessage(mm.deserialize("<gold>OmniTranslator Commands:"));
        p.sendMessage(mm.deserialize("<gray>/omni info <white>- Show Analytic ID & current Status."));
        p.sendMessage(mm.deserialize("<gray>/omni accept [translation|analytics|all] <white>- Enable services."));
        p.sendMessage(mm.deserialize("<gray>/omni decline [translation|analytics|all] <white>- Disable services."));
        p.sendMessage(mm.deserialize("<gray>/omni toggle [translation|analytics] <white>- Toggle Omni services."));
        p.sendMessage(mm.deserialize("<gray>/omni export <white>- Export Data we've collected about you."));
        p.sendMessage(mm.deserialize("<gray>/omni delete <white>- Delete your collected Data."));

        if (p.hasPermission("omni.admin")) {
            p.sendMessage(mm.deserialize("<gray>/omni info <Playername> <white>- Get the Analytic Id from a Player."));
            p.sendMessage(mm.deserialize("<gray>/omni reset <Playername> <white>- Resets the profile from a player."));
        }
    }
}