package org.pytenix.limbo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class LimboConfigGenerator {

    public static void generateMinimalConfig(File configFile, String ip, int port, String secret) throws IOException {
        String yamlContent = """
                # NanoLimbo configuration
                            
                # Server's host address and port. Set ip empty to use public address
                bind:
                  ip: "%s"
                  port: %d
                            
                # Max number of players can join to server
                # Set -1 to make it infinite
                maxPlayers: 100
                            
                # Server's data in servers list
                ping:
                  description: "<gradient:blue:white>NanoLimbo"
                  version: "<gradient:blue:white>NanoLimbo"
                  protocol: -1
                            
                # Available dimensions: OVERWORLD, THE_NETHER, THE_END
                dimension: THE_END
                            
                # Whether to display the player in the player list
                playerList:
                  enable: false
                  username: "NanoLimbo"
                            
                # Whether to display header and footer in player list
                headerAndFooter:
                  enable: false
                  header: "<white>Welcome!"
                  footer: "<gradient:blue:white>NanoLimbo"
                            
                # Setup player's game mode
                # 0 - Survival, 1 - Creative, 2 - Adventure, 3 - Spectator
                gameMode: 3
                            
                # Remove secure-chat toast
                secureProfile: false
                            
                # Server name which is shown under F3
                brandName:
                  enable: false
                  content: "<gradient:blue:white>NanoLimbo"
                            
                # Message sends when player joins to the server
                joinMessage:
                  enable: false
                  text: "<white>Welcome to the <gradient:blue:white>NanoLimbo<white>!"
                            
                # BossBar displays when player joins to the server
                bossBar:
                  enable: false
                  text: "<white>Welcome to the <gradient:blue:white>NanoLimbo<white>!"
                  health: 1.0
                  color: BLUE
                  division: SOLID
                            
                # Display title and subtitle
                title:
                  enable: false
                  title: "<white><b>Welcome!"
                  subtitle: "<gradient:blue:white>NanoLimbo"
                  fadeIn: 10
                  stay: 100
                  fadeOut: 10
                            
                # Player info forwarding support.
                infoForwarding:
                  type: MODERN
                  secret: "%s"
                  tokens:
                  - "<BUNGEE_GUARD_TOKEN>"
                            
                # Read timeout for connections in milliseconds
                readTimeout: 30000
                            
                # Log player IP addresses on connect.
                logPlayersIp: true
                            
                # Define log level. For production, I'd recommend to use level 2
                debugLevel: 2
                            
                # Warning! Do not touch params of this block if you are not completely sure what is this!
                netty:
                  transportType: EPOLL
                  threads:
                    bossGroup: 1
                    workerGroup: 4
                            
                # Options to check incoming traffic and kick potentially malicious connections.
                traffic:
                  enable: true
                  maxPacketSize: 8192
                  interval: 7.0
                  maxPacketRate: 500.0
                  maxPacketBytesRate: 2048.0
                """.formatted(ip, port, secret);

        Files.writeString(configFile.toPath(), yamlContent);
    }
}
