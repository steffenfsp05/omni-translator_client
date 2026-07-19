package org.pytenix.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import org.omni.packets.PacketMapperRegistry;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.ConsentRefreshRequestData;
import org.omni.proto.generated.Protobuf;
import org.omni.transport.endpoint.ProfileEndpoint;
import org.pytenix.TranslatorPlugin;

import java.util.UUID;

@Singleton
public class TranslateCommand implements SimpleCommand {

    private final TranslatorPlugin translatorPlugin;
    private final ProfileEndpoint profileEndpoint;

    @Inject
    public TranslateCommand(TranslatorPlugin translatorPlugin, ProfileEndpoint profileEndpoint) {
        this.translatorPlugin = translatorPlugin;
        this.profileEndpoint = profileEndpoint;
    }


    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) return;

        profileEndpoint.sendRequest(player.getUniqueId())
                .thenAcceptAsync(profileData ->
                {

                    if (player.getCurrentServer().isPresent()) {


                        String[] args = invocation.arguments();

                        if (args.length == 1) {

                            if (args[0].equalsIgnoreCase("accept")) {
                                player.sendMessage(Component.text("§aYou accepted!"));

                                profileEndpoint.update(
                                        profileData.withConsentType(Protobuf.ConsentType.EXPLICIT)
                                );

                                sendToLobby(player);
                                return;
                            } else if (args[0].equalsIgnoreCase("decline")) {
                                player.sendMessage(Component.text("§cYou declined!"));

                                profileEndpoint.update(
                                        profileData.withConsentType(Protobuf.ConsentType.DECLINED)
                                );

                                sendToLobby(player);
                                return;

                            }


                        }


                    }
                });
    }

    private void sendToLobby(Player player) {
        translatorPlugin.getProxyServer().getServer("lobby").ifPresent(lobbyServer ->
                player.createConnectionRequest(lobbyServer).fireAndForget());
    }
}
