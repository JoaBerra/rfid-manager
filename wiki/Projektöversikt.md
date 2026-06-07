---
title: Projektöversikt
tags: [projekt, översikt, mål]
created: 2026-05-26
---

# Projektöversikt

## Syfte

Utveckla Android-applikationer som används i **RF-ID-sammanhang**, med särskilt fokus på att läsa av **RF-ID eskortminne** — en vanlig och viktig uppgift inom industrin.

## Miljö och Teknisk Stack

- **Operativsystem / Desktop**: Arch Linux + [[Omarchy]]
- **IDE**: Android Studio (officiell, kostnadsfri)
- **Språk**: Kotlin
- **Hårdvara för RFID**: Android-enheter med inbyggt NFC-chip (13.56 MHz HF)
- **Deployment**: Sideloading av APK (helt gratis). Endast vid eventuell publicering på Google Play Store tillkommer en engångsavgift på 25 USD.

## Grundläggande Principer

- **Inga kostnader** för utvecklingsmiljö eller distribution till egna enheter.
- AI-verktyg (Grok m.fl.) används aktivt som stöd genom hela byggprocessen.
- Fokus på praktisk nytta: appar som faktiskt kan läsa och hantera eskortminnen på fältet.

## Begränsningar och Krav

- UHF-RFID (860–960 MHz) stöds inte inbyggt i vanliga Android-telefoner → kräver extern läsare via Bluetooth/USB.
- Målenheter måste ha kompatibelt NFC-chip för den specifika typ av eskortminne som används i verksamheten.

## Relaterade Sidor

- [[Projektplan]] — Den detaljerade rekommendationen och strukturerade planen.
- [[Utvecklingsmiljö]]
- [[RFID-och-NFC]]
- [[Eskortminne]]
- [[Android-NFC-API]]
- [[Kotlin-Android-NFC]] (kommer att skapas vid implementation)

## Status

Initial struktur upprättad 2026-05-26. Projektet är i planerings- och kunskapsinsamlingsfas. Redo för första källor och kodexempel.