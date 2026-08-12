package org.pytenix.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.omni.entity.TranslationModule;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.CacheInvalidationRequest;
import org.omni.transport.TransportSender;
import org.omni.util.SignalOperations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Singleton
public class DebugCommand implements BasicCommand {

    private final MiniMessage mm = MiniMessage.miniMessage();


    final TransportSender transportSender;

    @Inject
    public DebugCommand(TransportSender transportSender) {
      this.transportSender = transportSender;
    }

    @Override
    public void execute(@NotNull CommandSourceStack source, @NotNull String[] args) {
        CommandSender sender = source.getSender();

        if (!sender.hasPermission("omni.admin")) {
            sender.sendMessage(mm.deserialize("<red>Dazu hast du keine Rechte."));
            return;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        if (args[0].equalsIgnoreCase("resetcache")) {
            if (args.length < 2) {
                sender.sendMessage(mm.deserialize("<red>Bitte gib an, welcher Cache resettet werden soll: <translation|profile|all>"));
                return;
            }

            String targetCache = args[1].toLowerCase();
            switch (targetCache) {
                case "translation" -> {

                    transportSender.sendPacket(PacketRegistry.CACHE_INVALIDATION,
                            new CacheInvalidationRequest(UUID.randomUUID(), new CacheInvalidationRequest.Translation("*","*", TranslationModule.LIVE_CHAT)));

                    sender.sendMessage(mm.deserialize("<green>Translation Cache wurde erfolgreich resettet."));
                }
                case "profile" -> {

                    transportSender.sendPacket(PacketRegistry.CACHE_INVALIDATION,
                            new CacheInvalidationRequest(UUID.randomUUID(), new CacheInvalidationRequest.Profile(SignalOperations.CACHE_PROFILE_INVALIDATION_ALL)));
                    sender.sendMessage(mm.deserialize("<green>Profile Cache wurde erfolgreich resettet."));
                }
                case "all" -> {
                    // TODO: Implementiere hier deine Logik für alle Caches

                    transportSender.sendPacket(PacketRegistry.CACHE_INVALIDATION,
                            new CacheInvalidationRequest(UUID.randomUUID(), new CacheInvalidationRequest.Profile(SignalOperations.CACHE_PROFILE_INVALIDATION_ALL)));

                    transportSender.sendPacket(PacketRegistry.CACHE_INVALIDATION,
                            new CacheInvalidationRequest(UUID.randomUUID(), new CacheInvalidationRequest.Translation("*","*", TranslationModule.LIVE_CHAT)));
                    sender.sendMessage(mm.deserialize("<green>Alle Caches wurden erfolgreich resettet."));
                }
                default -> sender.sendMessage(mm.deserialize("<red>Unbekannter Cache-Typ. Nutze: <translation|profile|all>"));
            }
            return;
        }

        sendHelp(sender);
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack source, @NotNull String[] args) {
        if (!source.getSender().hasPermission("omni.admin")) {
            return List.of();
        }

        if (args.length == 0 || args.length == 1) {
            String currentArg = args.length == 0 ? "" : args[0];
            return filterStart(List.of("resetcache"), currentArg);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("resetcache")) {
            return filterStart(List.of("translation", "profile", "all"), args[1]);
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

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(mm.deserialize("<gold>OmniDebug Commands:"));
        sender.sendMessage(mm.deserialize("<gray>/omniadmin resetcache <translation|profile|all> <white>- Resettet den ausgewählten Cache."));
    }
}