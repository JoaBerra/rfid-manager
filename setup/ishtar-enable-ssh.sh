#!/bin/bash
# Kör på ishtar (Proxmox Console), inte på fakir.
# Installerar och startar OpenSSH-server.

set -e
sudo apt update
sudo apt install -y openssh-server
sudo systemctl enable --now ssh
echo "--- SSH status ---"
sudo systemctl is-active ssh
ss -tlnp | grep ':22' || true
echo "Klart. Nästa: lägg till fakir-nyckel (se setup/ishtar-ssh-setup.md)"