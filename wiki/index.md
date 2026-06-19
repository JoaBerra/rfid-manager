---
title: Projekt RF-ID Applikationer på Android
tags: [projekt, android, rfid, nfc, kotlin, eskortminne, arklinux]
created: 2026-05-26
---

# Projekt RF-ID Applikationer på Android

> **LLM Wiki** för utveckling av kostnadsfria Android-applikationer i Kotlin med fokus på RFID/NFC-integration — särskilt läsning och hantering av eskortminnen.  
> Miljö: Arch Linux + Omarchy + Android Studio. Helt utan licens- eller distributionskostnader. AI-verktyg som primärt stöd i byggnationen.

Denna wiki följer strikt Karpathy LLM Wiki pattern (se [schema.md](/home/joakim/llm-wiki/schema.md)) och underhålls av LLM som wiki-maintainer.

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
- [[Hardware-Testenheter]] — Detaljer om fysiska testtelefoner (t.ex. Galaxy Note 10 SM-N970F/DS).
- [[App-Architecture]] — Övergripande arkitektur för RFID Manager. Uppgraderad med Fas 2 (MQTT, persistens) + Fas 3 (navigation, ViewModels, spacing).
- [[Figma-to-Compose]] — Hur Figma-designen översatts till Jetpack Compose (design tokens, tema, manuell implementation).
- [[Nomenclature-Figma-Android]] — Namnsättning / nomenclature för Figma-komponenter, variabler och Android/Kotlin (återanvänds 1:1 av AI-assistenten i design + kod).
- [[Fas2-Implementation-Overview]] — Översikt över filer, struktur och steg för Fas 2.
- [[Fas3-Implementation-Plan]] — Fullständig plan för Fas 3: navigation, ViewModels, spacing, Room-enablement, polish och PC-stöd. Alla steg slutförda och sign-offade.
- [[Fas3-Navigation-Spacing-Design]] — Design-dokument för navigation + breathing room-spacing.
- [[Fas4-Implementation-Plan]] — Plan och resultat för Fas 4: lokalisering, textstorlek, sök, export, haptik, dark mode, MQTT, paginering. Alla 8 punkter ✅ godkända.
- [[Figma-Design-Spec-Fas2]] — Full "Developer Handoff Document" från Figma AI.
- [[Figma-Prototype-Fas2-Proof]] — Detaljerad Figma-spec för mobil prototyp-app.
- [[Figma-Steps-Fas2-Build-Guide]] — Steg-för-steg guide för att bygga prototypen i Figma.
- [[Projektrapport]] — Fullständig projektrapport: vad som gjorts, teknikstack, milstolpar.

### Verktyg, Resurser och Process
- [[Verktyg-och-Setup]] — Omarchy-specifika konfigurationer, Android Studio AUR-paket, Git, AI-assistenter.
- [[Android-Studio-Installation]] — Fullständig installationsdokumentation (primär referens för detta steg).
- [[MQTT-Infrastruktur]] — Fördjupning (Fas-100): MQTT-protokollet, broker, topologi, topics, meddelandeformat, verktyg och hela dataflödet.
- [[Fas-101-MQTT-Configuration]] — Praktisk implementation (Fas-101): Fullständig MQTT-konfiguration i appens Settings-skärm (host, port, TLS, auth, Sparkplug, topics, beteende).
- [[Fas-200-Web-Dashboard]] — Realtidsdashboard (Fas-200): Webbaserad visualisering av MQTT-meddelanden med FastAPI + SSE + Docker Compose.
- [[MQTT-Explorer]] — Gratis GUI-verktyg för att inspektera MQTT-meddelanden i realtid.
- [[MQTT-Manual]] — Praktisk bruksanvisning: starta broker, dashboard, skicka data från telefonen, felsökning.

### Projektstyrning, Status och Arbetsätt
- [[Kanban]] — Visuellt Kanban-board: vad som är kvar, pågår, klart och blockerat. Uppdateras löpande av AI-assistenten.
- [[Rollfördelning-och-Arbetsätt]] — Explicit rollbaserat samarbete (Kund / Projektledare / Arkitekt / Technical Lead / Programmerare / Testare / DevOps / Wiki Curator). Working Agreement för 1-människa + AI-team. Uppdaterad 2026-06-06.
- [[Kundrelationer-och-Acceptans]] — Formell dokumentation av relationer till Kund, UAT-tester utförda i kundrollen samt tidsstämplade godkännanden (sign-off) per fas. Innehåller mallar och den aktuella Fas 2 UAT-godkännandet 2026-06-07.
- [[Produkt-Roadmap]] — Översikt över slutfört (Fas 2–4), planerat (Fas 5–6) och framtida features. Single source of truth för roadmap + backlog. Uppdaterad 2026-06-11. Länkar även till ny samlad struktur under `~/rfid-manager/`.

### Felrapporter (Bugs)
- `bugs/` — Katalog för formella felrapporter.
- [[bugs/2026-06-07-mqtt-socket-epem-samsung-note10]] — **EPERM på rå TCP-socket för MQTT (Paho) från debug-app på Samsung Galaxy Note 10 (SM-N970F)**. Fullständig felrapport med miljö, reproduktionssteg, Logcat, alla försökta inställningar, PC-side validering och hypoteser. Avsedd för second opinion (t.ex. Gemini).

## Arbetsflöde (enligt schema)

1. Ny fil läggs i `raw/`
2. LLM läser, diskuterar nyckelpunkter
3. Skapar/uppdaterar wiki-sidor (entiteter, koncept, sammanfattningar)
4. Uppdaterar denna `index.md`
5. Lägger till backlinks och stärker grafen
6. Loggar i [[log]]

## Status

**Fas 1–6 slutförda (v1.0 releasad).** Initial struktur (Fas 1) 2026-05-26. Eskortminne-läs/skriv (Fas 2) klar 2026-06-04. UI-reallokering, navigation, ViewModels, spacing, polish och PC-stöd (Fas 3) sign-off av Kund 2026-06-10. Lokalisering, inställningar och användbarhet (Fas 4) sign-off av Kund 2026-06-11. Dokumentation, kvalitet och radar-estetik (Fas 5) + 1.0 Release (Fas 6) klara 2026-06-13.

**Fas-100 (MQTT-infrastruktur):** Pågående fördjupning. Se [[MQTT-Infrastruktur]].

**Fas-101 (MQTT-klientkonfiguration):** Planerad. Fullständig konfiguration av anslutning, TLS, autentisering, Sparkplug och beteende direkt i appen. Se [[Fas-101-MQTT-Configuration]].

**Fas-200 (MQTT Realtidsdashboard):** Implementerad och godkänd av Kund ✅. Webbaserad dashboard med FastAPI + SSE för realtidsvisualisering av MQTT-meddelanden. Docker Compose för enkel demo. Se [[Fas-200-Web-Dashboard]].

**MQTT-Manual:** Praktisk bruksanvisning för hela MQTT-kedjan: broker, dashboard, kommandon, felsökning. Se [[MQTT-Manual]].

**GitHub:** https://github.com/JoaBerra/rfid-manager

Se [[log]] för full historik. Mer detaljer i:
- [[Projektrapport]] (Fas 1–2)
- [[Fas3-Implementation-Plan]] (Fas 3)
- [[Fas4-Implementation-Plan]] (Fas 4)
- [[Kundrelationer-och-Acceptans]] (UAT + sign-off)