package org.pytenix.data;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.DialogKeys;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

public class GDPRBootstrap implements PluginBootstrap {

    private static final List<String> SUPPORTED_LANGS = List.of("en", "de", "es", "fr", "pt", "pl", "ru");

    private static final TextColor ACCENT = TextColor.color(0x63C9A6);
    private static final TextColor LINK = TextColor.color(0x7AA2FF);
    private static final TextColor MUTED = TextColor.color(0xA0A6B8);
    private static final TextColor HEADER = TextColor.color(0xD8DCE8);
    private static final TextColor WARN = TextColor.color(0xE0B34D);
    private static final TextColor SUCCESS = TextColor.color(0x63C9A6);

    private static Component sectionLabel(String text) {
        return Component.text(text, Style.style(HEADER, TextDecoration.BOLD));
    }
    public static Component detailsLine(String lang, String path, String url) {
        return Component.text(GDPRMessages.get(lang, "details"), MUTED)
                .append(Component.text(path, Style.style(LINK, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(url))
                        .hoverEvent(HoverEvent.showText(Component.text(GDPRMessages.get(lang, "open_url"), MUTED)))));
    }

    @SuppressWarnings("all")
    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(RegistryEvents.DIALOG.compose(), event -> {
            for (String lang : SUPPORTED_LANGS) {

                List<ActionButton> actionButtons = new ArrayList<>();
                actionButtons.add(ActionButton.builder(Component.text(GDPRMessages.get(lang, "skip_btn"), Style.style(NamedTextColor.GRAY)))
                                .action(DialogAction.customClick(Key.key("omni:gdpr/skip"), null))
                                .build());

                actionButtons.add(ActionButton.builder(Component.text(GDPRMessages.get(lang, "save_btn"), Style.style(SUCCESS)))
                                .action(DialogAction.customClick(Key.key("omni:gdpr/submit"), null))
                                .build());

                actionButtons.add(ActionButton.builder(Component.text(GDPRMessages.get(lang, "accept_all_btn"), Style.style(SUCCESS, TextDecoration.BOLD)))
                                .action(DialogAction.customClick(Key.key("omni:gdpr/submit_all"), null))
                                .build());

                event.registry().register(
                        DialogKeys.create(Key.key("omni", "gdpr_dialog_" + lang)),
                        builder -> builder
                                .base(DialogBase.builder(
                                                        Component.text("Omni-Translator", Style.style(ACCENT, TextDecoration.BOLD))
                                                                .append(Component.text(GDPRMessages.get(lang, "title_sub"), MUTED))
                                                )
                                                .canCloseWithEscape(true)
                                        .externalTitle(Component.text("DSHAJDJHASHJDSAJHJDH"))
                                                .body(List.of(
                                                        DialogBody.plainMessage(sectionLabel(GDPRMessages.get(lang, "trans_header"))),
                                                        DialogBody.plainMessage(Component.text(GDPRMessages.get(lang, "trans_desc"), MUTED)),
                                                        DialogBody.plainMessage(detailsLine(lang, "omni-translator.com/privacy", "https://omni-translator.com/privacy")),
                                                        DialogBody.plainMessage(Component.empty()),

                                                        DialogBody.plainMessage(sectionLabel(GDPRMessages.get(lang, "analytics_header"))),
                                                        DialogBody.plainMessage(Component.text(GDPRMessages.get(lang, "analytics_desc"), MUTED)),
                                                        DialogBody.plainMessage(detailsLine(lang, "omni-translator.com/tracking", "https://omni-translator.com/tracking")),
                                                        DialogBody.plainMessage(Component.empty()),

                                                        DialogBody.plainMessage(Component.text(
                                                                GDPRMessages.get(lang, "change_anytime"),
                                                                Style.style(WARN, TextDecoration.ITALIC)))
                                                ))
                                                .inputs(List.of(
                                                        DialogInput.bool("accept_translation",
                                                                Component.text(GDPRMessages.get(lang, "trans_checkbox"), Style.style(NamedTextColor.WHITE))
                                                        ).build(),
                                                        DialogInput.bool("accept_tracking",
                                                                Component.text(GDPRMessages.get(lang, "analytics_checkbox"), Style.style(NamedTextColor.WHITE))
                                                        ).build()
                                                )).build()
                                )
                                .type(DialogType.multiAction(
                                        actionButtons
                                ).build())
                );
            }
        });
    }

    public static Key getDialogKey(String lang) {
        if (!SUPPORTED_LANGS.contains(lang)) {
            return Key.key("omni", "gdpr_dialog_en");
        }
        return Key.key("omni", "gdpr_dialog_" + lang);
    }
}