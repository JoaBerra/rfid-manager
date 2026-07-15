#!/usr/bin/env bash
# Installera pCloud Drive på fakir (AppImage 2.1.1, extraherad — som MQTT Explorer).
# Kräver fuse2 (FUSE2 + fusermount) för virtuell enhet ~/pCloudDrive. Inget sudo för själva appen.
set -euo pipefail

VERSION="2.1.1"
PUBLINK_CODE="XZtwII5Zjf5noLYtDwJ1qkyAXaqujuvVKBbX"
DEST="${HOME}/.local/opt/pcloud"
APPIMAGE="${DEST}/pCloud-${VERSION}.AppImage"
LAUNCHER="${DEST}/pcloud-launcher.sh"

mkdir -p "$DEST"

download_appimage() {
  local api host path tag url
  api=$(curl -fsSL "https://api.pcloud.com/getpublinkdownload?code=${PUBLINK_CODE}&forcedownload=1")
  host=$(echo "$api" | python3 -c "import sys,json; print(json.load(sys.stdin)['hosts'][0])")
  path=$(echo "$api" | python3 -c "import sys,json; print(json.load(sys.stdin)['path'])")
  tag=$(echo "$api" | python3 -c "import sys,json; print(json.load(sys.stdin)['dwltag'])")
  url="https://${host}${path}?dwltag=${tag}"
  echo "Laddar ner pCloud ${VERSION}..."
  curl -fL --progress-bar -o "$APPIMAGE" "$url"
  chmod +x "$APPIMAGE"
}

if [[ ! -f "$APPIMAGE" ]]; then
  download_appimage
fi

if [[ ! -x "${DEST}/squashfs-root/AppRun" ]]; then
  echo "Extraherar AppImage..."
  (cd "$DEST" && ./"pCloud-${VERSION}.AppImage" --appimage-extract)
fi

cat > "$LAUNCHER" <<'EOF'
#!/usr/bin/env bash
# Use pcloud wrapper (sets LD_LIBRARY_PATH for libfuse/libpsynclib in resources/)
ROOT="${HOME}/.local/opt/pcloud/squashfs-root"
exec "${ROOT}/pcloud" "$@"
EOF
chmod +x "$LAUNCHER"
ln -sf "$LAUNCHER" "${HOME}/.local/bin/pcloud"

echo ""
echo "Installerat: pcloud"
echo "Starta: pcloud"
echo "Efter inloggning: virtuell enhet ~/pCloudDrive"
echo ""
if pacman -Q fuse2 &>/dev/null; then
  echo "fuse2: installerad (krävs för Drive-mount)"
else
  echo "VARNING: fuse2 saknas — installera: sudo pacman -S fuse2"
  echo "  (pCloud använder FUSE2/libfuse.so.2; fusermount3 räcker inte)"
fi