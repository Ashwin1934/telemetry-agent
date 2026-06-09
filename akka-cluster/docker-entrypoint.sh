#!/bin/bash

# Docker entrypoint script for Akka cluster nodes
# Passes NODE_ID to the application

NODE_ID=${NODE_ID:-1}
echo "Starting Akka cluster node with NODE_ID=$NODE_ID"

java $JVM_OPTS -cp akka-cluster-app.jar com.telemetry.ClusterNode "$NODE_ID"
