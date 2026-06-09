# Akka Cluster Telemetry Setup

## Project Structure

```
akka-cluster/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── docker-entrypoint.sh
├── .gitignore
├── .dockerignore
├── src/
│   └── main/
│       ├── java/com/telemetry/
│       │   ├── ClusterNode.java          # Main entry point
│       │   ├── messages/RoomMessages.java # Message/command definitions
│       │   └── actors/
│       │       ├── RoomActor.java         # Sharded room entity
│       │       └── MqttClientActor.java   # MQTT client (runs on node 1)
│       └── resources/
│           ├── application.conf          # Akka configuration
│           └── logback.xml               # Logging configuration
```

## Architecture Overview

- **Two Akka Cluster Nodes** on ports 2551 and 2552
  - Node 1 additionally runs MqttClientActor for MQTT broker integration
  - Both nodes participate in cluster sharding

- **Cluster Sharding**: RoomActor entities sharded by room name
  - When MQTT receives telemetry for a new room, the entity is created automatically
  - Each room actor stores: temperature, humidity, lastUpdate timestamp
  - Entities are passivated (stopped) after 2 minutes of inactivity

- **MQTT Integration**: 
  - Subscribes to `telemetry/#` topics
  - Parses room names from topic path: `telemetry/<room>/sensorData`
  - Routes UpdateSensorData messages to appropriate room entity via shard region

- **Mosquitto MQTT Broker**: Runs in Docker, accessible at `mosquitto:1883`

## Building and Running

### Prerequisites
- Docker
- Docker Compose
- Java 11+ (for local building, not needed for Docker)
- Maven 3.6+ (for local building, not needed for Docker)

### Quick Start with Docker Compose

```bash
cd akka-cluster
docker-compose up --build
```

This will:
1. Build the Akka application using Maven in a builder stage
2. Start Mosquitto MQTT broker
3. Start Akka Node 1 on port 2551 (with MQTT client)
4. Start Akka Node 2 on port 2552

Logs from all services will be displayed in the terminal.

### Local Build (if needed)

```bash
mvn clean package
```

Produces: `target/akka-cluster-app.jar`

## Testing the Setup

### 1. Connect to Mosquitto and publish test data

```bash
# From another terminal
docker exec mosquitto mosquitto_pub -h localhost -t "telemetry/garage/sensorData" -m '{"temperature":22.5,"humidity":45.3}'
```

### 2. View cluster logs

```bash
docker-compose logs -f akka-node-1
docker-compose logs -f akka-node-2
```

Expected in logs when message arrives:
- `MQTT client connected`
- `Subscribed to MQTT topic`
- `Received telemetry: room=garage`
- `Room garage updated: temp=22.5, humidity=45.3`

### 3. Connect to Mosquitto CLI for testing

```bash
docker exec -it mosquitto sh
mosquitto_pub -h localhost -t "telemetry/bedroom/sensorData" -m '{"temperature":20.0,"humidity":50.0}'
```

## Customization

### Add more sensor readings per message
Modify `RoomMessages.UpdateSensorData` to include additional fields
Modify `RoomActor` to store them
Modify `MqttClientActor.handleMqttMessage()` to parse them from JSON

### Change MQTT broker connection
Edit `docker-compose.yml` to point to external broker
Or modify the broker URL in `ClusterNode.RootBehavior.create()`

### Scale to more nodes
Add more service definitions in `docker-compose.yml`
Update ports (2553, 2554, etc.)
Increase `number-of-shards` in `application.conf`

## Troubleshooting

### Cluster not forming
- Check logs: `docker-compose logs akka-node-1`
- Ensure seed-nodes address matches service name in docker-compose.yml
- Verify network connectivity between containers

### MQTT not connecting
- Check Mosquitto is running: `docker-compose ps`
- Verify broker health: `docker-compose logs mosquitto`
- Check MQTT broker URL in ClusterNode (should match docker-compose service name)

### OutOfMemory errors
- Increase JVM heap in docker-compose.yml: modify JVM_OPTS environment variable
- Example: `-Xmx1G` for 1GB heap
