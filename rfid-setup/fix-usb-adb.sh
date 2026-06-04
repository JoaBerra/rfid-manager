#!/bin/bash
set -e
echo "=== Fixing ADB/USB for Samsung on Arch ==="
echo "This will require sudo password."

sudo pacman -S --needed --noconfirm usbutils 2>/dev/null || sudo pacman -S --needed usbutils

echo "Creating plugdev group if missing..."
sudo groupadd plugdev 2>/dev/null || true

echo "Adding user $USER to plugdev..."
sudo usermod -aG plugdev "$USER"

echo "Installing udev rules..."
sudo mkdir -p /etc/udev/rules.d
sudo cp ~/rfid-setup/51-android.rules /etc/udev/rules.d/51-android.rules || sudo tee /etc/udev/rules.d/51-android.rules << 'RULES'
SUBSYSTEM=="usb", ATTR{idVendor}=="04e8", MODE="0666", GROUP="plugdev"
SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", MODE="0666", GROUP="plugdev"
RULES

echo "Reloading udev rules..."
sudo udevadm control --reload-rules
sudo udevadm trigger

echo "=== Host setup done ==="
echo "IMPORTANT: You must log out and log back in (or reboot) for the new group 'plugdev' to take effect in your session."
echo "The udev rules are active immediately for device permissions (MODE 0666 helps too)."
echo "Now plug in the phone and run: adb kill-server && adb devices"
