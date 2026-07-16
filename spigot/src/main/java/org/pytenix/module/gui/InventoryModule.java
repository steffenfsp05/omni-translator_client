package org.pytenix.module.gui;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.omni.entity.TranslationModule;
import org.omni.translation.TranslatorService;
import org.omni.translation.locale.PlayerLocaleProcessor;
import org.omni.translation.module.AbstractTranslatorModule;
import org.pytenix.TranslatorPlugin;
import org.pytenix.module.gui.cache.ItemStackCache;
import org.pytenix.module.gui.listener.PacketListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
public class InventoryModule extends AbstractTranslatorModule {

    private static final Pattern COLOR_PATTERN = Pattern.compile("^§[0-9a-fk-or]+$");
    final PacketListener inventoryPacketListener;
    final ItemStackCache itemStackCache;
    private final LegacyComponentSerializer legacyComponentSerializer;

    @Inject
    public InventoryModule(
            TranslatorService translatorService,
            PlayerLocaleProcessor playerLocaleProcessor,
            PacketListener inventoryPacketListener,
            ItemStackCache itemStackCache
    ) {
        super( translatorService, playerLocaleProcessor, TranslationModule.GUI);

        this.itemStackCache = itemStackCache;
        this.legacyComponentSerializer = TranslatorPlugin.getLegacyComponentSerializer();
        this.inventoryPacketListener = inventoryPacketListener;

    }

    @Override
    public void init() {
        PacketEvents.getAPI().getEventManager().registerListener(
                inventoryPacketListener,
                PacketListenerPriority.NORMAL
        );
        System.out.println("REGISTERED EVENT!!!!!!!!!!");
    }

    public CompletableFuture<ItemStack> translateItem(ItemStack item, String targetLanguage) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta() || item.getItemMeta() == null) {
            return CompletableFuture.completedFuture(item);
        }

        return itemStackCache.getOrCompute(item, targetLanguage, () -> {

            ItemStack clonedItem = item.clone();
            ItemMeta meta = clonedItem.getItemMeta();

            List<CompletableFuture<?>> allFutures = new ArrayList<>();

            final CompletableFuture<String> nameFuture = meta.hasDisplayName()
                    ? translate(legacyComponentSerializer.serialize(meta.displayName()), targetLanguage)
                    : CompletableFuture.completedFuture(null);

            if (meta.hasDisplayName()) {
                allFutures.add(nameFuture);
            }

            List<CompletableFuture<String>> loreFutures = new ArrayList<>();

            if (meta.hasLore() && meta.lore() != null) {
                StringBuilder currentBlock = new StringBuilder();

                for (Component component : meta.lore()) {
                    String serialized = legacyComponentSerializer.serialize(component);

                    if (serialized.trim().isEmpty() || COLOR_PATTERN.matcher(serialized).matches()) {
                        if (!currentBlock.isEmpty()) {
                            CompletableFuture<String> blockFuture = translate(currentBlock.toString(), targetLanguage);
                            loreFutures.add(blockFuture);
                            allFutures.add(blockFuture);
                            currentBlock.setLength(0);
                        }
                        loreFutures.add(CompletableFuture.completedFuture(serialized));
                    } else {
                        if (!currentBlock.isEmpty()) {
                            currentBlock.append("\n");
                        }
                        currentBlock.append(serialized);
                    }
                }

                if (!currentBlock.isEmpty()) {
                    CompletableFuture<String> lastBlockFuture = translate(currentBlock.toString(), targetLanguage);
                    loreFutures.add(lastBlockFuture);
                    allFutures.add(lastBlockFuture);
                }
            }

            return CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        if (meta.hasDisplayName()) {
                            String translatedName = nameFuture.join();
                            if (translatedName != null) {
                                meta.displayName(legacyComponentSerializer.deserialize(translatedName));
                            }
                        }

                        if (!loreFutures.isEmpty()) {
                            List<Component> newLore = new ArrayList<>();
                            for (CompletableFuture<String> future : loreFutures) {
                                String result = future.join();
                                for (String line : result.split("\n")) {
                                    newLore.add(legacyComponentSerializer.deserialize(line));
                                }
                            }
                            meta.lore(newLore);
                        }

                        clonedItem.setItemMeta(meta);
                        return clonedItem;
                    });
        });
    }

    public CompletableFuture<List<ItemStack>> translateInventoryBatch(List<ItemStack> items, String targetLanguage) {
        if (items == null || items.isEmpty()) {
            return CompletableFuture.completedFuture(items);
        }

        List<CompletableFuture<ItemStack>> itemFutures = items.stream()
                .map(item -> translateItem(item, targetLanguage))
                .toList();

        return CompletableFuture.allOf(itemFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> itemFutures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
    }
}