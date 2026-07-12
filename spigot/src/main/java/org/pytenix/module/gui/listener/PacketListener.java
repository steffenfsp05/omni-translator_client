package org.pytenix.module.gui.listener;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import com.google.inject.Inject;
import com.google.inject.Provider;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.pytenix.module.gui.InventoryModule;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PacketListener implements com.github.retrooper.packetevents.event.PacketListener {

    private final Provider<InventoryModule> inventoryModuleProvider;

    private final Set<String> activeTranslations = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> latestStateIdMap = new ConcurrentHashMap<>();

    @Inject
    public PacketListener(Provider<InventoryModule> inventoryModuleProvider) {
        this.inventoryModuleProvider = inventoryModuleProvider;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.isCancelled()) return;

        InventoryModule inventoryModule = inventoryModuleProvider.get();
        if (!inventoryModule.isActive()) return;

        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            inventoryModule.requiresTranslation(event.getUser().getUUID()).thenAccept(requiresTranslation -> {
                if (!requiresTranslation) return;
                handleWindowItems(event, inventoryModule);
            });
        } else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            inventoryModule.requiresTranslation(event.getUser().getUUID()).thenAccept(requiresTranslation -> {
                if (!requiresTranslation) return;
                handleSetSlot(event, inventoryModule);
            });
        }
    }

    public void handleWindowItems(PacketSendEvent event, InventoryModule inventoryModule) {
        WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
        latestStateIdMap.put(event.getUser().getUUID(), wrapper.getStateId());

        if (wrapper.getWindowId() == 0) return;

        com.github.retrooper.packetevents.protocol.player.User user = event.getUser();
        if (user == null) return;

        String lockKey = user.getUUID().toString() + ":" + wrapper.getWindowId() + ":" + wrapper.getStateId();

        if (activeTranslations.contains(lockKey)) return;

        activeTranslations.add(lockKey);

        Player player = Bukkit.getPlayer(user.getUUID());
        if (player == null) return;

        try {
            int windowId = wrapper.getWindowId();
            int stateId = wrapper.getStateId();

            List<ItemStack> bukkitItems = wrapper.getItems().stream()
                    .map(SpigotConversionUtil::toBukkitItemStack)
                    .collect(Collectors.toList());

            ItemStack carriedItem = wrapper.getCarriedItem()
                    .map(SpigotConversionUtil::toBukkitItemStack)
                    .orElse(null);

            String locale = inventoryModule.getPlayerLocaleProcessor().retrieveLocale(player.getUniqueId());

            CompletableFuture.runAsync(() -> {
                try {
                    translateAndSendUpdate(player, windowId, stateId, bukkitItems, carriedItem, locale, inventoryModule);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.out.println("Fehler beim Lesen des Pakets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleSetSlot(PacketSendEvent event, InventoryModule inventoryModule) {
        WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);

        if (wrapper.getWindowId() == 0) return;

        com.github.retrooper.packetevents.protocol.player.User user = event.getUser();
        if (user == null) return;

        Player player = org.bukkit.Bukkit.getPlayer(user.getUUID());
        if (player == null) return;

        event.setCancelled(true);

        int windowId = wrapper.getWindowId();
        int stateId = wrapper.getStateId();
        int slot = wrapper.getSlot();
        final ItemStack item = SpigotConversionUtil.toBukkitItemStack(wrapper.getItem()).clone();
        String locale = inventoryModule.getPlayerLocaleProcessor().retrieveLocale(player.getUniqueId());

        inventoryModule.translateItem(item, locale).thenAccept(translatedItem -> {
            if (!player.isOnline()) return;

            Integer lastId = latestStateIdMap.get(player.getUniqueId());
            if (lastId != null && stateId < lastId) {
                if (player.getOpenInventory() == null || player.getOpenInventory().getTopInventory() == null ||
                        !player.getOpenInventory().getTopInventory().getItem(slot).isSimilar(item)) {
                    return;
                }
            }

            WrapperPlayServerSetSlot updatePacket = new WrapperPlayServerSetSlot(
                    windowId,
                    stateId,
                    slot,
                    SpigotConversionUtil.fromBukkitItemStack(translatedItem)
            );

            PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, updatePacket);
        });
    }

    private void sendUpdateToClient(Player player, int windowId, int stateId, List<ItemStack> items, ItemStack carriedItem) {
        List<com.github.retrooper.packetevents.protocol.item.ItemStack> peItems = items.stream()
                .map(SpigotConversionUtil::fromBukkitItemStack)
                .collect(Collectors.toList());

        com.github.retrooper.packetevents.protocol.item.ItemStack peCarriedItem =
                SpigotConversionUtil.fromBukkitItemStack(carriedItem);

        WrapperPlayServerWindowItems updatePacket = new WrapperPlayServerWindowItems(
                windowId,
                stateId,
                peItems,
                peCarriedItem
        );

        Integer lastId = latestStateIdMap.get(player.getUniqueId());
        if (lastId != null && stateId < lastId) return;

        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, updatePacket);
    }

    private void translateAndSendUpdate(Player player, int windowId, int stateId, List<ItemStack> items, ItemStack carriedItem, String locale, InventoryModule inventoryModule) {
        int topInventorySize = items.size() - 36;
        List<ItemStack> batchToTranslate = new ArrayList<>();

        for (int i = 0; i < topInventorySize; i++) {
            ItemStack item = items.get(i);
            if (item != null && item.hasItemMeta() && (item.getItemMeta().displayName() != null || item.getItemMeta().lore() != null)) {
                batchToTranslate.add(item);
            } else {
                batchToTranslate.add(null);
            }
        }
        batchToTranslate.add(carriedItem);

        inventoryModule.translateInventoryBatch(batchToTranslate, locale).thenAccept(translatedBatch -> {
            if (!player.isOnline()) return;

            ItemStack translatedCarried = translatedBatch.get(translatedBatch.size() - 1);
            List<ItemStack> translatedItems = new ArrayList<>();

            for (int i = 0; i < topInventorySize; i++) {
                ItemStack original = items.get(i);
                ItemStack translated = translatedBatch.get(i);
                translatedItems.add(translated != null ? translated : original);
            }

            Integer lastId = latestStateIdMap.get(player.getUniqueId());
            if (lastId != null && stateId < lastId) {
                activeTranslations.remove(player.getUniqueId() + ":" + windowId + ":" + stateId);
                return;
            }

            sendUpdateToClient(player, windowId, stateId, translatedItems, translatedCarried);
            activeTranslations.remove(player.getUniqueId() + ":" + windowId + ":" + stateId);
        });
    }
}