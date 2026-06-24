package org.pytenix.network;

import org.pytenix.TranslatorPlugin;
import org.pytenix.network.service.ChannelCarrierService;
import org.transport.TransportOptions;
import org.transport.TransportService;
import org.transport.io.minecraft.PluginMessageSender;
import org.transport.service.impl.DefaultPacketService;

public class TransportFactory {
    public static TransportService<String> createSpigotTransport(
            String secret,
            String channel,
            TranslatorPlugin plugin,
            ChannelCarrierService carrierManager) {

        return TransportService.<String>builder()
                .packetService(new DefaultPacketService<>())
                .secret(secret)
                .encryptionEnabled(true)
                .options(TransportOptions.builder()
                        .batchingEnabled(true)
                        .maxBatchSize(100)
                        .batchingIntervalMs(5)
                        .maxPayloadSize(20000)
                        .build())
                .networkSender((PluginMessageSender<String>)
                        (s, bytes) -> {

                            carrierManager.getRandomCarrier().ifPresent(carrier ->
                            {
                                plugin.getTaskScheduler().runForEntity(carrier, () ->
                                        carrier.sendPluginMessage(plugin, channel, bytes));

                            });
                        })
                .build();
    }
}