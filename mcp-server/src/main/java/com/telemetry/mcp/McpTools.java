package com.telemetry.mcp;

import com.google.gson.JsonObject;

/**
 * MCP Tool definitions and utilities.
 * 
 * Defines the available MCP tools that can be invoked via the HTTP endpoint.
 * Each tool corresponds to a cluster sharding EntityRef + ask pattern.
 */
public class McpTools {

    /**
     * MCP Tool: get_room_state
     * 
     * Retrieves the current state of a room entity from the cluster.
     * Sends a GetRoomState query via ask to the RoomEntity shard region.
     */
    public static class GetRoomStateTool {
        public static final String NAME = "get_room_state";
        public static final String DESCRIPTION = 
            "Retrieve the current state of a room entity (temperature, humidity, last update)";

        public static JsonObject getSchema() {
            JsonObject schema = new JsonObject();
            schema.addProperty("type", "object");
            schema.addProperty("required", "[\"room\"]");
            
            JsonObject properties = new JsonObject();
            JsonObject roomProp = new JsonObject();
            roomProp.addProperty("type", "string");
            roomProp.addProperty("description", "The room name/ID");
            properties.add("room", roomProp);
            
            schema.add("properties", properties);
            return schema;
        }

        public static JsonObject buildRequest(String room) {
            JsonObject request = new JsonObject();
            request.addProperty("room", room);
            return request;
        }
    }

    /**
     * MCP Tool: update_room_sensors
     * 
     * Updates sensor data for a room entity in the cluster.
     * Sends an UpdateSensorData command via ask to the RoomEntity shard region.
     */
    public static class UpdateRoomSensorsTool {
        public static final String NAME = "update_room_sensors";
        public static final String DESCRIPTION = 
            "Update sensor data for a room (temperature and humidity)";

        public static JsonObject getSchema() {
            JsonObject schema = new JsonObject();
            schema.addProperty("type", "object");
            schema.addProperty("required", "[\"room\", \"temperature\", \"humidity\"]");
            
            JsonObject properties = new JsonObject();
            
            JsonObject roomProp = new JsonObject();
            roomProp.addProperty("type", "string");
            roomProp.addProperty("description", "The room name/ID");
            properties.add("room", roomProp);
            
            JsonObject tempProp = new JsonObject();
            tempProp.addProperty("type", "number");
            tempProp.addProperty("description", "Temperature in Celsius");
            properties.add("temperature", tempProp);
            
            JsonObject humidityProp = new JsonObject();
            humidityProp.addProperty("type", "number");
            humidityProp.addProperty("description", "Humidity percentage (0-100)");
            properties.add("humidity", humidityProp);
            
            schema.add("properties", properties);
            return schema;
        }

        public static JsonObject buildRequest(String room, float temperature, float humidity) {
            JsonObject request = new JsonObject();
            request.addProperty("room", room);
            request.addProperty("temperature", temperature);
            request.addProperty("humidity", humidity);
            return request;
        }
    }

    /**
     * Get all available MCP tools.
     */
    public static JsonObject getAllTools() {
        JsonObject tools = new JsonObject();
        
        JsonObject getTool = new JsonObject();
        getTool.addProperty("name", GetRoomStateTool.NAME);
        getTool.addProperty("description", GetRoomStateTool.DESCRIPTION);
        getTool.add("schema", GetRoomStateTool.getSchema());
        tools.add(GetRoomStateTool.NAME, getTool);
        
        JsonObject updateTool = new JsonObject();
        updateTool.addProperty("name", UpdateRoomSensorsTool.NAME);
        updateTool.addProperty("description", UpdateRoomSensorsTool.DESCRIPTION);
        updateTool.add("schema", UpdateRoomSensorsTool.getSchema());
        tools.add(UpdateRoomSensorsTool.NAME, updateTool);
        
        return tools;
    }
}
