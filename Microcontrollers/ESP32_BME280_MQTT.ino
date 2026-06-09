/*
 * ESP32 BME280 MQTT Telemetry Agent
 * Publishes temperature, humidity, and pressure data from BME280 sensors to MQTT broker
 * Supports multiple ESP32 boards with different room configurations
 */

#include <WiFi.h>
#include <PubSubClient.h>
#include <Adafruit_BME280.h>
#include <Wire.h>

// ============================================================================
// CONFIGURATION SECTION - MODIFY THESE VALUES
// ============================================================================

// WiFi Configuration
const char* WIFI_SSID = "YOUR_SSID";
const char* WIFI_PASSWORD = "YOUR_PASSWORD";

// MQTT Configuration
const char* MQTT_BROKER = "YOUR_MQTT_BROKER_IP";
const int MQTT_PORT = 1883;
const char* MQTT_CLIENT_ID = "ESP32_Device_1";  // Change for each device
const char* MQTT_USERNAME = "";  // Leave empty if not required
const char* MQTT_PASSWORD = "";  // Leave empty if not required

// Sensor Configuration
const char* ROOM_NAME = "Room1";  // Change to "Room2" for second device

// Publishing interval in milliseconds (2 seconds = 2000 ms)
unsigned long PUBLISH_INTERVAL = 2000;

// BME280 I2C Address (0x76 or 0x77, check your sensor)
const uint8_t BME280_I2C_ADDRESS = 0x77;

// ============================================================================
// GLOBAL VARIABLES
// ============================================================================

WiFiClient espClient;
PubSubClient mqttClient(espClient);
Adafruit_BME280 bme280;
unsigned long lastPublishTime = 0;
bool sensorConnected = false;

// ============================================================================
// SETUP FUNCTION
// ============================================================================

void setup() {
  Serial.begin(115200);
  delay(2000);
  
  Serial.println("\n\n=== ESP32 BME280 MQTT Telemetry Agent ===");
  Serial.print("Room: ");
  Serial.println(ROOM_NAME);
  
  // Initialize BME280 sensor
  initializeSensor();
  
  // Connect to WiFi
  connectToWiFi();
  
  // Configure MQTT
  mqttClient.setServer(MQTT_BROKER, MQTT_PORT);
  mqttClient.setCallback(onMqttMessage);
}

// ============================================================================
// LOOP FUNCTION
// ============================================================================

void loop() {
  // Maintain WiFi connection
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi disconnected. Reconnecting...");
    connectToWiFi();
  }
  
  // Maintain MQTT connection
  if (!mqttClient.connected()) {
    connectToMqtt();
  }
  mqttClient.loop();
  
  // Publish sensor data at configured interval
  if (millis() - lastPublishTime >= PUBLISH_INTERVAL) {
    publishSensorData();
    lastPublishTime = millis();
  }
}

// ============================================================================
// SENSOR INITIALIZATION
// ============================================================================

void initializeSensor() {
  Serial.println("Initializing BME280 sensor...");
  
  if (!bme280.begin(BME280_I2C_ADDRESS)) {
    Serial.println("ERROR: Could not find BME280 sensor!");
    Serial.print("Attempted I2C address: 0x");
    Serial.println(BME280_I2C_ADDRESS, HEX);
    Serial.println("Check wiring and I2C address.");
    sensorConnected = false;
  } else {
    Serial.println("BME280 sensor initialized successfully.");
    sensorConnected = true;
  }
}

// ============================================================================
// WiFi CONNECTION
// ============================================================================

void connectToWiFi() {
  Serial.print("Connecting to WiFi: ");
  Serial.println(WIFI_SSID);
  
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 20) {
    delay(500);
    Serial.print(".");
    attempts++;
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\nWiFi connected!");
    Serial.print("IP address: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println("\nFailed to connect to WiFi!");
  }
}

// ============================================================================
// MQTT CONNECTION
// ============================================================================

void connectToMqtt() {
  Serial.print("Connecting to MQTT broker: ");
  Serial.println(MQTT_BROKER);
  
  if (MQTT_USERNAME[0] != '\0') {
    mqttClient.connect(MQTT_CLIENT_ID, MQTT_USERNAME, MQTT_PASSWORD);
  } else {
    mqttClient.connect(MQTT_CLIENT_ID);
  }
  
  if (mqttClient.connected()) {
    Serial.println("MQTT connected!");
    
    // Subscribe to command topics for this room
    String subscribeTopic = "telemetry/";
    subscribeTopic += ROOM_NAME;
    subscribeTopic += "/commands";
    mqttClient.subscribe(subscribeTopic.c_str());
    
    Serial.print("Subscribed to: ");
    Serial.println(subscribeTopic);
  } else {
    Serial.print("Failed to connect to MQTT. Error code: ");
    Serial.println(mqttClient.state());
  }
}

// ============================================================================
// PUBLISH SENSOR DATA
// ============================================================================

void publishSensorData() {
  if (!sensorConnected) {
    Serial.println("WARNING: Sensor not connected. Skipping publish.");
    return;
  }
  
  // Read sensor values
  float temperature = bme280.readTemperature();
  float humidity = bme280.readHumidity();
  float pressure = bme280.readPressure() / 100.0F;  // Convert to hPa
  
  // Create MQTT topic
  String topic = "telemetry/" + String(ROOM_NAME) + "/sensorData";
  
  // Create JSON payload
  String jsonPayload = "{";
  jsonPayload += "\"temperature\":" + String(temperature, 2);
  jsonPayload += ",\"humidity\":" + String(humidity, 2);
  jsonPayload += ",\"pressure\":" + String(pressure, 2);
  jsonPayload += "}";
  
  // Publish to MQTT
  mqttClient.publish(topic.c_str(), jsonPayload.c_str());
  
  // Log to serial
  Serial.print("[");
  Serial.print(ROOM_NAME);
  Serial.print("] ");
  Serial.println(jsonPayload);
}

// ============================================================================
// MQTT MESSAGE CALLBACK
// ============================================================================

void onMqttMessage(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message received on topic: ");
  Serial.println(topic);
  
  Serial.print("Payload: ");
  for (int i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
  }
  Serial.println();
  
  // Handle commands here if needed
  // Example: Change publish interval based on command
}

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

// Function to update publish interval (call from MQTT commands if needed)
void setPublishInterval(unsigned long intervalMs) {
  PUBLISH_INTERVAL = intervalMs;
  Serial.print("Publish interval updated to: ");
  Serial.print(PUBLISH_INTERVAL);
  Serial.println(" ms");
}
