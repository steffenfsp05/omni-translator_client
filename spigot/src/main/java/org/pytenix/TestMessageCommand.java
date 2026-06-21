package org.pytenix;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TestMessageCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Nur für Spieler!");
            return true;
        }

        Player player = (Player) sender;
        net.kyori.adventure.text.minimessage.MiniMessage mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();

        // Der Root-Node, an den wir alles anhängen
        Component doomsdayMessage = Component.empty();

        // ==========================================================
        // 1. PREFIX: 3-Farben-Gradient + Regex-Zerstörer Emojis
        // ==========================================================
        Component prefix = mm.deserialize("<gradient:#ff0000:#00ff00:#0000ff><bold><italic>[ ☢ SYSTEM OVERLOAD ☢ ]</italic></bold></gradient>")
                .hoverEvent(HoverEvent.showText(
                        mm.deserialize("<gradient:#ffffff:#000000>VERSTECKTE MATRIX-DATEN...</gradient>\n<red>Error 404: <i>Sinnhaftigkeit nicht gefunden</i></red>")
                ))
                .clickEvent(ClickEvent.runCommand("/sudo @a explode"));

        doomsdayMessage = doomsdayMessage.append(prefix).append(Component.newline());

        // ==========================================================
        // 2. EVENT-SPAM LOOP: 20 Events direkt nebeneinander!
        // ==========================================================
        doomsdayMessage = doomsdayMessage.append(Component.text("Inventar-Scan: ").color(NamedTextColor.GRAY));
        for (int i = 1; i <= 10; i++) {
            double fakeValue = i * 3.14159; // Fiese Dezimalzahl für die LLM
            doomsdayMessage = doomsdayMessage.append(
                    Component.text("[" + i + "]")
                            // Jedes Item hat eine komplett andere Farbe
                            .color(net.kyori.adventure.text.format.TextColor.color(i * 25, 255 - (i * 10), 200))
                            .hoverEvent(HoverEvent.showText(
                                    Component.text("Slot ID: " + i + "\n").color(NamedTextColor.GRAY)
                                            .append(Component.text("Wert: " + fakeValue + " XP").color(NamedTextColor.YELLOW))
                            ))
                            .clickEvent(ClickEvent.suggestCommand("/trade add slot_" + i))
            ).append(Component.space());
        }
        doomsdayMessage = doomsdayMessage.append(Component.newline());

        // ==========================================================
        // 3. PHANTOM-NODES (Test für unser "injectTags" Update)
        // ==========================================================
        Component phantom1 = Component.empty()
                .hoverEvent(HoverEvent.showText(Component.text("👻 Buh! Ich bin ein Ghost-Event!")))
                .append(Component.text("<Geist A>").color(NamedTextColor.DARK_GRAY));

        Component phantom2 = Component.text("")
                .clickEvent(ClickEvent.copyToClipboard("SECRET_GHOST_KEY_XYZ"))
                .append(mm.deserialize("<gradient:#ff00aa:#aa00ff> [Kopiere Ghost-Daten] </gradient>"));

        doomsdayMessage = doomsdayMessage.append(Component.text("Phantome: ").color(NamedTextColor.WHITE))
                .append(phantom1).append(Component.space()).append(phantom2).append(Component.newline());

        // ==========================================================
        // 4. REGEX- & ZAHLEN-ZERSTÖRER
        // ==========================================================
        Component edgeCases = Component.text("Netzwerk-Daten: ")
                .color(NamedTextColor.GRAY)
                .append(Component.text("9,999.99$").color(NamedTextColor.GOLD)
                        .hoverEvent(HoverEvent.showText(Component.text("Steuersatz: 19.5%"))))
                .append(Component.text(" | IP: "))
                .append(Component.text("192.168.178.1").color(NamedTextColor.RED)
                .clickEvent(ClickEvent.openUrl("https://admin.router.net/login?token=xyz123&user=%3Cadmin%3E&auth=true")))
                .append(Component.text(" | Datum: "))
                .append(Component.text("25.12.2026").color(NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.runCommand("/calendar view 2026-12-25")));

        doomsdayMessage = doomsdayMessage.append(edgeCases).append(Component.newline());

        // ==========================================================
        // 5. DEEP-INCEPTION (Level 5 Verschachtelung)
        // ==========================================================
        Component deepNode = Component.text(">> Deep Nesting Test: ").color(NamedTextColor.DARK_GRAY)
                .append(Component.text("Level 1 -> ")
                        .color(NamedTextColor.WHITE)
                        .append(Component.text("Level 2 -> ")
                                .color(NamedTextColor.YELLOW)
                                .hoverEvent(HoverEvent.showText(Component.text("Hover auf Level 2!")))
                                .append(Component.text("Level 3 -> ")
                                        .color(NamedTextColor.GOLD)
                                        .clickEvent(ClickEvent.runCommand("/level 3"))
                                        .append(mm.deserialize("<gradient:#00d2ff:#3a7bd5>Level 4 (Gradient) -> </gradient>")
                                                .hoverEvent(HoverEvent.showText(Component.text("Hover auf Level 4 Gradient!")))
                                                .append(Component.text("[LEVEL 5 KERN]")
                                                        .color(NamedTextColor.RED)
                                                        .decorate(TextDecoration.BOLD, TextDecoration.OBFUSCATED)
                                                        .clickEvent(ClickEvent.copyToClipboard("LEVEL_5_CRACKED"))
                                                )
                                        )
                                )
                        )
                );

        doomsdayMessage = doomsdayMessage.append(deepNode);

        // 🔥 FEUER FREI FÜR DEN DOOMSDAY-NODE 🔥
        player.sendMessage(doomsdayMessage);

        // ==========================================================
        // 6. FULL-LINE BYPASS STRESSTEST (Rainbow)
        // ==========================================================
        Component rainbowBypass = mm.deserialize("<rainbow><bold>================== SYSTEM WURDE ERFOLGREICH KOMPROMITTIERT ==================</bold></rainbow>");
        player.sendMessage(rainbowBypass);

        return true;
    }
}