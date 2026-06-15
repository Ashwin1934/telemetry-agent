package com.telemetry.mcp;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.AskPattern;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.telemetry.messages.RoomMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import scala.concurrent.duration.Duration;
import com.google.gson.JsonArray;
import java.util.List;
import java.util.ArrayList;

/**
 * HTTP handler for MCP endpoints.
 * 
 * Provides REST routes that translate HTTP requests into Akka ask() calls
 * to cluster sharded RoomEntity actors, returning actor state as JSON responses.
 */
public class McpHttpHandler extends AllDirectives {
    private static final Logger log = LoggerFactory.getLogger(McpHttpHandler.class);
    
    private final ClusterSharding sharding;
    private final Gson gson;
    private final Duration askTimeout;

    public McpHttpHandler(ClusterSharding sharding) {
        this.sharding = sharding;
        this.gson = new Gson();
        this.askTimeout = Duration.create(3, TimeUnit.SECONDS);
    }

    /**
     * Create all MCP HTTP routes.
     */
    public Route createRoutes() {
        return concat(
            pathPrefix("mcp", () -> concat(
                pathPrefix("tools", () -> concat(
                    get(() -> handleListTools()),
                    path(Segment, toolName -> 
                        post(() -> handleToolCall(toolName))
                    )
                )),
                path("rooms", () ->
                    get(() -> handleGetAllRooms())
                ),
                pathPrefix("room", () -> concat(
                    path(Segment, roomName ->
                        get(() -> handleGetRoomState(roomName))
                    ),
                    path(Segment / "update", roomName ->
                        post(() -> handleUpdateRoomState(roomName))
                    )
                )),
                get(() -> handleHealthCheck())
            ))
        );
    }

    /**
     * List available MCP tools.
     */
    private Route handleListTools() {
        JsonObject response = new JsonObject();
        
        JsonObject tools = new JsonObject();
        
        // Tool 1: Get room state
        JsonObject getRoomTool = new JsonObject();
        getRoomTool.addProperty("name", "get_room_state");
        getRoomTool.addProperty("description", "Retrieve the current state of a room entity (temperature, humidity, last update)");
        JsonObject getRoomParams = new JsonObject();
        getRoomParams.addProperty("room", "string - The room name/ID");
        getRoomTool.add("parameters", getRoomParams);
        tools.add("get_room_state", getRoomTool);
        
        // Tool 2: Update room sensors
        JsonObject updateTool = new JsonObject();
        updateTool.addProperty("name", "update_room_sensors");
        updateTool.addProperty("description", "Update sensor data for a room (temperature and humidity)");
        JsonObject updateParams = new JsonObject();
        updateParams.addProperty("room", "string - The room name/ID");
        updateParams.addProperty("temperature", "number - Temperature in Celsius");
        updateParams.addProperty("humidity", "number - Humidity percentage");
        updateTool.add("parameters", updateParams);
        tools.add("update_room_sensors", updateTool);
        
        response.add("tools", tools);
        
        return complete(
            akka.http.javadsl.model.StatusCodes.OK,
            response.toString(),
            Jackson.marshaller()
        );
    }

    /**
     * Handle tool invocation via MCP.
     * Routes to appropriate handler based on tool name.
     */
    private Route handleToolCall(String toolName) {
        return entity(Jackson.unmarshaller(JsonObject.class), requestBody -> {
            log.info("Tool call: {} with body: {}", toolName, requestBody);
            
            switch (toolName) {
                case "get_room_state":
                    String roomName = requestBody.get("room").getAsString();
                    return handleGetRoomState(roomName);
                    
                case "update_room_sensors":
                    String room = requestBody.get("room").getAsString();
                    float temperature = requestBody.get("temperature").getAsFloat();
                    float humidity = requestBody.get("humidity").getAsFloat();
                    return handleUpdateRoomState(room, temperature, humidity);
                    
                default:
                    return complete(
                        akka.http.javadsl.model.StatusCodes.NOT_FOUND,
                        "Tool not found: " + toolName
                    );
            }
        });
    }

    /**
     * Get the state of a room entity via cluster sharding ask.
     */
    private Route handleGetRoomState(String roomName) {
        // Get EntityRef for the room
        EntityRef<Object> roomRef = sharding.entityRefFor(
            "RoomEntity",
            roomName
        );

        // Use ask pattern with proper replyTo
        // Signature: AskPattern.ask(ref, messageFactory, timeout, scheduler)
        CompletionStage<RoomMessages.RoomState> response = AskPattern.ask(
            roomRef,
            (ActorRef<RoomMessages.RoomState> replyTo) -> 
                new RoomMessages.GetRoomState(roomName, replyTo),
            askTimeout,
            sharding.system().scheduler()
        );

        return onSuccess(response, (RoomMessages.RoomState state) -> {
            JsonObject json = new JsonObject();
            json.addProperty("room", state.room);
            json.addProperty("temperature", state.temperature);
            json.addProperty("humidity", state.humidity);
            json.addProperty("lastUpdate", state.lastUpdate);
            
            return complete(
                akka.http.javadsl.model.StatusCodes.OK,
                json.toString(),
                Jackson.marshaller()
            );
        });
    }

    /**
     * Update room sensor data via cluster sharding.
     */
    private Route handleUpdateRoomState(String roomName) {
        return entity(Jackson.unmarshaller(JsonObject.class), requestBody -> {
            float temperature = requestBody.get("temperature").getAsFloat();
            float humidity = requestBody.get("humidity").getAsFloat();
            return handleUpdateRoomState(roomName, temperature, humidity);
        });
    }

    private Route handleUpdateRoomState(String roomName, float temperature, float humidity) {
        // Get EntityRef for the room
        EntityRef<Object> roomRef = sharding.entityRefFor(
            "RoomEntity",
            roomName
        );

        // Send UpdateSensorData command
        long timestamp = System.currentTimeMillis();
        RoomMessages.UpdateSensorData update = new RoomMessages.UpdateSensorData(
            roomName,
            temperature,
            humidity,
            timestamp
        );

        roomRef.tell(update);

        // Return success response
        JsonObject response = new JsonObject();
        response.addProperty("status", "success");
        response.addProperty("room", roomName);
        response.addProperty("temperature", temperature);
        response.addProperty("humidity", humidity);
        response.addProperty("timestamp", timestamp);

        return complete(
            akka.http.javadsl.model.StatusCodes.OK,
            response.toString(),
            Jackson.marshaller()
        );
    }

    /**
     * Health check endpoint.
     */
    private Route handleHealthCheck() {
        JsonObject response = new JsonObject();
        response.addProperty("status", "healthy");
        response.addProperty("timestamp", System.currentTimeMillis());

        return complete(
            akka.http.javadsl.model.StatusCodes.OK,
            response.toString(),
            Jackson.marshaller()
        );
    }

    private Route handleGetAllRooms() {

        List<String> roomNames = List.of(
                "LivingRoom",
                "Bedroom"
        );

        List<CompletionStage<RoomMessages.RoomState>> futures
                = new ArrayList<>();

        for (String roomName : roomNames) {

            EntityRef<Object> roomRef = sharding.entityRefFor(
                    "RoomEntity",
                    roomName
            );

            CompletionStage<RoomMessages.RoomState> response
                    = AskPattern.ask(
                            roomRef,
                            (ActorRef<RoomMessages.RoomState> replyTo)
                            -> new RoomMessages.GetRoomState(
                                    roomName,
                                    replyTo
                            ),
                            askTimeout,
                            sharding.system().scheduler()
                    );

            futures.add(response);
        }

        CompletionStage<Void> all
                = java.util.concurrent.CompletableFuture.allOf(
                        futures.stream()
                                .map(CompletionStage::toCompletableFuture)
                                .toArray(java.util.concurrent.CompletableFuture[]::new)
                );

        return onSuccess(
                all,
                ignored -> {

                    JsonArray rooms = new JsonArray();

                    for (CompletionStage<RoomMessages.RoomState> future : futures) {

                        RoomMessages.RoomState state
                        = future.toCompletableFuture().join();

                        JsonObject room = new JsonObject();

                        room.addProperty("room", state.room);
                        room.addProperty("temperature", state.temperature);
                        room.addProperty("humidity", state.humidity);
                        room.addProperty("lastUpdate", state.lastUpdate);

                        rooms.add(room);
                    }

                    return complete(
                            akka.http.javadsl.model.StatusCodes.OK,
                            rooms.toString(),
                            Jackson.marshaller()
                    );
                }
        );
    }
}
