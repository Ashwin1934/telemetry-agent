package com.telemetry.messages;
public class RoomMessages {

    // Command to update room sensor data
    public static class UpdateSensorData {
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

    // Query to get room state
    public static class GetRoomState {
        public final String room;

        public GetRoomState(String room) {
            this.room = room;
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
