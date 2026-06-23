package org.pytenix.limbo.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.pytenix.TranslatorPlugin;
import org.pytenix.proto.generated.NetworkPackets;

public class OptInCommand implements SimpleCommand {

    private final TranslatorPlugin translatorPlugin;

    public OptInCommand(TranslatorPlugin translatorPlugin) {
        this.translatorPlugin = translatorPlugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) return;
        Player player = (Player) invocation.source();

        if (player.getCurrentServer().isPresent() &&
                player.getCurrentServer().get().getServerInfo().getName().equals("dynamic-limbo")) {

            player.sendMessage(Component.text("Du hast erfolgreich akzeptiert!"));

            translatorPlugin.getProfileService().getProfile(player.getUniqueId())
                  .thenAcceptAsync(profileData ->
                  {
                      translatorPlugin.getProfileService().updateProfile(profileData.withConsentType(NetworkPackets.ProfilePacket.ConsentType.EXPLICIT));
                      translatorPlugin.getProxyServer().getServer("lobby").ifPresent(lobbyServer -> {
                          player.createConnectionRequest(lobbyServer).fireAndForget();
                      });
                  });


        }
    }
}
