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
import org.omni.event.register.ConsentUpdateEvent;
import org.omni.packets.data.ConsentRefreshRequestData;
import org.omni.profile.AbstractAnalyticsSecret;
import org.omni.profile.AnalyticsKey;
import org.omni.proto.generated.Protobuf;
import org.omni.transport.endpoint.ProfileEndpoint;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Singleton
public class OmniCommand implements BasicCommand {

    private final EventService eventService;
    private final AbstractAnalyticsSecret abstractAnalyticsSecret;
    private final ProfileEndpoint profileEndpoint;

    private final MiniMessage mm = MiniMessage.miniMessage();

    @Inject
    public OmniCommand(
            EventService eventService,
            AbstractAnalyticsSecret abstractAnalyticsSecret,
            ProfileEndpoint profileEndpoint
    ) {
        this.eventService = eventService;
        this.abstractAnalyticsSecret = abstractAnalyticsSecret;
        this.profileEndpoint = profileEndpoint;
    }

    @Override
    public void execute(@NotNull CommandSourceStack source, @NotNull String[] args) {
        CommandSender sender = source.getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Dieser Befehl ist nur für Spieler verfügbar."));
            return;
        }

        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            handleInfoAboutPlayer(player, args[1]);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> handlePrivacy(player);
            case "accept" -> handleConsent(player, true);
            case "decline" -> handleConsent(player, false);
            case "toggle" -> handleToggle(player);
            case "export" -> handleExport(player);
            case "delete" -> handleDelete(player);
            default -> sendHelp(player);
        }
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack source, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subCommands = List.of("info", "accept", "decline", "toggle", "export", "delete");
            List<String> results = new ArrayList<>();
            String search = args[0].toLowerCase();
            for (String s : subCommands) {
                if (s.startsWith(search)) {
                    results.add(s);
                }
            }
            return results;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            if (source.getSender().hasPermission("omni.admin")) {
                List<String> results = new ArrayList<>();
                String search = args[1].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(search)) {
                        results.add(player.getName());
                    }
                }
                return results;
            }
        }

        return List.of();
    }

    private String formatAnalyticId(UUID uuid) {
        final AnalyticsKey analyticsKey = abstractAnalyticsSecret.getAnalyticsKey(uuid);
        return Base64.getEncoder().encodeToString(analyticsKey.bytes());
    }

    private void handleInfoAboutPlayer(Player p, String targetPlayer)
    {
        if(!p.hasPermission("omni.admin"))
        {
            p.sendMessage(mm.deserialize("<red>No permission"));
            return;
        }

        @Nullable OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(targetPlayer);
        if(player == null)
        {
            p.sendMessage(mm.deserialize("<red>This player does not exists on this server!"));
            return;
        }

        profileEndpoint.sendRequest(player.getUniqueId()).thenAccept(profileResultData ->
        {
            String status = "<white>Unknown";

            switch (profileResultData.consentType()) {
                case EXPLICIT -> status = "<green>Opt-In";
                case DECLINED -> status = "<red>Opt-Out";
                case AUTO -> status = "<yellow>Auto";
                case UNKNOWN -> status = "<white>Unknown";
            }

            p.sendMessage(mm.deserialize("<gold>--- OmniTranslator Privacy ---"));
            p.sendMessage(mm.deserialize("<grey>Name: " + player.getName()));
            p.sendMessage(mm.deserialize("<gray>Analytics Id: <white>" + formatAnalyticId(player.getUniqueId())));
            p.sendMessage(mm.deserialize("<gray>Status: " + status));
        });


    }

    private void handlePrivacy(Player p) {
        final UUID playerId = p.getUniqueId();

        profileEndpoint.sendRequest(playerId).thenAccept(profileResultData -> {
            String status = "<white>Unknown";

            switch (profileResultData.consentType()) {
                case EXPLICIT -> status = "<green>Opt-In";
                case DECLINED -> status = "<red>Opt-Out";
                case AUTO -> status = "<yellow>Auto";
                case UNKNOWN -> status = "<white>Unknown";
            }

            p.sendMessage(mm.deserialize("<gold>--- OmniTranslator Privacy ---"));
            p.sendMessage(mm.deserialize("<gray>Your Analytics Id: <white>" + formatAnalyticId(playerId)));
            p.sendMessage(mm.deserialize("<gray>Status: " + status));
        });
    }

    private void handleConsent(Player p, boolean accept) {
        p.sendMessage(mm.deserialize("<gray>Verarbeite Anfrage..."));

        profileEndpoint.sendRequest(p.getUniqueId()).thenAccept(profileResultData -> {
            String message = accept ? "enabled" : "<red>disabled";
            Protobuf.ConsentType newConsent = accept ? Protobuf.ConsentType.EXPLICIT : Protobuf.ConsentType.DECLINED;

            profileEndpoint.update(profileResultData.withConsentType(newConsent));

            eventService.callEvent(new ConsentUpdateEvent( new ConsentRefreshRequestData(UUID.randomUUID(), p.getUniqueId(), newConsent)));

            p.sendMessage(mm.deserialize("<green>Translations " + message));
        });
    }

    private void handleToggle(Player p) {
        profileEndpoint.sendRequest(p.getUniqueId()).thenAccept(profileResultData -> {
            handleConsent(p, profileResultData.consentType() == Protobuf.ConsentType.DECLINED);
        });
    }

    private void handleExport(Player p) {
        p.sendMessage(mm.deserialize("<yellow>Export-Anfrage wird an Backend gesendet..."));
    }

    private void handleDelete(Player p) {
        p.sendMessage(mm.deserialize("<red>Lösch-Anfrage für alle deine Daten wurde gesendet."));
    }

    private void sendHelp(Player p) {
        p.sendMessage(mm.deserialize("<gold>OmniTranslator Commands:"));
        p.sendMessage(mm.deserialize("<gray>/omni info <white>- Show Analytic ID & current Status."));
        p.sendMessage(mm.deserialize("<gray>/omni toggle <white>- turn Omni Services on-/off."));
        p.sendMessage(mm.deserialize("<gray>/omni export <white>- Export Data we've collected about you."));
        p.sendMessage(mm.deserialize("<gray>/omni export/delete <white>- Delete your collected Data."));

        if(p.hasPermission("omni.admin"))
            p.sendMessage(mm.deserialize("<gray>/omni info <Playername> <white>- Get the Analytic Id from a Player."));
    }
}