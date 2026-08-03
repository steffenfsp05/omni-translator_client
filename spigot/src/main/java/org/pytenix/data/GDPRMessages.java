package org.pytenix.data;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Map;

public class GDPRMessages {

    public static final TextColor ACCENT = TextColor.color(0x63C9A6);
    public static final TextColor LINK = TextColor.color(0x7AA2FF);
    public static final TextColor MUTED = TextColor.color(0xA0A6B8);
    public static final TextColor HEADER = TextColor.color(0xD8DCE8);
    public static final TextColor WARN = TextColor.color(0xE0B34D);
    public static final TextColor SUCCESS = TextColor.color(0x63C9A6);
    public static final TextColor SKIP = TextColor.color(0xC98A4B);

    private static final Map<String, Map<String, String>> MESSAGES = Map.of(
            "de", Map.ofEntries(
                    Map.entry("title_sub", " · Datenschutzeinstellungen"),
                    Map.entry("trans_header", "ÜBERSETZUNG"),
                    Map.entry("trans_desc", "Übersetzt Chat, Schilder, Scoreboards & GUI-Text automatisch über externe KI (z.B. Gemini, ChatGPT), soweit möglich anonymisiert."),
                    Map.entry("details", "Details: "),
                    Map.entry("analytics_header", "ANALYTIK (optional)"),
                    Map.entry("analytics_desc", "Anonyme Nutzungsstatistiken, damit der Serverbesitzer sieht, ob sich Übersetzungen lohnen. Niemals mit dir verknüpft."),
                    Map.entry("change_anytime", "Jederzeit änderbar mit /omni."),
                    Map.entry("trans_checkbox", "Chat-Übersetzungen erlauben"),
                    Map.entry("analytics_checkbox", "Anonymes Nutzungs-Tracking erlauben"),
                    Map.entry("save_btn", "Speichern & Weiter"),
                    Map.entry("skip_btn", "Überspringen"),
                    Map.entry("accept_all_btn", "Alles akzeptieren"),
                    Map.entry("link_hover", "Link kann erst im Spiel (mit /omni) angeklickt werden."),
                    Map.entry("open_url", "Klicke, um die Seite zu öffnen"),
                    Map.entry("chat_privacy_info", "Unsere Richtlinien & Tracking-Details:"),
                    Map.entry("chat_privacy_link", "[Datenschutzerklärung]"),
                    Map.entry("chat_tracking_link", "[Tracking-Details]")
            ),
            "es", Map.ofEntries(
                    Map.entry("title_sub", " · Configuración de privacidad"),
                    Map.entry("trans_header", "TRADUCCIÓN"),
                    Map.entry("trans_desc", "Traduce automáticamente el chat, letreros, marcadores y texto de GUI mediante IA externa (ej. Gemini, ChatGPT), anonimizado cuando sea posible."),
                    Map.entry("details", "Detalles: "),
                    Map.entry("analytics_header", "ANÁLISIS (opcional)"),
                    Map.entry("analytics_desc", "Estadísticas de uso anónimas para que el propietario vea si la traducción vale la pena. Nunca vinculadas a ti."),
                    Map.entry("change_anytime", "Cámbialo en cualquier momento con /omni."),
                    Map.entry("trans_checkbox", "Permitir traducción de chat"),
                    Map.entry("analytics_checkbox", "Permitir análisis de uso anónimo"),
                    Map.entry("save_btn", "Guardar y Continuar"),
                    Map.entry("skip_btn", "Omitir"),
                    Map.entry("accept_all_btn", "Aceptar todo"),
                    Map.entry("link_hover", "El enlace solo se puede hacer clic en el juego (usando /omni)."),
                    Map.entry("open_url", "Haz clic para abrir la página"),
                    Map.entry("chat_privacy_info", "Nuestras políticas y detalles de seguimiento:"),
                    Map.entry("chat_privacy_link", "[Política de privacidad]"),
                    Map.entry("chat_tracking_link", "[Detalles de seguimiento]")
            ),
            "fr", Map.ofEntries(
                    Map.entry("title_sub", " · Paramètres de confidentialité"),
                    Map.entry("trans_header", "TRADUCTION"),
                    Map.entry("trans_desc", "Traduit automatiquement le chat, les panneaux, les tableaux de bord et le texte GUI via une IA externe (ex. Gemini, ChatGPT), anonymisé si possible."),
                    Map.entry("details", "Détails : "),
                    Map.entry("analytics_header", "ANALYSES (optionnel)"),
                    Map.entry("analytics_desc", "Statistiques d'utilisation anonymes pour que le propriétaire voie si la traduction en vaut la peine. Jamais liées à vous."),
                    Map.entry("change_anytime", "Modifiable à tout moment avec /omni."),
                    Map.entry("trans_checkbox", "Autoriser la traduction du chat"),
                    Map.entry("analytics_checkbox", "Autoriser le suivi d'utilisation anonyme"),
                    Map.entry("save_btn", "Enregistrer & Continuer"),
                    Map.entry("skip_btn", "Passer"),
                    Map.entry("accept_all_btn", "Tout accepter"),
                    Map.entry("link_hover", "Le lien ne peut être cliqué qu'en jeu (avec /omni)."),
                    Map.entry("open_url", "Cliquez pour ouvrir la page"),
                    Map.entry("chat_privacy_info", "Nos politiques et détails de suivi :"),
                    Map.entry("chat_privacy_link", "[Politique de confidentialité]"),
                    Map.entry("chat_tracking_link", "[Détails de suivi]")
            ),
            "pt", Map.ofEntries(
                    Map.entry("title_sub", " · Configurações de privacidade"),
                    Map.entry("trans_header", "TRADUÇÃO"),
                    Map.entry("trans_desc", "Traduz automaticamente chat, placas, placas e texto de GUI via IA externa (ex: Gemini, ChatGPT), anonimizado quando possível."),
                    Map.entry("details", "Detalhes: "),
                    Map.entry("analytics_header", "ANÁLITICOS (opcional)"),
                    Map.entry("analytics_desc", "Estatísticas de uso anônimas para o dono ver se a tradução vale a pena. Nunca vinculadas a você."),
                    Map.entry("change_anytime", "Altere a qualquer momento com /omni."),
                    Map.entry("trans_checkbox", "Permitir tradução de chat"),
                    Map.entry("analytics_checkbox", "Permitir rastreamento de uso anônimo"),
                    Map.entry("save_btn", "Salvar & Continuar"),
                    Map.entry("skip_btn", "Pular"),
                    Map.entry("accept_all_btn", "Aceitar tudo"),
                    Map.entry("link_hover", "O link só pode ser clicado no jogo (usando /omni)."),
                    Map.entry("open_url", "Clique para abrir a página"),
                    Map.entry("chat_privacy_info", "Nossas políticas e detalhes de rastreamento:"),
                    Map.entry("chat_privacy_link", "[Política de Privacidade]"),
                    Map.entry("chat_tracking_link", "[Detalhes de Rastreamento]")
            ),
            "pl", Map.ofEntries(
                    Map.entry("title_sub", " · Ustawienia prywatności"),
                    Map.entry("trans_header", "TŁUMACZENIE"),
                    Map.entry("trans_desc", "Automatycznie tłumaczy czat, tabliczki, scoreboardy i tekst GUI przez zewnętrzne AI (np. Gemini, ChatGPT), w miarę możliwości anonimowo."),
                    Map.entry("details", "Szczegóły: "),
                    Map.entry("analytics_header", "ANALYTIKA (opcjonalnie)"),
                    Map.entry("analytics_desc", "Anonimowe statystyki użycia, aby właściciel widział, czy tłumaczenie się opłaca. Nigdy nie powiązane z Tobą."),
                    Map.entry("change_anytime", "Zmień w każdej chwili za pomocą /omni."),
                    Map.entry("trans_checkbox", "Zezwól na tłumaczenie czatu"),
                    Map.entry("analytics_checkbox", "Zezwól na anonimowe śledzenie użycia"),
                    Map.entry("save_btn", "Zapisz i kontynuuj"),
                    Map.entry("skip_btn", "Pomiń"),
                    Map.entry("accept_all_btn", "Zaakceptuj wszystko"),
                    Map.entry("link_hover", "Link można kliknąć tylko w grze (używając /omni)."),
                    Map.entry("open_url", "Kliknij, aby otworzyć stronę"),
                    Map.entry("chat_privacy_info", "Nasza polityka i szczegóły śledzenia:"),
                    Map.entry("chat_privacy_link", "[Polityka prywatności]"),
                    Map.entry("chat_tracking_link", "[Szczegóły śledzenia]")
            ),
            "ru", Map.ofEntries(
                    Map.entry("title_sub", " · Настройки конфиденциальности"),
                    Map.entry("trans_header", "ПЕРЕВОД"),
                    Map.entry("trans_desc", "Автоматический перевод чата, табличек, скорбордов и текстов GUI через внешний ИИ (например, Gemini, ChatGPT) с максимальной анонимизацией."),
                    Map.entry("details", "Подробнее: "),
                    Map.entry("analytics_header", "АНАЛИТИКА (опционально)"),
                    Map.entry("analytics_desc", "Анонимная статистика использования, чтобы владелец видел, востребован ли перевод. Никогда не привязывается к вам."),
                    Map.entry("change_anytime", "Изменить в любое время через /omni."),
                    Map.entry("trans_checkbox", "Разрешить перевод чата"),
                    Map.entry("analytics_checkbox", "Разрешить анонимную аналитику"),
                    Map.entry("save_btn", "Сохранить и продолжить"),
                    Map.entry("skip_btn", "Пропустить"),
                    Map.entry("accept_all_btn", "Принять все"),
                    Map.entry("link_hover", "Ссылку можно нажать только в игре (через /omni)."),
                    Map.entry("open_url", "Нажмите, чтобы открыть страницу"),
                    Map.entry("chat_privacy_info", "Наша политика и детали отслеживания:"),
                    Map.entry("chat_privacy_link", "[Политика конфиденциальности]"),
                    Map.entry("chat_tracking_link", "[Детали отслеживания]")
            )
    );

    private static final Map<String, String> EN = Map.ofEntries(
            Map.entry("title_sub", " · Privacy Settings"),
            Map.entry("trans_header", "TRANSLATION"),
            Map.entry("trans_desc", "Auto-translates chat, signs, scoreboards & item/GUI text via third-party AI (e.g. Gemini, ChatGPT), anonymized where possible."),
            Map.entry("details", "Details: "),
            Map.entry("analytics_header", "ANALYTICS (optional)"),
            Map.entry("analytics_desc", "Anonymous, pseudonymous usage stats so the owner can see if translation is worth it. Never linked back to you."),
            Map.entry("change_anytime", "Change anytime with /omni."),
            Map.entry("trans_checkbox", "Enable chat translation"),
            Map.entry("analytics_checkbox", "Enable anonymous usage analytics"),
            Map.entry("save_btn", "Save & Continue"),
            Map.entry("skip_btn", "Skip"),
            Map.entry("accept_all_btn", "Accept All"),
            Map.entry("link_hover", "Link can only be clicked in-game (using /omni)."),
            Map.entry("open_url", "Click to open the page"),
            Map.entry("chat_privacy_info", "Our privacy policies & tracking details:"),
            Map.entry("chat_privacy_link", "[Privacy Policy]"),
            Map.entry("chat_tracking_link", "[Tracking Details]")
    );

    public static String get(String lang, String key) {
        return MESSAGES.getOrDefault(lang, EN).getOrDefault(key, EN.get(key));
    }

    public static Component sectionLabel(String text) {
        return Component.text(text, Style.style(HEADER, TextDecoration.BOLD));
    }


    public static Component safeDetailsLine(String lang, String path) {
        return Component.text(get(lang, "details"), MUTED)
                .append(Component.text(path, Style.style(LINK, TextDecoration.UNDERLINED))
                        .hoverEvent(HoverEvent.showText(Component.text(get(lang, "link_hover"), MUTED))));
    }


    public static Component getChatLinksMessage(String lang) {
        Component prefix = Component.text("Omni-Translator | ", Style.style(ACCENT, TextDecoration.BOLD));

        Component info = Component.text(get(lang, "chat_privacy_info") + " ", MUTED);

        Component privacyLink = Component.text(get(lang, "chat_privacy_link"), Style.style(LINK, TextDecoration.BOLD))
                .clickEvent(ClickEvent.openUrl("https://omni-translator.com/privacy"))
                .hoverEvent(HoverEvent.showText(Component.text(get(lang, "open_url"), MUTED)));

        Component separator = Component.text(" · ", MUTED);

        Component trackingLink = Component.text(get(lang, "chat_tracking_link"), Style.style(LINK, TextDecoration.BOLD))
                .clickEvent(ClickEvent.openUrl("https://omni-translator.com/tracking"))
                .hoverEvent(HoverEvent.showText(Component.text(get(lang, "open_url"), MUTED)));

        return prefix.append(info).append(privacyLink).append(separator).append(trackingLink);
    }
}