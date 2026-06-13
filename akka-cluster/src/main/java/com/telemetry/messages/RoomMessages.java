package com.telemetry.messages;

import akka.actor.typed.ActorRef;

public class RoomMessages {

    // Base interface for all room messages
    public interface RoomCommand {}

    // Command to update room sensor data (fire-and-forget)
    public static class UpdateSensorData implements RoomCommand {
        public final String room;
        public final float temperature;
        public final float humidity;
        public final long timestamp;

        public UpdateSensorData(String room, float temperature, float humidity, long timestamp) {
            this.room = room;
            this.temperature = temperature;
            this.humidity = humidity;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "UpdateSensorData{" +
                    "room='" + room + '\'' +
                    ", temperature=" + temperature +
                    ", humidity=" + humidity +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }

    // Query to get room state (requires response via replyTo)
    public static class GetRoomState implements RoomCommand {
        public final String room;
        public final ActorRef<RoomState> replyTo;

        public GetRoomState(String room, ActorRef<RoomState> replyTo) {
            this.room = room;
            this.replyTo = replyTo;
        }

        @Override
        public String toString() {
            return "GetRoomState{" + "room='" + room + '\'' + '}';
        }
    }

    // Response with room state
    public static class RoomState {
        public final String room;
        public final float temperature;
        public final float humidity;
        public final long lastUpdate;

        public RoomState(String room, float temperature, float humidity, long lastUpdate) {
            this.room = room;
            this.temperature = temperature;
            this.humidity = humidity;
            this.lastUpdate = lastUpdate;
        }

        @Override
        public String toString() {
            return "RoomState{" +
                    "room='" + room + '\'' +
                    ", temperature=" + temperature +
                    ", humidity=" + humidity +
                    ", lastUpdate=" + lastUpdate +
                    '}';
        }
    }
}
