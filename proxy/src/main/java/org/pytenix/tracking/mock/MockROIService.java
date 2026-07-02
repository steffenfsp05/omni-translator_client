package org.pytenix.tracking.mock;

import lombok.Getter;
import org.pytenix.TranslatorPlugin;
import org.pytenix.packets.PacketMapperRegistry;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.packets.impl.TrackPlayerRequestMapper;
import org.pytenix.proto.generated.NetworkPackets;
import org.pytenix.tracking.ROIService;

import java.util.*;
import java.util.concurrent.*;

public class MockROIService {
    private final ROIService roiService;
    private final TranslatorPlugin translatorPlugin;
    private final Random random = new Random();


    private static final int TIME_COMPRESSION_FACTOR = 30;

    private record VirtualSession(long joinTime, long expectedDurationMs) {}

    @Getter
    private final Map<UUID, VirtualSession> virtualPlayers = new ConcurrentHashMap<>();

    public MockROIService(TranslatorPlugin translatorPlugin, ROIService roiService) {
        this.translatorPlugin = translatorPlugin;
        this.roiService = roiService;

        // HEBEL 2: Server sofort füllen

        new Thread()
        {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {


                }
                prefillServer();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {


                }
                startSimulation();

            }
        }.start();
    }

    private NetworkPackets.ProfilePacket.ConsentType getWeightedConsent() {
        double r = random.nextDouble(); // 0.0 bis 1.0

        if (r < 0.70) return NetworkPackets.ProfilePacket.ConsentType.EXPLICIT;  // 70%
        if (r < 0.85) return NetworkPackets.ProfilePacket.ConsentType.AUTO;      // 15%
        // 10%
        return NetworkPackets.ProfilePacket.ConsentType.DECLINED;                 // 5%
    }

    private void prefillServer() {
        for (UUID uuid : MockData.HARDCODED_UUIDS) {


                translatorPlugin.getProfileService().retrieveProfile(uuid).thenAccept(profileData -> {
                    // 1. Consent setzen
                    NetworkPackets.ProfilePacket.ConsentType consent = getWeightedConsent();

                    // 2. Profil aktualisieren
                    translatorPlugin.getProfileService().updateProfile(profileData.withConsentType(consent));

                });

        }
    }
    private void startSimulation() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::tick, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        long now = System.currentTimeMillis();

        // 1. Session-Check
        virtualPlayers.forEach((uuid, session) -> {
            if (now - session.joinTime() > (session.expectedDurationMs() / TIME_COMPRESSION_FACTOR)) {
                onLeave(uuid);
            }
        });

        if (virtualPlayers.size() < 3000 && random.nextDouble() > 0.20) {
            UUID randomUuid = MockData.HARDCODED_UUIDS.get(random.nextInt(MockData.HARDCODED_UUIDS.size()));
            if (!virtualPlayers.containsKey(randomUuid)) {
                onJoin(randomUuid, getRandomLocale());
            }
        }
    }

    public void onJoin(UUID uuid, String locale) {
        translatorPlugin.getProfileService().retrieveProfile(uuid).thenAccept(profileData -> {
            roiService.getLanguageCache().put(uuid, locale.toLowerCase());

            translatorPlugin.getTranslatorService().requiresTranslation(uuid).thenAccept(aBoolean -> {
                String defaultLang = translatorPlugin.getTranslatorService().getTranslationConfiguration().getDefaultLanguage().toLowerCase();
                boolean isNative = locale.startsWith(defaultLang);
                boolean isDeclined = profileData.consentType().equals(NetworkPackets.ProfilePacket.ConsentType.DECLINED);
                boolean isExplicitOrAuto = (profileData.consentType().equals(NetworkPackets.ProfilePacket.ConsentType.EXPLICIT) ||
                        profileData.consentType().equals(NetworkPackets.ProfilePacket.ConsentType.AUTO));

                long duration = calculateDuration(isDeclined, isExplicitOrAuto, isNative);

                long simulatedStart = System.currentTimeMillis() - random.nextInt(10000);

                virtualPlayers.put(uuid, new VirtualSession(simulatedStart, duration));
                roiService.initTrackingProcess(uuid);
            });
        });
    }

    private long calculateDuration(boolean isDeclined, boolean isExplicitOrAuto, boolean isNative) {
        double prob = isDeclined ? (isNative ? 0.50 : 0.90) : (isExplicitOrAuto ? 0.40 : 0.70);

        long ms = (random.nextDouble() < prob)
                ? (30 + random.nextInt(60)) * 1000L      // Kurz
                : (5 + random.nextInt(10)) * 60 * 1000L; // Lang

        return ms ;
    }

    public void onLeave(UUID uuid) {
        if (virtualPlayers.containsKey(uuid)) {
            stopTrackingProcess(uuid);
            virtualPlayers.remove(uuid);
        }
    }

    public static String getRandomLocale() {
        String[] locales = {
                // --- HAUPTSPRACHEN (~45% Verteilung) ---
                "en_US", "en_US", "en_US", "en_US", "en_GB", "en_GB",
                "de_DE", "de_DE", "de_DE", "de_AT", "ch_DE",

                // --- EUROPA (WEST & SÜD) (~25% Verteilung) ---
                "fr_FR", "fr_FR", "es_ES", "es_ES", "it_IT", "nl_NL",

                // --- AMERIKA (LATAM) (~15% Verteilung) ---
                "pt_BR", "pt_BR", "es_MX", "es_AR",

                // --- OSTEUROPA & ASIEN (~15% Verteilung) ---
                "ru_RU", "ru_RU", "pl_PL", "tr_TR", "zh_CN", "zh_TW", "ja_JP", "ko_KR"
        };

        return locales[new Random().nextInt(locales.length)];
    }
    public void stopTrackingProcess(UUID uuid) {

            int playtimeInSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(virtualPlayers.get(uuid).expectedDurationMs());

            translatorPlugin.getTranslatorService().requiresTranslation(uuid).thenAccept(requiresTranslation ->
            {
                roiService.getOmniConnectionService().sendPacket(PacketRegistry.TRACK_PLAYER, PacketMapperRegistry.toProto(
                        new TrackPlayerRequestMapper.TrackData(
                                translatorPlugin.getConfigurationFile().getLicenseKey(),
                                UUID.randomUUID(),
                                uuid,
                                System.currentTimeMillis(),
                                playtimeInSeconds,
                                requiresTranslation,
                                translatorPlugin.getPlayerLocaleProcessor().retrieveLocale(uuid)
                        )
                ));
            });
        }
    }
