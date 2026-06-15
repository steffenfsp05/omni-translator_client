package org.pytenix;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.pytenix.util.CaffeineCache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PlayerLocaleService {



    // FOR TESTING

    static Cache<UUID,String> localeCache = CacheBuilder.newBuilder()
            .build();

            static ArrayList<String> topMinecraftLanguages = new ArrayList<>(Arrays.asList(
                    "en_us", // Englisch (US)
                    "en_gb", // Englisch (UK)
                    "de_de", // Deutsch (Deutschland)
                    "es_es", // Spanisch (Spanien)
                    "es_mx", // Spanisch (Mexiko)
                    "fr_fr", // Französisch (Frankreich)
                    "fr_ca", // Französisch (Kanada)
                    "it_it", // Italienisch
                    "ja_jp", // Japanisch
                    "ko_kr", // Koreanisch
                    "pt_br", // Portugiesisch (Brasilien)
                    "pt_pt", // Portugiesisch (Portugal)
                    "ru_ru", // Russisch
                    "zh_cn", // Chinesisch (Vereinfacht)
                    "zh_tw", // Chinesisch (Traditionell)
                    "pl_pl", // Polnisch
                    "nl_nl", // Niederländisch
                    "tr_tr", // Türkisch
                    "sv_se", // Schwedisch
                    "no_no", // Norwegisch (Bokmål)
                    "da_dk", // Dänisch
                    "fi_fi", // Finnisch
                    "cs_cz", // Tschechisch
                    "hu_hu", // Ungarisch
                    "ro_ro", // Rumänisch
                    "uk_ua", // Ukrainisch
                    "el_gr", // Griechisch
                    "bg_bg", // Bulgarisch
                    "th_th", // Thai
                    "vi_vn"  // Vietnamesisch
            ));


            final static Random random = new Random();

            public static String getPlayerLocale(UUID uuid)
            {
                String locale = localeCache.getIfPresent(uuid);
                if(locale == null)
                {
                    locale = topMinecraftLanguages.get(random.nextInt(topMinecraftLanguages.size()));
                    localeCache.put(uuid, locale);
                }
                return locale;
            }


}
