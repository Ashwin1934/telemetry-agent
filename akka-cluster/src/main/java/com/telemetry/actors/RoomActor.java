package com.telemetry.actors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telemetry.messages.RoomMessages;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

// RoomActor is a sharded entity where each room gets its own actor instance
// Entity ID = room name (e.g., "garage", "bedroom", "living_room")
// The shard region automatically creates/destroys instances based on demand
// State stored: temperature, humidity, and lastUpdate timestamp
public class RoomActor extends AbstractBehavior<Object> {
    private static final Logger log = LoggerFactory.getLogger(RoomActor.class);

    private final String room;
    private float temperature = 0.0f;
    private float humidity = 0.0f;
    private long lastUpdate = 0L;

    public RoomActor(ActorContext<Object> context, String room) {
        super(context);
        this.room = room;
        log.info("RoomActor created for room: {}", room);
    }

    // Factory method to create RoomActor with room name as entity ID
    public static Behavior<Object> create(String room) {
        return Behaviors.setup(context -> new RoomActor(context, room));
    }

    @Override
    public Receive<Object> createReceive() {
        return newReceiveBuilder()
                .onMessage(RoomMessages.UpdateSensorData.class, this::onUpdateSensorData)
                .onMessage(RoomMessages.GetRoomState.class, this::onGetRoomState)
                .build();
    }

    // Handle sensor data update command
    // Called when MQTT client receives new telemetry data for this room
    // Updates state: temperature, humidity, and timestamp
    private Behavior<Object> onUpdateSensorData(RoomMessages.UpdateSensorData update) {
        this.temperature = update.temperature;
        this.humidity = update.humidity;
        this.lastUpdate = update.timestamp;

        log.info("Room {} updated: temp={}, humidity={}, lastUpdate={}", 
                 room, temperature, humidity, lastUpdate);

        // In a future enhancement, could emit events here for other subscribers
        // or trigger alarms based on sensor values

        return this;
    }

    // Handle state query and respond via replyTo
    // Sends the current room state back to the requestor
    private Behavior<Object> onGetRoomState(RoomMessages.GetRoomState query) {
        log.info("State query for room {}: temp={}, humidity={}, lastUpdate={}", 
                 room, temperature, humidity, lastUpdate);
        
        // Send response via replyTo ActorRef
        RoomMessages.RoomState response = new RoomMessages.RoomState(
            room,
            temperature,
            humidity,
            lastUpdate
        );
        query.replyTo.tell(response);

        return this;
    }
}
