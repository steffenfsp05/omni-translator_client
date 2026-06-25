package org.pytenix.limbo.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.pytenix.TranslatorPlugin;
import org.pytenix.packets.PacketMapperRegistry;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.packets.impl.ConsentRefreshRequestMapper;
import org.pytenix.proto.generated.NetworkPackets;

import java.util.UUID;

public class TranslateCommand implements SimpleCommand {

    private final TranslatorPlugin translatorPlugin;

    public TranslateCommand(TranslatorPlugin translatorPlugin) {
        this.translatorPlugin = translatorPlugin;
    }


    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) return;
        Player player = (Player) invocation.source();

        translatorPlugin.getProfileService().retrieveProfile(player.getUniqueId())
                .thenAcceptAsync(profileData ->
                {

                    if (player.getCurrentServer().isPresent()) {


                        String[] args = invocation.arguments();

                        if (args.length == 1) {

                            if (args[0].equalsIgnoreCase("accept")) {
                                player.sendMessage(Component.text("§aYou accepted!"));

                                translatorPlugin.getProfileService().updateProfile(
                                        profileData.withConsentType(NetworkPackets.ProfilePacket.ConsentType.EXPLICIT)
                                );

                                sendToLobby(player);
                                return;
                            } else if (args[0].equalsIgnoreCase("decline")) {
                                player.sendMessage(Component.text("§cYou declined!"));

                                translatorPlugin.getProfileService().updateProfile(
                                        profileData.withConsentType(NetworkPackets.ProfilePacket.ConsentType.DECLINED)
                                );

                                sendToLobby(player);
                                return;

                            } else if (args[0].equalsIgnoreCase("toggle")) {
                                NetworkPackets.ProfilePacket.ConsentType newConsent = NetworkPackets.ProfilePacket.ConsentType.DECLINED;
                                ComponentLike component = Component.text("§cUnknown value");

                                if (profileData.consentType().equals(NetworkPackets.ProfilePacket.ConsentType.EXPLICIT)) {
                                    newConsent = NetworkPackets.ProfilePacket.ConsentType.DECLINED;
                                    component = Component.text("§cYou turned translations off");
                                }

                                if (profileData.consentType().equals(NetworkPackets.ProfilePacket.ConsentType.DECLINED)) {
                                    newConsent = NetworkPackets.ProfilePacket.ConsentType.EXPLICIT;
                                    component = Component.text("§aYou turned translations on");
                                }


                                player.sendMessage(component);

                                RegisteredServer registeredServer = null;
                                if (player.getCurrentServer().isPresent())
                                    registeredServer = player.getCurrentServer().get().getServer();


                                translatorPlugin.getProfileService().updateProfile(
                                        profileData.withConsentType(newConsent)
                                );


                                if (registeredServer != null)
                                    translatorPlugin.getProxyTransport().getTransportService().send(
                                            registeredServer,
                                            PacketRegistry.CONSENT_REFRESH,
                                            PacketMapperRegistry.toProto(
                                                    new ConsentRefreshRequestMapper.Data(
                                                            UUID.randomUUID(),
                                                            player.getUniqueId(),
                                                            newConsent
                                                    )
                                            ));

                                return;
                            }


                            player.sendMessage(Component.text("§cInvalid command.\n" +
                                    "§c/translate toggle\n" +
                                    "§c/translate accept\n" +
                                    "§c/translate decline"));


                            return;

                        }


                    }
                });
    }

    private void sendToLobby(Player player) {
        translatorPlugin.getProxyServer().getServer("lobby").ifPresent(lobbyServer ->
                player.createConnectionRequest(lobbyServer).fireAndForget());
    }
}
