#!/bin/bash
# Start RFID MQTT dashboard on ishtar (broker must already run on :1883)
set -e
cd ~/Projects/rfid-manager/dashboard
docker-compose -f docker-compose.ishtar.yml up -d --build
docker-compose -f docker-compose.ishtar.yml ps
echo "Dashboard: http://192.168.50.151:8000"