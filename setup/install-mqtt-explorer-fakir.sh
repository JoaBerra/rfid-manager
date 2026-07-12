#!/bin/bash
# Installera MQTT Explorer på fakir (engångs).
# Arch: extraherar AppImage (fuse2 saknas ofta) — inget sudo krävs.
set -euo pipefail
VERSION="0.3.5"
DEST="${HOME}/.local/opt/mqtt-explorer"
URL="https://github.com/thomasnordquist/MQTT-Explorer/releases/download/v${VERSION}/MQTT-Explorer-${VERSION}.AppImage"
APPIMAGE="${DEST}/MQTT-Explorer-${VERSION}.AppImage"
LAUNCHER="${DEST}/mqtt-explorer-launcher.sh"
mkdir -p "$DEST"
if [[ ! -f "$APPIMAGE" ]]; then
  curl -fsSL -o "$APPIMAGE" "$URL"
  chmod +x "$APPIMAGE"
fi
# Extrahera (fungerar utan libfuse.so.2)
if [[ ! -x "${DEST}/squashfs-root/AppRun" ]]; then
  (cd "$DEST" && ./"MQTT-Explorer-${VERSION}.AppImage" --appimage-extract)
fi
cat > "$LAUNCHER" <<'EOF'
#!/bin/bash
ROOT="${HOME}/.local/opt/mqtt-explorer/squashfs-root"
exec "${ROOT}/AppRun" "$@"
EOF
chmod +x "$LAUNCHER"
ln -sf "$LAUNCHER" "${HOME}/.local/bin/mqtt-explorer"
echo "Installerat: mqtt-explorer (utan FUSE)"
echo "Starta: mqtt-explorer"
echo "Broker: 192.168.50.151:1883 (ishtar)"
echo ""
echo "Valfritt (native AppImage): sudo pacman -S fuse2"