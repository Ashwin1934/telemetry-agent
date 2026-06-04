# ESP32 BME280 MQTT Telemetry Setup Guide

## Overview
This Arduino sketch runs on two ESP32 dev kits, each with a BME280 sensor, publishing temperature/humidity/pressure data to an MQTT broker every 2 seconds.

## Hardware Requirements
- 2x ESP32 Development Kits
- 2x BME280 Temperature/Humidity/Pressure Sensors
- USB cables for programming
- WiFi network access
- MQTT broker (local or cloud)

## Wiring (I2C)
Connect BME280 to ESP32 using I2C protocol:

```
BME280 PIN    ESP32 PIN    Description
GND           GND          Ground
3.3V          3.3V         Power
SCL           GPIO 22      I2C Clock
SDA           GPIO 21      I2C Data
```

**Note:** Verify your BME280 I2C address beforehand. Most use `0x77` or `0x76`.

## Arduino IDE Setup

### 1. Install ESP32 Board Support
1. Open Arduino IDE
2. Go to **File → Preferences**
3. Add to "Additional Board Manager URLs": 
   ```
   https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
   ```
4. Go to **Tools → Board → Board Manager**
5. Search for "esp32" and install by Espressif Systems

### 2. Install Required Libraries
In Arduino IDE, go to **Sketch → Include Library → Manage Libraries** and install:

1. **Adafruit BME280 Library** (by Adafruit)
   - Used for sensor communication

2. **Adafruit Unified Sensor** (by Adafruit)
   - Dependency for BME280 library

3. **PubSubClient** (by Nick O'Leary)
   - Used for MQTT communication

### 3. Select Board and Port
- **Tools → Board → ESP32 Arduino → ESP32 Dev Module**
- **Tools → Port → COMx** (your ESP32's serial port)

## Configuration

### Before Uploading
Edit these values in the sketch:

```cpp
// WiFi Configuration
const char* WIFI_SSID = "YOUR_SSID";
const char* WIFI_PASSWORD = "YOUR_PASSWORD";

// MQTT Configuration
const char* MQTT_BROKER = "192.168.1.100";  // Your broker IP
const int MQTT_PORT = 1883;
const char* MQTT_CLIENT_ID = "ESP32_Device_1";  // Change to "ESP32_Device_2" for second
const char* MQTT_USERNAME = "";  // If your broker requires auth
const char* MQTT_PASSWORD = "";  // If your broker requires auth

// Sensor Configuration
const char* ROOM_NAME = "Room1";  // Change to "Room2" for second device

// Publishing interval (milliseconds)
unsigned long PUBLISH_INTERVAL = 2000;  // 2 seconds

// BME280 I2C Address
const uint8_t BME280_I2C_ADDRESS = 0x77;  // Check with I2C scanner if unsure
```

### Find Your BME280 I2C Address
If unsure about your sensor's I2C address, upload this sketch first:

```cpp
#include <Wire.h>

void setup() {
  Serial.begin(115200);
  Wire.begin();
}

void loop() {
  for (uint8_t address = 1; address < 127; address++) {
    Wire.beginTransmission(address);
    if (Wire.endTransmission() == 0) {
      Serial.print("Device found at 0x");
      Serial.println(address, HEX);
    }
  }
  delay(5000);
}
```

## MQTT Topics

The sketch publishes sensor data as a JSON object to a single topic:

### For Room1:
- `telemetry/Room1/sensorData` → JSON object with all sensor readings
  ```json
  {"temperature": 22.45, "humidity": 55.30, "pressure": 1013.25}
  ```

### For Room2:
- `telemetry/Room2/sensorData` → JSON object with all sensor readings
  ```json
  {"temperature": 23.12, "humidity": 52.88, "pressure": 1012.98}
  ```

### Subscribed Command Topics:
- `telemetry/Room1/commands` (Room1)
- `telemetry/Room2/commands` (Room2)

## Testing with MQTT

### Using MQTT Explorer (GUI)
1. Download from [http://mqtt-explorer.com/](http://mqtt-explorer.com/)
2. Connect to your broker
3. Subscribe to `telemetry/#` to see all data

### Using mosquitto_sub (CLI)
```bash
mosquitto_sub -h YOUR_BROKER_IP -t "telemetry/#" -v

# Example output:
telemetry/Room1/sensorData {"temperature":22.45,"humidity":55.30,"pressure":1013.25}
telemetry/Room2/sensorData {"temperature":23.12,"humidity":52.88,"pressure":1012.98}
```

## Uploading to ESP32
1. Connect ESP32 via USB
2. Open the `.ino` file in Arduino IDE
3. Configure WiFi, MQTT, and Room settings in the code
4. Click **Upload** (Ctrl+U / Cmd+U)
5. Monitor serial output via **Tools → Serial Monitor**

## Serial Monitor Output
Expected output when running:
```
=== ESP32 BME280 MQTT Telemetry Agent ===
Room: Room1
Initializing BME280 sensor...
BME280 sensor initialized successfully.
Connecting to WiFi: YOUR_SSID
WiFi connected!
IP address: 192.168.1.50
Connecting to MQTT broker: 192.168.1.100
MQTT connected!
Subscribed to: telemetry/Room1/commands
[Room1] {"temperature":22.45,"humidity":55.30,"pressure":1013.25}
[Room1] {"temperature":22.46,"humidity":55.28,"pressure":1013.24}
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Could not find BME280 sensor" | Check I2C wiring, verify address (try both 0x76 and 0x77), ensure 3.3V power |
| WiFi connects but MQTT fails | Verify MQTT broker IP/port, check firewall, confirm broker is running |
| No serial output | Check USB cable, verify board is selected in Tools menu, press ESP32 reset button |
| Data not publishing | Check MQTT credentials, verify broker connectivity, monitor logs in Tools → Serial Monitor |

## Customization
- **Change interval dynamically:** Call `setPublishInterval(intervalMs)` with new value in milliseconds
- **Add more sensors:** Duplicate sensor reading logic with different I2C addresses or use multiplexer
- **Change MQTT structure:** Modify topic paths in `publishSensorData()` function

## MQTT Broker Options
- **Local:** Mosquitto (raspberrypi.org or docker)
- **Cloud:** HiveMQ Cloud (free tier available)
- **Docker:** `docker run -d -p 1883:1883 eclipse-mosquitto`

