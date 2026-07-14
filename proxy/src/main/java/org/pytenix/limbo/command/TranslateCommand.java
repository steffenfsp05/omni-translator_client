package org.pytenix.limbo.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import org.omni.packets.PacketMapperRegistry;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.ConsentRefreshRequestData;
import org.omni.profile.ProfileService;
import org.omni.proto.generated.Protobuf;
import org.pytenix.TranslatorPlugin;

import java.util.UUID;

@Singleton
public class TranslateCommand implements SimpleCommand {

    private final TranslatorPlugin translatorPlugin;
    private final ProfileService profileService;
    private final PacketMapperRegistry packetMapperRegistry;

    @Inject
    public TranslateCommand(TranslatorPlugin translatorPlugin, ProfileService profileService, PacketMapperRegistry packetMapperRegistry) {
        this.translatorPlugin = translatorPlugin;
        this.profileService = profileService;
        this.packetMapperRegistry = packetMapperRegistry;
    }


    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) return;

        profileService.retrieveProfile(player.getUniqueId())
                .thenAcceptAsync(profileData ->
                {

                    if (player.getCurrentServer().isPresent()) {


                        String[] args = invocation.arguments();

                        if (args.length == 1) {

                            if (args[0].equalsIgnoreCase("accept")) {
                                player.sendMessage(Component.text("§aYou accepted!"));

                                profileService.updateProfile(
                                        profileData.withConsentType(Protobuf.ConsentType.EXPLICIT)
                                );

                                sendToLobby(player);
                                return;
                            } else if (args[0].equalsIgnoreCase("decline")) {
                                player.sendMessage(Component.text("§cYou declined!"));

                                profileService.updateProfile(
                                        profileData.withConsentType(Protobuf.ConsentType.DECLINED)
                                );

                                sendToLobby(player);
                                return;

                            } else if (args[0].equalsIgnoreCase("toggle")) {
                                Protobuf.ConsentType newConsent = Protobuf.ConsentType.DECLINED;

                                //  if (profileData.consentType().equals(NetworkPackets.ProfilePacket.ConsentType.EXPLICIT)) {
                                //    }

                                //TODO: IMPLEMENT FOR AUTO_OPT LOGIC

                                if (profileData.consentType().equals(Protobuf.ConsentType.DECLINED))
                                    newConsent = Protobuf.ConsentType.EXPLICIT;


                                RegisteredServer registeredServer = null;
                                if (player.getCurrentServer().isPresent())
                                    registeredServer = player.getCurrentServer().get().getServer();


                                profileService.updateProfile(
                                        profileData.withConsentType(newConsent)
                                );


                                if (registeredServer != null)
                                    translatorPlugin.getProxyTransport().getTransportService().send(
                                            registeredServer,
                                            PacketRegistry.CONSENT_REFRESH,
                                            packetMapperRegistry.toProto(
                                                    new ConsentRefreshRequestData(
                                                            UUID.randomUUID(),
                                                            player.getUniqueId(),
                                                            newConsent
                                                    )
                                            ));

                                return;
                            }


                            player.sendMessage(Component.text("""
                                    §cInvalid command.
                                    §c/translate toggle
                                    §c/translate accept
                                    §c/translate decline"""));


                        }


                    }
                });
    }

    private void sendToLobby(Player player) {
        translatorPlugin.getProxyServer().getServer("lobby").ifPresent(lobbyServer ->
                player.createConnectionRequest(lobbyServer).fireAndForget());
    }
}
