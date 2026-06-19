package org.pytenix.pluginmessage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.player.PlayerUnregisterChannelEvent;
import org.pytenix.SpigotTranslator;
import org.pytenix.pluginmessage.consumer.ConfigUpdateConsumer;
import org.pytenix.pluginmessage.listener.ConfigUpdateListener;
import org.pytenix.packets.Packets;
import org.pytenix.proto.generated.NetworkPackets;
import org.pytenix.util.UuidUtil;
import org.transport.TransportOptions;
import org.transport.TransportService;
import org.transport.io.minecraft.PluginMessageReceiver;
import org.transport.io.minecraft.PluginMessageSender;
import org.transport.service.impl.DefaultPacketService;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

public class SpigotTransport implements Listener {

    public final Cache<UUID, List<CompletableFuture<String>>> pendingRequests = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofSeconds(20))
            .build();
    public final Cache<DeduplicationKey, UUID> deduplicationRequests = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofSeconds(20))
            .build();
    @Getter
    final TransportService<String> transportService;
    private final SpigotTranslator plugin;


    @Setter
    public boolean hasConfiguration;

    public String pluginMessagingChannel;

    public Set<UUID> availableCarriers;

    public SpigotTransport(SpigotTranslator plugin, String secret, String pluginMessagingChannel) {

        this.pluginMessagingChannel = pluginMessagingChannel;
        this.plugin = plugin;
        this.hasConfiguration = false;
        this.availableCarriers = new HashSet<>();

        plugin.getTranslatorService().getEventService().register(new ConfigUpdateListener(plugin));



        //TODO IMPLEMENT VELOCITY SECRET FROM CFG!
        this.transportService = TransportService.<String>builder()
                .packetService(new DefaultPacketService<>())
                .secret(secret)
                .encryptionEnabled(true)
                .options(
                        TransportOptions.builder()
                                .batchingEnabled(true)
                                .maxBatchSize(100)
                                .batchingIntervalMs(5)
                                .maxPayloadSize(20000)
                                .build()
                )
                .networkSender((PluginMessageSender<String>) (s, bytes) -> {

                    if(availableCarriers.isEmpty())
                        return;

                    Player carrier = Bukkit.getPlayer(availableCarriers.stream().findAny().get());

                    if (carrier == null)
                        return;

                    carrier.sendPluginMessage(plugin, s, bytes);
                })
                .build();

        PluginMessageReceiver<String> receiver = PluginMessageReceiver.zeroCopyBridge(transportService);


        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, pluginMessagingChannel);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, pluginMessagingChannel,
                (ch, player, msg) -> {


                    if (ch.equalsIgnoreCase(pluginMessagingChannel)) {
                        transportService.ready(ch);

                        receiver.handle(ch, msg);

                    }

                });


        transportService.connect(pluginMessagingChannel);


        Bukkit.getPluginManager().registerEvents(this, plugin);


        this.transportService.registerPacket(Packets.CONFIG_REQUEST, (stringPacketContext, configRequestPacket) -> {});
        this.transportService.registerPacket(Packets.TRANSLATION_REQUEST, (stringPacketContext, translationRequest) -> {});

        this.transportService.registerPacket(Packets.TRANSLATION_RESULT, (stringPacketContext, translationResult) -> {

            UUID id = UuidUtil.fromByteString(translationResult.getRequestId());
            String result = translationResult.getResult();

            List<CompletableFuture<String>> futures = pendingRequests.getIfPresent(id);
            if (futures != null) {
                for (CompletableFuture<String> future : futures) {
                    future.complete(result);
                }
            }

            pendingRequests.invalidate(id);


        });

        this.transportService.registerPacket(Packets.SERVER_CONFIG, new ConfigUpdateConsumer(plugin, plugin.getTranslatorService()));

    }

    public CompletableFuture<String> translate(UUID id, String text, String targetLang, String module) {
        if (text == null || text.isEmpty()) return CompletableFuture.completedFuture("");

        //TODO DEDUPLICATION BEVOR DEN GANZEN PLACEHOLDER....
        DeduplicationKey key = new DeduplicationKey(text, targetLang, module);
        CompletableFuture<String> future = new CompletableFuture<>();
        future.orTimeout(15, TimeUnit.SECONDS).exceptionally(ex -> text);

        boolean[] isNewRequest = new boolean[1];

        UUID actualRequestId = deduplicationRequests.get(key, k -> {
            isNewRequest[0] = true;
            return id;
        });

        pendingRequests.get(actualRequestId, k -> new CopyOnWriteArrayList<>()).add(future);

        if (isNewRequest[0]) {
            NetworkPackets.TranslationRequest req = NetworkPackets.TranslationRequest.newBuilder()
                    .setRequestId(UuidUtil.toByteString(actualRequestId))
                    .setText(text)
                    .setTargetLang(targetLang)
                    .setModule(module)
                    .build();

            transportService.send(pluginMessagingChannel, Packets.TRANSLATION_REQUEST, req);
        }

        return future;
    }

    @EventHandler
    public void onChannelRegister(PlayerRegisterChannelEvent event) {
        if (event.getChannel().equalsIgnoreCase(pluginMessagingChannel)) {
            String channel = event.getChannel();

            System.out.println("CHANNEL REGISTERED!" + pluginMessagingChannel);



            transportService.ready(channel);

            availableCarriers.add(event.getPlayer().getUniqueId());

            if (!hasConfiguration)
                System.out.println(transportService.send(channel, Packets.CONFIG_REQUEST, NetworkPackets.ConfigRequestPacket.newBuilder()
                        .setTimestamp(System.currentTimeMillis())
                        .build()));


        }
    }
    @EventHandler
    public void onChannelUnregister(PlayerUnregisterChannelEvent event) {
        if (event.getChannel().equals(pluginMessagingChannel)) {

            String channel = event.getChannel();

            availableCarriers.remove(event.getPlayer().getUniqueId());

            if(availableCarriers.isEmpty())
            {
                transportService.disconnect(channel);
                transportService.connect(channel);
            }




        }
    }



    public record DeduplicationKey(String text, String lang, String module) {
    }
}
