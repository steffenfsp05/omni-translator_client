package org.pytenix.module.gui.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Singleton;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Singleton
public class ItemStackCache {

    private final Cache<String, CompletableFuture<ItemStack>> cache = Caffeine.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    public CompletableFuture<ItemStack> getOrCompute(ItemStack item, String locale, Supplier<CompletableFuture<ItemStack>> computeFunction) {
        if (item == null || !item.hasItemMeta()) {
            return computeFunction.get();
        }

        String cacheKey = generateKey(item, locale);

        return cache.get(cacheKey, k -> computeFunction.get().thenApply(translated -> {
            return translated != null ? translated.clone() : null;
        })).thenApply(cachedItem -> {
            if (cachedItem == null) return null;
            ItemStack finalItem = cachedItem.clone();
            finalItem.setAmount(item.getAmount());
            return finalItem;
        });
    }

    private String generateKey(ItemStack item, String locale) {
        int metaHash = item.hasItemMeta() ? item.getItemMeta().hashCode() : 0;
        return locale + ":" + item.getType().name() + ":" + metaHash;
    }
}