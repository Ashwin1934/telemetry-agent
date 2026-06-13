FROM openjdk:11-jre-slim

WORKDIR /app

# Copy the MCP server jar
COPY mcp-server/target/mcp-server.jar /app/mcp-server.jar

# Create logs directory
RUN mkdir -p /app/logs

# Expose HTTP MCP endpoint
EXPOSE 8080

# Expose Akka remoting port
EXPOSE 2553

ENTRYPOINT ["java", "-jar", "/app/mcp-server.jar"]
