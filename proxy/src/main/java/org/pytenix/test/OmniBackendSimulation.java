package org.pytenix.test;

/*
import io.gatling.app.Gatling;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import org.pytenix.proto.generated.NetworkPackets; // Dein Protobuf-Import
import org.pytenix.util.UuidUtil; // Dein Utility

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;


 */
public class OmniBackendSimulation /*extends Simulation
 */ {

/*
    public static void main(String[] args) {
     //   Gatling.main(new String[] {
    //            "--simulation", OmniBackendSimulation.class.getName(),
     //           "--results-folder", "target/gatling-results"
     //   });
    }


    // 1. WebSocket & Authentifizierung konfigurieren (Gibt sich als Velocity aus)
    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://178.105.71.66:8083") // Deine Backend-IP
            .wsBaseUrl("ws://178.105.71.66:8083")
            .header("X-API-KEY", "DEIN-TEST-KEY"); // Authentifizierung wie in RestfulService

    // 2. Das vorbereitete Platzhalter-Dataset laden (zufällig ziehen)
    FeederBuilder<String> chatFeeder = csv("chat_data.csv").random();
    List<Map<String, Object>> apiKeys = IntStream.rangeClosed(0, 50)
            .mapToObj(i -> Map.<String, Object>of("apiKey", "DEIN-TEST-KEY-" + i))
            .collect(Collectors.toList());

    FeederBuilder<Object> apiKeyFeeder = listFeeder(apiKeys).random();
    final Random random = new Random();
    // 3. Das Test-Szenario
    ScenarioBuilder scn = scenario("Omni Protobuf Load Test")
            .feed(chatFeeder)   // Deine Chat-Nachrichten
            .feed(apiKeyFeeder) // Zieht einen zufälligen API-Key aus der Liste
            // WebSocket Verbindung aufbauen
            .exec(ws("Connect to Omni").connect("/").header("X-API-KEY", "#{apiKey}"))
            .pause(1) // Kurz warten bis Connection stehtQ

            // Den Loop starten (Jeder "Fake-Server" schickt dauerhaft Nachrichten)
            .during(Duration.ofMinutes(10)).on(
                    exec(ws("Send Protobuf Batch")

                            // Sende BINÄRE Daten (sendBytes), kein Text!
                            .sendBytes(session -> {
                                // Daten aus der CSV holen

                             // String text = session.getString("chat_message");
                                String lang = "en_us";
                                String module = session.getString("module");

                                // Baue genau das gleiche Protobuf auf wie in RestfulService.java

                                NetworkPackets.TranslationBatchRequest.Builder batch = NetworkPackets.TranslationBatchRequest.newBuilder();

                                for(int i = 0; i <= 10; i++)
                                {
                                  String text = session.getString("chat_message") + " - " + UUID.randomUUID().toString().substring(0,4) + " Ich bin " + ThreadLocalRandom.current().nextInt(0, 100) + " Jahre alt";
                                    NetworkPackets.TranslationRequest request = NetworkPackets.TranslationRequest.newBuilder()
                                            .setRequestId(UuidUtil.toByteString(UUID.randomUUID()))
                                            .setText(text )
                                            .setTargetLang(lang)
                                            .setModule(module)
                                            .build();

                                    batch.addRequests(request);
                                }

                                // Da dein Backend Batches erwartet, packen wir es in einen Batch
                                // (Für den Stresstest reicht hier ein Batch der Größe 1, da wir ja 3000 Fake-Server simulieren)


                                NetworkPackets.PacketWrapper wrapper = NetworkPackets.PacketWrapper.newBuilder()
                                        .setBatchRequest(batch.build())
                                        .build();

                                // Als Byte-Array zurückgeben, Gatling schickt es über den WebSocket
                                return wrapper.toByteArray();
                            })
                    )
                            // Kurze Pause, damit ein Fake-Server nicht unendlich feuert, sondern realistisch bleibt
                            .pause(Duration.ofMillis(250))
            )
            .exec(ws("Close Connection").close());

    {
        // 4. Stresstest starten: Wir simulieren z.B. 50 Velocity-Server,
        // die gleichzeitig massiv Daten feuern.
        setUp(
                scn.injectOpen(rampUsers(17).during(10))
        ).protocols(httpProtocol);
    }

 */
}