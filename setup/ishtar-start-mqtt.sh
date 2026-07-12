#!/bin/bash
# Starta MQTT-broker på ishtar (efter Docker-installation)
set -e
cd ~/Projects/rfid-manager/test/fas2-mqtt
docker-compose -f docker-compose.hulda.yml up -d
docker-compose -f docker-compose.hulda.yml ps
echo "Broker: 192.168.50.151:1883"