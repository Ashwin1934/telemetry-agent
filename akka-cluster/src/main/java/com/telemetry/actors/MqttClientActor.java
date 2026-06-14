package com.telemetry.actors;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.telemetry.messages.RoomMessages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

// MqttClientActor runs on node 1 and manages MQTT broker connection
// Receives telemetry messages from MQTT and routes them to room entities via cluster sharding
// The shard region handles entity creation automatically - if a room doesn't exist, it's created
public class MqttClientActor extends AbstractBehavior<MqttClientActor.MqttCommand> {
    private static final Logger log = LoggerFactory.getLogger(MqttClientActor.class);

    public interface MqttCommand {}

    // Command to start MQTT client and connect to broker
    public static class StartMqtt implements MqttCommand {
        public final akka.cluster.sharding.typed.javadsl.ClusterSharding sharding;
        public final String brokerUrl;
        public final String topic;

        public StartMqtt(akka.cluster.sharding.typed.javadsl.ClusterSharding sharding, String brokerUrl, String topic) {
            this.sharding = sharding;
            this.brokerUrl = brokerUrl;
            this.topic = topic;
        }

        @Override
        public String toString() {
            return "StartMqtt{" +
                    "brokerUrl='" + brokerUrl + '\'' +
                    ", topic='" + topic + '\'' +
                    '}';
        }
    }

    public static class StopMqtt implements MqttCommand {}

    private final Gson gson = new Gson();
    private MqttClient mqttClient;
    private akka.cluster.sharding.typed.javadsl.ClusterSharding sharding;
    private String mqttTopic;
    private final ActorRef<RoomRegistryActor.Command>
        roomRegistry;

    public MqttClientActor(
        ActorContext<MqttCommand> context,
        ActorRef<RoomRegistryActor.Command> roomRegistry) {

    super(context);

    this.roomRegistry = roomRegistry;
}

    // Factory method to create MQTT client actor
    public static Behavior<MqttCommand> create(
        ActorRef<RoomRegistryActor.Command>
                roomRegistry) {

        return Behaviors.setup(
                context ->
                        new MqttClientActor(
                                context,
                                roomRegistry
                        )
        );
    }

    @Override
    public Receive<MqttCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(StartMqtt.class, this::onStartMqtt)
                .onMessage(StopMqtt.class, this::onStopMqtt)
                .build();
    }

    // Initialize and connect to MQTT broker
    // Once connected, subscribe to the configured topic
    private Behavior<MqttCommand> onStartMqtt(StartMqtt command) {
        this.sharding = command.sharding;
        this.mqttTopic = command.topic;

        try {
            // Create MQTT client with unique ID
            String clientId = "AkkaClusterNode_" + System.currentTimeMillis();
            mqttClient = new MqttClient(command.brokerUrl, clientId);

            // Configure connection options
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(10);

            // Set callback to handle incoming messages and connection events
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("MQTT connection lost", cause);
                }

                @Override
                public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) {
                    handleMqttMessage(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not used for subscriber role
                }
            });

            // Connect to broker
            mqttClient.connect(options);
            log.info("MQTT client connected to {}", command.brokerUrl);

            // Subscribe to telemetry topic with wildcard to catch all rooms
            // Topic structure: telemetry/<room>/sensorData
            String subscriptionTopic = command.topic + "/#";
            mqttClient.subscribe(subscriptionTopic);
            log.info("Subscribed to MQTT topic: {}", subscriptionTopic);

        } catch (MqttException e) {
            log.error("Failed to start MQTT client", e);
            return Behaviors.stopped();
        }

        return this;
    }

    // Disconnect from MQTT broker and stop actor
    private Behavior<MqttCommand> onStopMqtt(StopMqtt command) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
                log.info("MQTT client disconnected and closed");
            }
        } catch (MqttException e) {
            log.error("Error stopping MQTT client", e);
        }
        return Behaviors.stopped();
    }

    // Parse incoming MQTT message and route to room entity via shard region
    // Expected MQTT topic format: telemetry/<room>/sensorData
    // Expected message payload: {"temperature": 22.45, "humidity": 55.30}
    // The shard region will create the room entity if it doesn't exist
    private void handleMqttMessage(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) {
        try {
            // Parse topic to extract room name
            // Expected: telemetry/<room>/sensorData
            String[] parts = topic.split("/");
            if (parts.length < 3 || !parts[0].equals("telemetry")) {
                log.warn("Invalid topic format: {}", topic);
                return;
            }

            String room = parts[1];  // Extract room name from topic

            // Parse JSON payload
            String payload = new String(message.getPayload());
            JsonObject jsonObject = gson.fromJson(payload, JsonObject.class);
            
            if (!jsonObject.has("temperature") || !jsonObject.has("humidity")) {
                log.warn("Invalid payload format. Missing temperature or humidity: {}", payload);
                return;
            }

            float temperature = jsonObject.get("temperature").getAsFloat();
            float humidity = jsonObject.get("humidity").getAsFloat();
            long timestamp = System.currentTimeMillis();

            log.info("Received telemetry: room={}, temp={}, humidity={}", room, temperature, humidity);

            // Create update command
            RoomMessages.UpdateSensorData update = new RoomMessages.UpdateSensorData(
                    room,
                    temperature,
                    humidity,
                    timestamp
            );

            // Get EntityRef for the room using room name as entity ID
            // entityRefFor() returns a reference to the entity, creating it if it doesn't exist
            akka.cluster.sharding.typed.javadsl.EntityRef<Object> roomEntityRef = sharding.entityRefFor(RoomActor.typeKey, room);

            // Send the message to the specific room entity
            roomEntityRef.tell(update);
            log.debug("Routed update to room entity: {}", room);

        } catch (Exception e) {
            log.error("Error processing MQTT message", e);
        }
    }
}
