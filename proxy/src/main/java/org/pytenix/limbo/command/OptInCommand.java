package org.pytenix.limbo.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

public class OptInCommand implements SimpleCommand {

    private final ProxyServer proxy;

    public OptInCommand(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) return;
        Player player = (Player) invocation.source();

        if (player.getCurrentServer().isPresent() &&
                player.getCurrentServer().get().getServerInfo().getName().equals("dynamic-limbo")) {

            player.sendMessage(Component.text("Du hast erfolgreich akzeptiert!"));

            proxy.getServer("lobby").ifPresent(lobbyServer -> {
                player.createConnectionRequest(lobbyServer).fireAndForget();
            });
        }
    }
}
