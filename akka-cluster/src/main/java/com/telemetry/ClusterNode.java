package com.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telemetry.actors.MqttClientActor;
import com.telemetry.actors.RoomActor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.typed.Cluster;

// Main entry point for Akka cluster node
// Supports running as either node (1 or 2) via environment variable NODE_ID
// Each node runs on a different port (2551 and 2552) and can participate in cluster sharding
public class ClusterNode {
    private static final Logger log = LoggerFactory.getLogger(ClusterNode.class);

    public static void main(String[] args) throws Exception {
        // Read node ID from environment or command line
        String nodeId = System.getenv("NODE_ID");
        if (nodeId == null || nodeId.isEmpty()) {
            nodeId = args.length > 0 ? args[0] : "1";
        }

        log.info("Starting Akka cluster node with NODE_ID={}", nodeId);

        // Load base configuration
        Config baseConfig = ConfigFactory.load();
        
        // Build node-specific configuration
        Config nodeConfig;
        String hostname = System.getenv("AKKA_REMOTE_ARTERY_CANONICAL_HOSTNAME");
        if (hostname == null) {
            hostname = "127.0.0.1";
        }

        if ("1".equals(nodeId)) {
            nodeConfig = ConfigFactory.parseString(
                "akka.remote.artery.canonical.hostname = \"" + hostname + "\"\n" +
                "akka.remote.artery.canonical.port = 2551\n"
            ).withFallback(baseConfig);
        } else if ("2".equals(nodeId)) {
            nodeConfig = ConfigFactory.parseString(
                "akka.remote.artery.canonical.hostname = \"" + hostname + "\"\n" +
                "akka.remote.artery.canonical.port = 2552\n"
            ).withFallback(baseConfig);
        } else {
            nodeConfig = baseConfig;
        }

        // Create actor system
        ActorSystem<RootBehavior.RootCommand> system = 
            ActorSystem.create(RootBehavior.create(nodeId), "telemetry-system", nodeConfig);
    }

    // Root behavior that manages cluster setup
    public static class RootBehavior {
        private static final Logger log = LoggerFactory.getLogger(RootBehavior.class);

        public interface RootCommand {}

        public static Behavior<RootCommand> create(String nodeId) {
            return Behaviors.setup(context -> {
                log.info("Initializing cluster for node {}", nodeId);

                // Get cluster reference
                Cluster cluster = Cluster.get(context.getSystem());
                log.info("Cluster member address: {}", cluster.selfMember().address());

                // Initialize cluster sharding
                ClusterSharding sharding = ClusterSharding.get(context.getSystem());

                // Initialize RoomActor as a sharded entity
                // Each room name becomes an entity ID, and each room gets its own actor instance
                var roomShardRegion = sharding.init(
                    Entity.of(
                        RoomActor.typeKey,
                        ctx -> RoomActor.create(ctx.getEntityId())
                    )
                );

                log.info("Room entity sharding initialized");

                // If this is node 1, also start the MQTT client actor
                // The MQTT client will route messages to room entities via the shard region
                if ("1".equals(nodeId)) {
                    log.info("Node 1 detected. Starting MQTT client actor.");
                    var mqttClientRef = context.spawn(
                        MqttClientActor.create(),
                        "mqtt-client"
                    );

                    // Start MQTT connection after a short delay to allow cluster to form
                    context.scheduleOnce(
                        java.time.Duration.ofSeconds(3),
                        mqttClientRef,
                        new MqttClientActor.StartMqtt(
                            sharding,                 // ClusterSharding instance for entity routing
                            "tcp://mosquitto:1883",   // MQTT broker address (Docker service name)
                            "telemetry"               // MQTT topic to subscribe to
                        )
                    );

                    log.info("MQTT client scheduled to start in 3 seconds");
                }

                return Behaviors.same();
            });
        }
    }
}
