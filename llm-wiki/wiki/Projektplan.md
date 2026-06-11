---
title: Projektplan
tags: [plan, rekommendationer, struktur]
created: 2026-05-26
---

# Projektplan

Nedan följer den strukturerade planen och rekommendationerna för utvecklingsmiljö, RFID-integration och AI-stöd.

## 1. Utvecklingsmiljö: Android Studio

Google erbjuder **Android Studio** som officiell, kostnadsfri IDE.

- **Kostnad**: Helt gratis. Inga dolda licensavgifter.
- **Deployment**: 
  - Direktinstallation på egna enheter ("sideloading") via APK eller USB-felsökning är helt gratis.
  - Endast vid offentlig publicering på Google Play Store tillkommer engångsavgift på 25 USD.
- **Installation på Arch Linux**: Via AUR-paketet `android-studio` eller som fristående tar.gz från Googles utvecklarportal.

## 2. RFID och "Eskortminne" (NFC)

I Android hanteras RFID-taggar (särskilt eskortminnen / smarta taggar) oftast via det inbyggda **NFC-chippet** (Near Field Communication), förutsatt att hårdvaran stödjer rätt frekvenser (vanligtvis HF / 13.56 MHz).

- **Teknologi**: Google tillhandahåller det kostnadsfria **Android NFC API**.
- **Implementation**: Används klasser som `NfcAdapter`, `Tag`, och specifika teknologiprofiler:
  - `MifareClassic`
  - `NfcA`
  - `IsoDep`
  
  Dessa låter dig läsa och skriva till specifika minnessektorer på eskortminnet.

- **UHF (Långdistans-RFID)**: Om eskortminnena kräver UHF (860–960 MHz) har Android-telefoner inte detta inbyggt. Då behövs extern RFID-läsare ansluten via Bluetooth eller USB + tillverkarens Android SDK (oftast kostnadsfri).

- **Användarkontext**: Säkerställ att mål-enheten (telefon/handdator) har ett inbyggt NFC-chip som är kompatibelt med just den typ av RFID-eskortminne som används i verksamheten.

## 3. AI-stöd i byggnationen

Denna LLM Wiki (och Grok) används som centralt verktyg för:
- Kunskapsinsamling och strukturering
- Kodgenerering och granskning
- Dokumentation av arkitektur och beslut
- Problemlösning vid NFC-integration

## Nästa Steg (typisk ordning)

1. Sätt upp utvecklingsmiljö (Arch + Android Studio)
2. Verifiera NFC-stöd på tänkta test-enheter
3. Studera Android NFC API + specifika protokoll för eskortminne
4. Bygg proof-of-concept för läsning av sektor
5. Iterera med verkliga taggar

Se även [[Projektöversikt]], [[Utvecklingsmiljö]], [[RFID-och-NFC]] och [[Eskortminne]].