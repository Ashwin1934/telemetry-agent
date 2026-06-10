#!/usr/bin/env python3
"""
Sensor Simulator - Emulates IoT sensor devices publishing telemetry data to MQTT broker.
Generates random temperature and humidity readings for multiple rooms and publishes
them at regular intervals.
"""

import json
import random
import time
import argparse
import logging
import signal
import sys
from datetime import datetime
import paho.mqtt.client as mqtt

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class SensorSimulator:
    """Simulates sensor devices and publishes telemetry to MQTT broker."""

    def __init__(self, broker_host, broker_port, num_rooms, interval, room_prefix="room"):
        """
        Initialize sensor simulator.

        Args:
            broker_host: MQTT broker hostname/IP
            broker_port: MQTT broker port
            num_rooms: Number of rooms to simulate
            interval: Seconds between sensor readings
            room_prefix: Prefix for room names (e.g., "room_01", "room_02")
        """
        self.broker_host = broker_host
        self.broker_port = broker_port
        self.num_rooms = num_rooms
        self.interval = interval
        self.room_prefix = room_prefix
        self.rooms = [f"{room_prefix}_{i:02d}" for i in range(1, num_rooms + 1)]
        self.running = False
        self.client = None
        self.connected = False

        # Temperature and humidity ranges (realistic for indoor environments)
        self.temp_range = (18.0, 28.0)  # Celsius
        self.humidity_range = (30.0, 70.0)  # Percentage
        
        # Base values for more realistic simulation (values drift slightly)
        self.base_temps = {room: random.uniform(*self.temp_range) for room in self.rooms}
        self.base_humidity = {room: random.uniform(*self.humidity_range) for room in self.rooms}

    def on_connect(self, client, userdata, flags, rc):
        """Callback for when the client connects to the broker."""
        if rc == 0:
            logger.info(f"Connected to MQTT broker at {self.broker_host}:{self.broker_port}")
            self.connected = True
        else:
            logger.error(f"Failed to connect, return code {rc}")
            self.connected = False

    def on_disconnect(self, client, userdata, rc):
        """Callback for when the client disconnects from the broker."""
        if rc != 0:
            logger.warning(f"Unexpected disconnection from broker (code {rc}). Attempting to reconnect...")
            self.connected = False
        else:
            logger.info("Disconnected from MQTT broker")
            self.connected = False

    def on_publish(self, client, userdata, mid):
        """Callback for when a message is published."""
        logger.debug(f"Message published (ID: {mid})")

    def on_log(self, client, userdata, level, buf):
        """Callback for MQTT client logging."""
        logger.debug(f"MQTT: {buf}")

    def connect(self):
        """Connect to MQTT broker."""
        try:
            self.client = mqtt.Client(
                client_id=f"sensor-simulator-{int(time.time())}",
                clean_session=True
            )
            self.client.on_connect = self.on_connect
            self.client.on_disconnect = self.on_disconnect
            self.client.on_publish = self.on_publish
            
            logger.info(f"Connecting to MQTT broker at {self.broker_host}:{self.broker_port}...")
            self.client.connect(self.broker_host, self.broker_port, keepalive=60)
            self.client.loop_start()
            
            # Wait for connection
            timeout = 10
            while not self.connected and timeout > 0:
                time.sleep(0.1)
                timeout -= 0.1
            
            if not self.connected:
                raise Exception("Failed to connect to MQTT broker within timeout")
                
        except Exception as e:
            logger.error(f"Error connecting to MQTT broker: {e}")
            raise

    def generate_sensor_data(self, room):
        """
        Generate realistic sensor data for a room with slight drift from base values.

        Args:
            room: Room name

        Returns:
            Dictionary with temperature and humidity values
        """
        # Add small random drift to base values (±0.5°C and ±2% humidity)
        temp_drift = random.uniform(-0.5, 0.5)
        humidity_drift = random.uniform(-2, 2)
        
        temperature = self.base_temps[room] + temp_drift
        humidity = self.base_humidity[room] + humidity_drift
        
        # Clamp values to realistic ranges
        temperature = max(self.temp_range[0], min(self.temp_range[1], temperature))
        humidity = max(self.humidity_range[0], min(self.humidity_range[1], humidity))
        
        return {
            "temperature": round(temperature, 2),
            "humidity": round(humidity, 2)
        }

    def publish_sensor_data(self):
        """Publish sensor data for all rooms."""
        if not self.connected:
            logger.warning("Not connected to MQTT broker. Skipping publish...")
            return

        timestamp = datetime.now().isoformat()
        
        for room in self.rooms:
            try:
                sensor_data = self.generate_sensor_data(room)
                topic = f"telemetry/{room}/sensorData"
                payload = json.dumps(sensor_data)
                
                # Publish with QoS 1 (at least once delivery)
                result = self.client.publish(topic, payload, qos=1)
                
                if result.rc == mqtt.MQTT_ERR_SUCCESS:
                    logger.info(f"Published to {topic}: {payload}")
                else:
                    logger.error(f"Failed to publish to {topic}: {result.rc}")
                    
            except Exception as e:
                logger.error(f"Error publishing data for room {room}: {e}")

    def start(self, duration=None):
        """
        Start the sensor simulator.
        
        Args:
            duration: Optional duration in seconds to run before exiting
        """
        self.running = True
        logger.info(f"Starting sensor simulator with {self.num_rooms} rooms, interval: {self.interval}s")
        logger.info(f"Simulating rooms: {', '.join(self.rooms)}")
        
        if duration:
            logger.info(f"Simulator will run for {duration} seconds before exiting")
            end_time = time.time() + duration
        
        try:
            while self.running:
                self.publish_sensor_data()
                time.sleep(self.interval)
                
                if duration and time.time() >= end_time:
                    logger.info(f"Duration of {duration} seconds reached, shutting down...")
                    self.running = False
        except KeyboardInterrupt:
            logger.info("Interrupted by user")
        except Exception as e:
            logger.error(f"Error in simulator loop: {e}")
        finally:
            self.stop()

    def stop(self):
        """Stop the sensor simulator and disconnect from broker."""
        self.running = False
        if self.client:
            self.client.loop_stop()
            self.client.disconnect()
        logger.info("Sensor simulator stopped")


def signal_handler(signum, frame):
    """Handle SIGINT (Ctrl+C) gracefully."""
    logger.info("Received interrupt signal, shutting down...")
    sys.exit(0)


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description="MQTT Sensor Simulator - Emulates IoT sensor devices for testing"
    )
    parser.add_argument(
        "--host",
        default="localhost",
        help="MQTT broker host (default: localhost)"
    )
    parser.add_argument(
        "--port",
        type=int,
        default=1883,
        help="MQTT broker port (default: 1883)"
    )
    parser.add_argument(
        "--rooms",
        type=int,
        default=5,
        help="Number of rooms to simulate (default: 5)"
    )
    parser.add_argument(
        "--interval",
        type=int,
        default=2,
        help="Interval in seconds between sensor readings (default: 2)"
    )
    parser.add_argument(
        "--prefix",
        default="room",
        help="Prefix for room names (default: room)"
    )
    parser.add_argument(
        "--debug",
        action="store_true",
        help="Enable debug logging"
    )
    parser.add_argument(
        "--duration",
        type=int,
        default=None,
        help="Duration in seconds to run the simulator before exiting (optional)"
    )

    args = parser.parse_args()

    if args.debug:
        logging.getLogger().setLevel(logging.DEBUG)

    # Set up signal handler for graceful shutdown
    signal.signal(signalduration=args.duration.SIGINT, signal_handler)

    # Create and start simulator
    simulator = SensorSimulator(
        broker_host=args.host,
        broker_port=args.port,
        num_rooms=args.rooms,
        interval=args.interval,
        room_prefix=args.prefix
    )

    try:
        simulator.connect()
        simulator.start()
    except Exception as e:
        logger.error(f"Fatal error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
