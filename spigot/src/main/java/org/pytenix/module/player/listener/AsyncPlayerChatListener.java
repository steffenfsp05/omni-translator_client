package org.pytenix.module.player.listener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.omni.translation.component.TextComponentService;
import org.pytenix.module.player.LiveChatModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Singleton
public class AsyncPlayerChatListener implements Listener {

    private final Provider<LiveChatModule> liveChatModuleProvider;
    private final TextComponentService textComponentUtil;

    @Inject
    public AsyncPlayerChatListener(Provider<LiveChatModule> liveChatModuleProvider, TextComponentService textComponentUtil) {
        this.liveChatModuleProvider = liveChatModuleProvider;
        this.textComponentUtil = textComponentUtil;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {

        LiveChatModule liveChatModule = liveChatModuleProvider.get();
        if (!liveChatModule.isModuleActive()) return;

        Player sender = event.getPlayer();
        Component originalMessage = event.message();

        List<Player> targetPlayers = new ArrayList<>();
        for (Audience audience : event.viewers()) {
            if (audience instanceof Player p && !p.getUniqueId().equals(sender.getUniqueId())) {
                targetPlayers.add(p);
            }
        }

        event.viewers().clear();
        event.viewers().add(sender);

        if (targetPlayers.isEmpty()) return;

        CompletableFuture<String> senderLocaleFuture = liveChatModule.getPlayerLocaleProcessor().retrieveLocale(sender.getUniqueId());

        Map<Player, CompletableFuture<String>> targetLocaleFutures = new HashMap<>();
        for (Player p : targetPlayers) {
            targetLocaleFutures.put(p, liveChatModule.getPlayerLocaleProcessor().retrieveLocale(p.getUniqueId()));
        }

        List<CompletableFuture<?>> allFutures = new ArrayList<>(targetLocaleFutures.values());
        allFutures.add(senderLocaleFuture);

        CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).thenAccept(v -> {

            String senderLocale = senderLocaleFuture.join();
            if (senderLocale == null) senderLocale = "en_us"; // Fallback

            Map<String, List<Player>> languageGroups = new HashMap<>();

            for (Map.Entry<Player, CompletableFuture<String>> entry : targetLocaleFutures.entrySet()) {
                Player targetPlayer = entry.getKey();
                String targetLocale = entry.getValue().join();

                if (targetLocale == null) targetLocale = "en_us";

                if (targetLocale.equals(senderLocale)) {
                    if (targetPlayer.isOnline()) {
                        Component rendered = event.renderer().render(sender, sender.displayName(), originalMessage, targetPlayer);
                        liveChatModule.sendSystemMessage(targetPlayer, rendered);
                    }
                } else {
                    languageGroups.computeIfAbsent(targetLocale, k -> new ArrayList<>()).add(targetPlayer);
                }
            }

            if (languageGroups.isEmpty()) return;

            languageGroups.forEach((targetLang, groupMembers) -> {
                textComponentUtil.translateComplexMessage(originalMessage, targetLang, liveChatModule.getTranslationModule())
                        .orTimeout(5, TimeUnit.SECONDS)
                        .whenComplete((translatedText, ex) -> {

                            Component finalText = (ex == null && translatedText != null) ? translatedText : originalMessage;

                            for (Player recipient : groupMembers) {
                                if (!recipient.isOnline()) continue;

                                Component finalRendered = event.renderer().render(sender, sender.displayName(), finalText, recipient);
                                liveChatModule.sendSystemMessage(recipient, finalRendered);
                            }
                        });
            });

        }).exceptionally(ex -> {
            System.err.println("Fehler beim Chat-Routing: " + ex.getMessage());
            return null;
        });
    }
}