#!/bin/bash

# Variables
REMOTE_USER="pawel"
REMOTE_HOST="vypas.com"
REMOTE_PORT="1222"
REMOTE_DIR="~/audio/container/server"
K8S_CONFIG_FILE="config.yml"

# Build the fat JAR locally
echo "Building fat JAR..."
./gradlew clean fatJar

if [ $? -ne 0 ]; then
    echo "Error building JAR file. Exiting."
    exit 1
fi

# Find the fat JAR file
JAR_FILE=$(find build/libs -name "*-fat.jar" | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo "No fat JAR file found in build/libs/. Exiting."
    exit 1
fi

echo "Found JAR file: $JAR_FILE"

# Copy only the JAR file to the remote directory
echo "Copying JAR file to remote server..."
scp -P "$REMOTE_PORT" "$JAR_FILE" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR/app.jar"

if [ $? -ne 0 ]; then
    echo "Error during JAR file transfer. Exiting."
    exit 1
fi

# Run commands on the remote server
echo "Running commands on remote server..."
ssh -p "$REMOTE_PORT" "$REMOTE_USER@$REMOTE_HOST" <<EOF
    set -e
    echo "Building and pushing Docker images..."
    cd ~/audio/container
    docker compose build
    docker compose push

    echo "Rolling out Kubernetes deployments..."
    kubectl apply -f ~/audio/$K8S_CONFIG_FILE
    kubectl -n vaigu rollout restart deployment backend -n audio
EOF

if [ $? -ne 0 ]; then
    echo "Error during remote execution. Exiting."
    exit 1
fi

echo "Deployment completed successfully!"