---
title: Utvecklingsmiljö
tags: [android-studio, arch-linux, omarchy, kotlin, gratis]
created: 2026-05-26
---

# Utvecklingsmiljö

## Android Studio

**Android Studio** är Googles officiella, kostnadsfria Integrated Development Environment för Android-utveckling.

### Kostnad
- Helt gratis för själva verktyget.
- Inga dolda licensavgifter.

### Deployment / Distribution
- **Sideloading** (installera APK direkt på egna enheter via USB-felsökning eller filöverföring) → **helt gratis**.
- Endast vid publicering på Google Play Store tillkommer en engångsavgift på **25 USD**.

### Installation på Arch Linux + Omarchy
- Rekommenderat: Installera via AUR med paketet `android-studio` — se [[Android-Studio-Installation]] för komplett steg-för-steg-guide inklusive Omarchy/Hyprland-specifika fixar.
- Alternativ: Ladda ner fristående tar.gz direkt från Googles utvecklarportal.

### Fördelar i denna kontext
- Fullt stöd för Kotlin.
- Utmärkt emulator + USB-debugging.
- Inbyggt stöd för NFC-taggar i testmiljö (kan simulera taggar i viss utsträckning).
- Stark integration med Git och moderna build-verktyg (Gradle).

## Kotlin

- Rekommenderat språk för all ny Android-utveckling.
- Modern, koncist, null-säkert och utmärkt interoperabilitet med Java-bibliotek (inklusive Android NFC API).

## Relaterat

- [[Projektplan]] (avsnitt 1)
- [[Projektöversikt]]
- [[Verktyg-och-Setup]] (kommer att utökas med specifika Omarchy-konfigurationer och AUR-paket)