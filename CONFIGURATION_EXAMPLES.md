/*
 * CONFIGURATION EXAMPLES
 * 
 * Use these as reference for setting up Device 1 and Device 2
 * Copy the relevant configuration section into the main ESP32_BME280_MQTT.ino
 */

// ============================================================================
// DEVICE 1 - ROOM 1 CONFIGURATION
// ============================================================================
/*
// WiFi Configuration
const char* WIFI_SSID = "MyNetwork";
const char* WIFI_PASSWORD = "MyPassword123";

// MQTT Configuration
const char* MQTT_BROKER = "192.168.1.100";
const int MQTT_PORT = 1883;
const char* MQTT_CLIENT_ID = "ESP32_Device_1";
const char* MQTT_USERNAME = "";
const char* MQTT_PASSWORD = "";

// Sensor Configuration
const char* ROOM_NAME = "Room1";

// Publishing interval in milliseconds
unsigned long PUBLISH_INTERVAL = 2000;  // 2 seconds

// BME280 I2C Address
const uint8_t BME280_I2C_ADDRESS = 0x77;
*/

// ============================================================================
// DEVICE 2 - ROOM 2 CONFIGURATION
// ============================================================================
/*
// WiFi Configuration
const char* WIFI_SSID = "MyNetwork";
const char* WIFI_PASSWORD = "MyPassword123";

// MQTT Configuration
const char* MQTT_BROKER = "192.168.1.100";
const int MQTT_PORT = 1883;
const char* MQTT_CLIENT_ID = "ESP32_Device_2";
const char* MQTT_USERNAME = "";
const char* MQTT_PASSWORD = "";

// Sensor Configuration
const char* ROOM_NAME = "Room2";

// Publishing interval in milliseconds
unsigned long PUBLISH_INTERVAL = 2000;  // 2 seconds

// BME280 I2C Address
const uint8_t BME280_I2C_ADDRESS = 0x77;
*/

// ============================================================================
// WITH MQTT AUTHENTICATION EXAMPLE
// ============================================================================
/*
const char* MQTT_BROKER = "broker.hivemq.com";
const int MQTT_PORT = 1883;
const char* MQTT_CLIENT_ID = "ESP32_Device_1";
const char* MQTT_USERNAME = "your_username";
const char* MQTT_PASSWORD = "your_password";
*/

// ============================================================================
// CUSTOM PUBLISH INTERVALS
// ============================================================================
/*
// Publish every 1 second (1000 ms) - More frequent updates
unsigned long PUBLISH_INTERVAL = 1000;

// Publish every 5 seconds (5000 ms) - Less frequent, lower bandwidth
unsigned long PUBLISH_INTERVAL = 5000;

// Publish every 30 seconds (30000 ms) - Energy efficient
unsigned long PUBLISH_INTERVAL = 30000;

// Publish every 60 seconds (60000 ms) - Low bandwidth
unsigned long PUBLISH_INTERVAL = 60000;
*/

// ============================================================================
// I2C ADDRESS REFERENCE
// ============================================================================
/*
Standard BME280 I2C addresses:
- 0x76 (Pin SDO connected to GND)
- 0x77 (Pin SDO connected to 3.3V or left floating)

If unsure, run the I2C scanner sketch provided in SETUP_GUIDE.md
*/

// ============================================================================
// MQTT TOPIC STRUCTURE
// ============================================================================
/*
Published Topics:
- telemetry/Room1/sensorData     (JSON: {"temperature":22.45,"humidity":55.30,"pressure":1013.25})
- telemetry/Room2/sensorData     (JSON: {"temperature":23.12,"humidity":52.88,"pressure":1012.98})

Subscribed Topics:
- telemetry/Room1/commands
- telemetry/Room2/commands

JSON Payload Format:
{
  "temperature": <float>,   // Temperature in Celsius
  "humidity": <float>,      // Relative humidity in %
  "pressure": <float>       // Atmospheric pressure in hPa
}
*/

// ============================================================================
// ADVANCED: CUSTOM SETTINGS FOR SPECIFIC USE CASES
// ============================================================================

// Energy-conscious configuration (battery/solar powered)
/*
const char* WIFI_SSID = "MyNetwork";
const char* WIFI_PASSWORD = "MyPassword123";
const char* MQTT_BROKER = "192.168.1.100";
const int MQTT_PORT = 1883;
const char* MQTT_CLIENT_ID = "ESP32_Device_1_LowPower";
const char* ROOM_NAME = "Room1";
unsigned long PUBLISH_INTERVAL = 30000;  // Publish every 30 seconds
*/

// High-frequency monitoring (research/testing)
/*
const char* WIFI_SSID = "MyNetwork";
const char* WIFI_PASSWORD = "MyPassword123";
const char* MQTT_BROKER = "192.168.1.100";
const int MQTT_PORT = 1883;
const char* MQTT_CLIENT_ID = "ESP32_Device_1_HighFreq";
const char* ROOM_NAME = "Room1";
unsigned long PUBLISH_INTERVAL = 500;  // Publish every 500ms
*/

// Cloud MQTT broker (HiveMQ Cloud)
/*
const char* MQTT_BROKER = "YOUR-HIVEMQ-CLOUD-URL.mqtt.cool";
const int MQTT_PORT = 8883;  // Use 8883 for secure TLS
const char* MQTT_CLIENT_ID = "esp32_device_1";
const char* MQTT_USERNAME = "your_hivemq_username";
const char* MQTT_PASSWORD = "your_hivemq_password";

Note: For TLS/SSL on port 8883, additional certificate handling is needed
*/
