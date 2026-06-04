---
title: Projekt RF-ID Applikationer på Android
tags: [projekt, android, rfid, nfc, kotlin, eskortminne, arklinux]
created: 2026-05-26
---

# Projekt RF-ID Applikationer på Android

> **LLM Wiki** för utveckling av kostnadsfria Android-applikationer i Kotlin med fokus på RFID/NFC-integration — särskilt läsning och hantering av eskortminnen.  
> Miljö: Arch Linux + Omarchy + Android Studio. Helt utan licens- eller distributionskostnader. AI-verktyg som primärt stöd i byggnationen.

Denna wiki följer strikt [[Karpathy LLM Wiki pattern]] (se [schema.md](/home/joakim/llm-wiki/schema.md)) och underhålls av LLM som wiki-maintainer.

## Projektmål

- Utveckla praktiska Android-appar för industriella RF-ID-uppgifter (primärt läsa av eskortminne).
- Använda enbart **gratis** verktyg och deployment-metoder (sideloading av APK via USB).
- Programmeringsspråk: **Kotlin**.
- Utvecklingsmiljö: Android Studio på Arch Linux/Omarchy.
- Hårdvarukrav: Android-enheter med inbyggt NFC-chip kompatibelt med 13.56 MHz HF RFID.

## Innehållskatalog

### Projekt och Översikt
- [[Projektöversikt]] — Mål, begränsningar (kostnadsfri, specifik miljö), scope och varför detta projekt.
- [[Projektplan]] — Den strukturerade planen: utvecklingsmiljö, RFID/NFC-integration, AI-stöd och rekommendationer.

### Utvecklingsmiljö
- [[Utvecklingsmiljö]] — Android Studio som gratis IDE, installation på Arch Linux via AUR, kostnad för verktyg vs. Play Store, sideloading.
- [[Android-Studio-Installation]] — Detaljerad AUR-installationsguide med starkt fokus på Omarchy + Hyprland (Wayland-fixar, consent dialog, window rules, KVM, etc.).
- [[Verktyg-och-Setup]] — Hub för AUR helpers, Omarchy-konfigurationer och relaterade verktyg.

### RFID/NFC och Eskortminne
- [[RFID-och-NFC]] — Teknikskillnader: HF (13.56 MHz) vs UHF (860-960 MHz), Androids inbyggda stöd, när extern Bluetooth/USB-läsare behövs.
- [[Eskortminne]] — Definition, vanliga användningsfall i industrin, minnesstruktur och varför det är en vanlig uppgift.
- [[Android-NFC-API]] — Kärnklasser: `NfcAdapter`, `Tag`, `MifareClassic`, `NfcA`, `IsoDep`. Läs- och skrivoperationer mot specifika sektorer.

### Implementation och Kod
- [[Kotlin-Android-NFC]] — Mönster för implementation, permissions, foreground dispatch, hantering av taggar, exempel på sektorläsning.
- [[Hårdvarukrav-och-Enheter]] — Rekommendationer för test- och produktionsenheter med kompatibelt NFC.
- [[Hardware-Testenheter]] — Detaljer om fysiska testtelefoner (t.ex. Galaxy Note 10 SM-N970F/DS).
- [[App-Architecture]] — Övergripande arkitektur för RFID Manager (UI / Domain / NFC-lager, RFIDManagerScreen, etc.).
- [[Figma-to-Compose]] — Hur Figma-designen översatts till Jetpack Compose (design tokens, tema, manuell implementation).
- [[Projektrapport]] — Fullständig projektrapport: vad som gjorts, tidsperiod (2026-05-26→06-04), teknikstack, Architecture-Design-Källkod-förankring, milstolpar (page 12 write etc.).

### Verktyg, Resurser och Process
- [[Verktyg-och-Setup]] — Omarchy-specifika konfigurationer, Android Studio AUR-paket, Git, AI-assistenter.
- [[Android-Studio-Installation]] — Fullständig installationsdokumentation (primär referens för detta steg).
- [[Källor]] — Register över alla raw-filer som ingesterats (tillsammans med backlinks till bearbetade sidor).
- [[Frågor-och-Svar]] — Samling av vanliga frågor som uppstått under utvecklingen.

## Arbetsflöde (enligt schema)

1. Ny fil läggs i `raw/`
2. LLM läser, diskuterar nyckelpunkter
3. Skapar/uppdaterar wiki-sidor (entiteter, koncept, sammanfattningar)
4. Uppdaterar denna `index.md`
5. Lägger till backlinks och stärker grafen
6. Loggar i [[log]]

## Status

**Initial struktur skapad** 2026-05-26. Inga raw-filer ingesterade ännu. Redo att börja med första källor.

Se [[log]] för full historik över ändringar och ingests.