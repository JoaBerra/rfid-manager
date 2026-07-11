---
title: Utvecklingsmiljö — fakir
tags: [fakir, arch-linux, android-sdk, omarchy, setup]
created: 2026-07-11
updated: 2026-07-11
---

# Utvecklingsmiljö — fakir

Workstation **fakir** (Arch Linux, Hyprland, RTX 3090). Repo: `/home/joakim/Projects/rfid-manager/`.

## Android SDK (user-local, utan sudo)

Installerad 2026-07-11 via Google command-line tools (alternativ till AUR `android-studio`):

| Fält | Värde |
|------|-------|
| `ANDROID_HOME` | `/home/joakim/Android/Sdk` |
| `sdk.dir` | samma — se `RFIDManager/local.properties` |
| Platform | `android-36` |
| Build-tools | `36.0.0` |
| cmdline-tools | `~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager` |

### Snabbverifiering

```bash
export ANDROID_HOME=~/Android/Sdk
cd ~/Projects/rfid-manager/RFIDManager
./gradlew assembleDebug
```

**Status 2026-07-11:** `BUILD SUCCESSFUL` (~61 s, första körning).

## Android Studio (valfritt)

Full IDE via AUR när sudo finns:

```bash
sudo pacman -S android-studio
```

SDK ovan räcker för Gradle-byggen från terminal/Cursor.

## Lokal AI (Utvecklare-roll)

- Ollama `qwen2.5-coder:32b` på `127.0.0.1:11434`
- Organisation: `andra-hjarna/Organisation/Processer/ollama-fakir.md`

## Relaterat

- [[Utvecklingsmiljö]] — generell Arch/Omarchy-guide
- [[Android-Studio-Installation]] — AUR-installation med Hyprland-fixar
- Andra Hjärnan: `Kunskapsbas/Projekt/rfid-manager.md`