FROM python:3.11-slim

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy simulator script
COPY sensor_simulator.py .

# Default: run for 10 minutes (600 seconds) before exiting
# Can be overridden with docker run -e DURATION=<seconds> or docker-compose
ENV DURATION=600
ENV MQTT_HOST=mosquitto
ENV MQTT_PORT=1883
ENV NUM_ROOMS=5
ENV INTERVAL=2

# Run the simulator
CMD ["python", "sensor_simulator.py", \
     "--host", "${MQTT_HOST}", \
     "--port", "${MQTT_PORT}", \
     "--rooms", "${NUM_ROOMS}", \
     "--interval", "${INTERVAL}", \
     "--duration", "${DURATION}"]
