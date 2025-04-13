#!/bin/bash

services=("echo" "auth" "userData" "social" "api_gw")
pids=()

mkdir -p logs

for service in "${services[@]}"; do
  mkdir -p "logs/$service"
  echo "Starting $service..."

  ./gradlew "$service:run" \
    > "logs/$service/stdout.log" \
    2> "logs/$service/stderr.log" &

  pids+=($!)
  sleep .1
done

echo "All services are running in background."
echo "Press Ctrl+D to stop all services and bring them to foreground..."

cat

echo -e "\nStopping all services..."

for pid in "${pids[@]}"; do
  echo "Killing PID $pid"
  kill "$pid"
  sleep .1
done

echo "All services stopped."
