#!/bin/bash
# Installera MQTT Explorer AppImage på fakir (engångs).
set -euo pipefail
VERSION="0.3.5"
DEST="${HOME}/.local/opt/mqtt-explorer"
URL="https://github.com/thomasnordquist/MQTT-Explorer/releases/download/v${VERSION}/MQTT-Explorer-${VERSION}.AppImage"
mkdir -p "$DEST"
curl -fsSL -o "${DEST}/MQTT-Explorer-${VERSION}.AppImage" "$URL"
chmod +x "${DEST}/MQTT-Explorer-${VERSION}.AppImage"
ln -sf "${DEST}/MQTT-Explorer-${VERSION}.AppImage" "${HOME}/.local/bin/mqtt-explorer"
echo "Installerat: mqtt-explorer"
echo "Starta: mqtt-explorer"
echo "Broker: 192.168.50.151:1883 (ishtar)"