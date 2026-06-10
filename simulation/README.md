# Sensor Simulator

A standalone sensor device emulator that publishes telemetry data to an MQTT broker. Useful for testing the telemetry application without physical devices.

## Features

- **Multi-room simulation**: Simulate multiple rooms with independent sensor readings
- **Realistic data**: Temperature and humidity values with natural drift patterns
- **Configurable**: Control number of rooms, publish interval, duration, and broker connection
- **Docker ready**: Easy to run as a container from Docker Desktop
- **Auto-termination**: Optional automatic exit after configured duration
- **Graceful shutdown**: Handles Ctrl+C cleanly

## Prerequisites

- Python 3.7+ (for local running)
- MQTT broker running (e.g., Mosquitto)
- Docker (for containerized deployment)

## Usage

### Option 1: Docker (Recommended)

#### Using Docker Desktop UI

1. Open Docker Desktop
2. Navigate to Containers
3. In the `simulation` folder, find the `docker-compose.yml`
4. Click the compose file to start it (the simulator runs independently)
5. The simulator will connect to the MQTT broker via the exposed port (1883) and run for 10 minutes

#### Using Docker Compose CLI

```bash
cd simulation
docker-compose up
```

The simulator will run for 10 minutes (600 seconds) by default and then exit cleanly.

**Note:** The simulator runs independently from the Akka cluster and connects to the MQTT broker via the exposed port (`localhost:1883`). No need to add it to the cluster's docker-compose.yml.

#### Customizing Duration

Set the `DURATION` environment variable (in seconds):

```bash
cd simulation
docker-compose run -e DURATION=300 sensor-simulator
```

For infinite runtime (until manually stopped):

```bash
cd simulation
docker-compose run -e DURATION=-1 sensor-simulator
```

#### Other Environment Variables

You can configure the simulator via environment variables:

```bash
cd simulation
docker-compose run \
  -e DURATION=300 \
  -e NUM_ROOMS=10 \
  -e INTERVAL=1 \
  -e MQTT_HOST=host.docker.internal \
  -e MQTT_PORT=1883 \
  sensor-simulator
```

- `DURATION` (default: `600`) - Run duration in seconds, or -1 for infinite
- `NUM_ROOMS` (default: `5`) - Number of rooms to simulate
- `INTERVAL` (default: `2`) - Seconds between sensor readings
- `MQTT_HOST` (default: `host.docker.internal`) - MQTT broker hostname
- `MQTT_PORT` (default: `1883`) - MQTT broker port

### Option 2: Local Python Execution

#### Installation

Install the required dependencies:

```bash
pip install -r requirements.txt
```

#### Basic Usage

```bash
python sensor_simulator.py
```

This runs with defaults: 5 rooms, 2-second interval, connects to localhost:1883

#### Custom Configuration

```bash
python sensor_simulator.py \
  --host mosquitto \
  --port 1883 \
  --rooms 10 \
  --interval 2 \
  --duration 300 \
  --debug
```

#### Arguments

- `--host` (default: `localhost`) - MQTT broker hostname/IP
- `--port` (default: `1883`) - MQTT broker port  
- `--rooms` (default: `5`) - Number of rooms to simulate
- `--interval` (default: `2`) - Seconds between sensor readings
- `--duration` (optional) - Duration in seconds before exiting (omit for infinite runtime)
- `--prefix` (default: `room`) - Prefix for room names (e.g., `room_01`, `room_02`)
- `--debug` - Enable debug logging for troubleshooting

## Example: Complete Test Flow

### 1. Start the Akka cluster with Docker Compose

```bash
cd akka-cluster
docker-compose up
```

Wait for all services to be healthy (mosquitto, akka-node-1, akka-node-2)

### 2. In another terminal, start the sensor simulator

The simulator runs completely independently and connects to the MQTT broker via the exposed port.

**Option A: Using Docker Compose** (from simulation folder)
```bash
cd simulation
docker-compose up
```

**Option B: Local Python** (if Python environment is set up)
```bash
cd simulation
pip install -r requirements.txt
python sensor_simulator.py --host localhost --rooms 5 --duration 300
```

### 3. Monitor the Akka cluster processing

In yet another terminal:
```bash
docker logs -f akka-node-1
```

You should see messages like:
```
Received telemetry: room=room_01, temp=22.45, humidity=55.30
Routed update to room entity: room_01
```

The simulator publishes to the MQTT broker on port 1883, and the Akka cluster subscribes to those messages automatically.

## MQTT Topic Structure

The simulator publishes to the following topic pattern:

```
telemetry/<room>/sensorData
```

**Payload Format (JSON):**

```json
{
  "temperature": 22.45,
  "humidity": 55.30
}
```

Example topic: `telemetry/room_01/sensorData`

## Data Characteristics

- **Temperature Range**: 18°C - 28°C (typical indoor environment)
- **Humidity Range**: 30% - 70% (percentage)
- **Drift**: ±0.5°C and ±2% per reading (realistic sensor variation)
- **Update Frequency**: Configurable (default 2 seconds)

## Troubleshooting

### Connection refused

Make sure the MQTT broker is running and accessible at the specified host and port.

```bash
# Test connectivity
mosquitto_sub -h localhost -t test
```

### No messages in Akka logs

- Check that simulator is connected: Look for "Connected to MQTT broker" in output
- Verify broker is healthy: Check Docker logs for Mosquitto
- Confirm topic format matches expectations (should be `telemetry/<room>/sensorData`)

### Enable debugging

Run with `--debug` flag to see detailed MQTT and message flow information:

```bash
python sensor_simulator.py --debug
```

Or in Docker:

```bash
docker-compose run -e DEBUG=--debug sensor-simulator
```

### Simulator exits immediately

Check if you hit the duration timeout. Look at the container logs:

```bash
docker logs sensor-simulator
```

## Performance Notes

- Each room publishes independently, so total message rate = `(number of rooms) / interval`
- Default: 5 rooms × 1 message per 2 seconds = 2.5 messages/sec
- MQTT QoS 1 ensures at-least-once delivery
