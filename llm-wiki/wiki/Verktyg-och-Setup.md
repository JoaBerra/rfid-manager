---
title: Verktyg och Setup
tags: [verktyg, setup, omarchy, arch-linux, aur, android-studio]
created: 2026-05-26
---

# Verktyg och Setup

Hub för alla verktyg, konfigurationer och installationsguider relaterade till projektets utvecklingsmiljö.

## Huvudverktyg

- **Android Studio** — Se [[Android-Studio-Installation]] för detaljerad AUR-installation på Omarchy.
- **Kotlin** — Ingår i Android Studio.
- **AUR Helpers** — `paru` eller `yay` (se installationsguiden).

## Omarchy-specifika Konfigurationer

- Hyprland window rules för JetBrains/Android Studio (fokus, popups, Wayland).
- Miljövariabler (`_JAVA_AWT_WM_NONREPARENTING`).

## Markdown → PDF (för felrapporter, dokumentation m.m.)

Vi använder **pandoc + weasyprint** som standardverktyg för att generera professionella PDF:er från Markdown-filer (t.ex. buggrapporter).

### Installation

```bash
sudo pacman -S pandoc
yay -S python-weasyprint     # eller paru -S python-weasyprint
```

**Viktigt:** Paketet heter `python-weasyprint` i de officiella repon (inte "weasyprint").

### Användning

Vi har ett litet wrapper-script på `~/.local/bin/md-to-pdf`:

```bash
md-to-pdf wiki/bugs/2026-06-07-mqtt-socket-epem-samsung-note10.md
```

Det genererar som standard till `~/<filnamn>.pdf`.

Direkt med pandoc:

```bash
pandoc input.md -o output.pdf --pdf-engine=weasyprint -V geometry:margin=2cm
```

### Varför detta verktyg?

- Gratis och öppen källkod.
- Utmärkt stöd för svenska tecken, kodblock, listor och tabeller.
- Enkelt att ha som fast del av utvecklingsmiljön.
- Pandoc är de-facto standard för dokumentkonvertering.

Se också `~/.local/bin/md-to-pdf` (wrapper-script som skapades som del av denna miljö).
- Emulator-kompatibilitet på Wayland.

## Kommande Sidor

- Specifika Hyprland-konfigurationssnuttar för Android-utveckling
- Git + Gradle + version control setup
- ADB / USB-debugging konfiguration för fysiska enheter (särskilt NFC-test)
- Backup-strategi för SDK och AVD:er

## Relaterat

- [[Utvecklingsmiljö]]
- [[Android-Studio-Installation]]
- [[Projektplan]]

Denna sida kommer att växa i takt med att vi installerar och konfigurerar fler verktyg.