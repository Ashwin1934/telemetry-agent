package com.telemetry.mcp;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.typed.Cluster;
import akka.http.javadsl.Http;
import com.telemetry.actors.RoomActor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MCP Server main entry point.
 * 
 * Starts an ActorSystem that joins the existing Akka cluster,
 * initializes cluster sharding, and exposes MCP tools via HTTP.
 */
public class McpServer {
    private static final Logger log = LoggerFactory.getLogger(McpServer.class);

    public static void main(String[] args) throws Exception {
        log.info("Starting MCP Server");

        // Read hostname from environment
        String hostname = System.getenv("AKKA_REMOTE_ARTERY_CANONICAL_HOSTNAME");
        if (hostname == null) {
            hostname = "127.0.0.1";
        }

        // Build MCP server specific configuration
        Config mcpConfig = ConfigFactory.parseString(
            "akka.remote.artery.canonical.hostname = \"" + hostname + "\"\n" +
            "akka.remote.artery.canonical.port = 2553\n"
        ).withFallback(ConfigFactory.load());

        // Create actor system
        ActorSystem<McpServerBehavior.RootCommand> system = 
            ActorSystem.create(
                McpServerBehavior.create(),
                "telemetry-system",
                mcpConfig
            );

        log.info("MCP Server ActorSystem created");
    }

    /**
     * Root behavior for the MCP server.
     * Manages cluster formation and HTTP server startup.
     */
    public static class McpServerBehavior {
        private static final Logger log = LoggerFactory.getLogger(McpServerBehavior.class);

        public interface RootCommand {}

        public static Behavior<RootCommand> create() {
            return Behaviors.setup(context -> {
                log.info("Initializing MCP server cluster node");

                // Get cluster reference
                Cluster cluster = Cluster.get(context.getSystem());
                log.info("Cluster member address: {}", cluster.selfMember().address());

                // Initialize cluster sharding to access RoomEntity
                ClusterSharding sharding = ClusterSharding.get(context.getSystem());

                // Initialize RoomActor as a sharded entity
                // This allows the MCP server to get EntityRef to existing room entities
                sharding.init(
                    Entity.of(
                        "RoomEntity",
                        ctx -> RoomActor.create(ctx.getEntityId())
                    )
                );

                log.info("Room entity sharding initialized on MCP server");

                // Start HTTP server after a short delay to allow cluster to stabilize
                context.scheduleOnce(
                    java.time.Duration.ofSeconds(2),
                    () -> startHttpServer(context.getSystem(), sharding)
                );

                return Behaviors.same();
            });
        }

        private static void startHttpServer(
            ActorSystem<?> system,
            ClusterSharding sharding) {
            
            log.info("Starting MCP HTTP server on port 8080");

            try {
                // Create HTTP routes using the MCP HTTP handler
                McpHttpHandler httpHandler = new McpHttpHandler(sharding);
                akka.http.javadsl.server.Route route = httpHandler.createRoutes();

                // Start HTTP server
                Http http = Http.get(system);
                http.newServerAt("0.0.0.0", 8080)
                    .bind(route)
                    .whenComplete((serverBinding, throwable) -> {
                        if (throwable != null) {
                            log.error("Failed to bind HTTP server", throwable);
                            system.terminate();
                        } else {
                            log.info("MCP HTTP server listening on {}", serverBinding.localAddress());
                        }
                    });
            } catch (Exception e) {
                log.error("Error starting HTTP server", e);
                system.terminate();
            }
        }
    }
}
