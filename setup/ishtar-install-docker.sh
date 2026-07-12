#!/bin/bash
# Kör på ishtar: ssh hulda
# Kräver sudo-lösenord (interaktivt).

set -e
sudo apt-get update
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y docker.io docker-compose-plugin git
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"
echo ""
echo "Docker installerat. Logga ut och in igen (eller: newgrp docker)"
echo "Test: docker ps"
echo "MQTT: cd ~/Projects/rfid-manager/test/fas2-mqtt && docker compose -f docker-compose.hulda.yml up -d"