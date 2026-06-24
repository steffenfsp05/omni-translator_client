package org.pytenix.module.player.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.pytenix.TranslatorPlugin;
import org.pytenix.module.player.LiveChatModule;
import org.pytenix.service.PlayerLocaleService;
import org.pytenix.service.TaskScheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AsyncPlayerChatListener implements Listener {


    final LiveChatModule liveChatModule;
    final TranslatorPlugin translatorPlugin;
    final TaskScheduler taskScheduler;

    public AsyncPlayerChatListener(LiveChatModule liveChatModule, TranslatorPlugin translatorPlugin) {
        this.liveChatModule = liveChatModule;
        this.translatorPlugin = translatorPlugin;
        this.taskScheduler = translatorPlugin.getTaskScheduler();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        if (!liveChatModule.isActive()) return;

        Player sender = event.getPlayer();
        Component originalMessage = event.message();


        String senderLang = PlayerLocaleService.getPlayerLocale(sender.getUniqueId()).split("_")[0].toLowerCase();

        Map<String, List<Player>> languageGroups = new HashMap<>();

        for (Audience audience : event.viewers()) {
            if (audience instanceof Player p && !p.getUniqueId().equals(sender.getUniqueId())) {

                String targetLang = PlayerLocaleService.getPlayerLocale(p.getUniqueId()).split("_")[0].toLowerCase();

                // 2. DER KOSTEN-KILLER: Gleiche Sprache? -> Direkt senden, kein API Call!
                //HIER AMBESTEN NICHT AUF DEFAULTLANGUAGE VERLASSEN!!
                if (targetLang.equals(senderLang)) {
                    Component rendered = event.renderer().render(sender, sender.displayName(), originalMessage, p);
                    liveChatModule.sendSystemMessage(p, rendered);
                    continue;
                }

                languageGroups.computeIfAbsent(targetLang, k -> new ArrayList<>()).add(p);
            }
        }

        event.viewers().clear();
        event.viewers().add(sender);

        if (languageGroups.isEmpty()) return;

        languageGroups.forEach((targetLang, groupMembers) -> {

            translatorPlugin.getTextComponentUtil().translateComplexMessage(originalMessage, targetLang, liveChatModule.getModuleName())
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
    }


    private Component replaceContent(Component base, String original, Component translated) {

        if (translated == null) return base;

        try {
            return base.replaceText(config -> {
                config.matchLiteral(original);
                config.replacement(translated);
                config.once();
            });
        } catch (Exception e) {

            return base.append(translated);
        }
    }
         /*
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {


        if(!liveChatModule.isActive())
            return;

        ChatRendererCache.set(event.getPlayer().getUniqueId(), event.renderer());

         */
/*

if(!liveChatModule.isActive())
            return;
        Player sender = event.getPlayer();

        Set<Player> actualRecipients = Bukkit.getOnlinePlayers().stream()
                .filter(p -> event.viewers().contains(p))
                .filter(p -> !p.getUniqueId().equals(sender.getUniqueId()))
                .collect(Collectors.toSet());

        event.setCancelled(true);

        Component originalMessage = event.message();
        String rawText = translatorPlugin.getLegacyComponentSerializer().serialize(originalMessage);
        ChatRenderer currentRenderer = event.renderer();



        liveChatModule.deliverToOne(sender, currentRenderer.render(sender, sender.displayName(), originalMessage, sender));

        if (actualRecipients.isEmpty()) return;

        Map<String, List<Player>> languageGroups = actualRecipients.stream()
                .collect(Collectors.groupingBy(Player::getLocale));


        languageGroups.forEach((locale, recipients) -> liveChatModule.translate(rawText, locale)
                .orTimeout(5, TimeUnit.SECONDS)
                .handle((translatedText, ex) -> {
                     if (ex != null || translatedText == null) {
                         liveChatModule.deliver(sender, originalMessage, originalMessage, recipients, currentRenderer);
                    } else {

                         Component translated = translatorPlugin.getLegacyComponentSerializer().deserialize(translatedText);

                         liveChatModule.deliver(sender, originalMessage, translated, recipients, currentRenderer);
                    }

                    return null;
                }));


    }
 */

}
