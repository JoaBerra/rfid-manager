---
title: UAT — fakir smoke test (Galaxy Note 10)
tags: [uat, smoke-test, fakir, note10, mqtt, nfc]
created: 2026-07-12
updated: 2026-07-12
---

# UAT — fakir smoke test (Galaxy Note 10)

**Uppdrag:** [002](https://github.com/JoaBerra/andra-hjarna/blob/main/Bearbetning/2026-07-12-rfid-manager-smoke-test-uppdrag.md)  
**Datum:** 2026-07-12  
**Testare:** Principalen (Sigge)  
**Dokumentation:** Grok (fakir)  
**Syfte:** Revalidera Uppdrag 001 dev-baseline mot fysisk enhet.

## Miljö

| Fält | Värde |
|------|-------|
| Dev-host | fakir (Arch, `~/Projects/rfid-manager`) |
| Testenhet | Samsung Galaxy Note 10 (SM-N970F) |
| Nätverk | `192.168.50.0/24` (Wi-Fi) |
| MQTT-broker | **ishtar** `192.168.50.151:1883` *(via app Settings; Uppdrag 003)* |
| App default broker | falstaff `192.168.50.107` *(oförändrad — Uppdrag 004)* |
| Hypervisor | hulda Proxmox `192.168.50.100` |

## Resultat per kriterium

| # | Kriterium | Resultat | Notis |
|---|-----------|----------|-------|
| 1 | `adb devices` → `device` | **PASS** *(Principal)* | App installerad och körd under testperioden |
| 2 | `installDebug` exit 0 | **PASS** *(Principal)* | APK på enheten under MQTT-test |
| 3 | App startar utan krasch | **PASS** *(Principal)* | — |
| 4 | MQTT CONNECTED | **PASS** *(Principal)* | Connectivity visar ansluten mot `192.168.50.151` (Settings) |
| 5 | NFC-tagg detekterad | **Ej verifierad** | Ej krävd för minimum (4 uppfyllt) |
| 6 | Denna rapport i wiki | **PASS** | — |
| 7 | Ingen produktkod i AH | **PASS** | — |

**Minimum för godkännande:** kriterier 1–3 och 4 — **uppfyllt**.

## Testsekvens (genomförd)

### Förberedelse

1. Uppdrag 003: MQTT-broker på ishtar (`rfid-mqtt-hulda`, Docker)
2. Note 10 på Wi-Fi som når `192.168.50.151`
3. Broker-IP konfigurerad i app Settings → `192.168.50.151`

### Principal (enhet)

1. Öppna RFID Manager
2. Connectivity → **MQTT ansluten** ✅ (bekräftat 2026-07-12)

### Agent (fakir)

- Broker verifierad: `mosquitto_pub -h 192.168.50.151 -p 1883` (Uppdrag 003)
- `ssh hulda` + `docker ps` — container `rfid-mqtt-hulda` Up

## Avvikelser / noteringar

| Punkt | Beskrivning |
|-------|-------------|
| Broker-IP | Smoke test mot **ishtar** (`.151`), inte falstaff (`.107`) — medvetet via Settings tills Uppdrag 004 |
| NFC | Ej testad i denna session — rekommenderas vid nästa UAT eller separat körning |
| ADB på fakir | `adb` ej i PATH i agent-session; enhetsverifiering via Principal |

## Slutsats

**Uppdrag 002 — GODKÄND** (Principal 2026-07-12). Fakir-baseline fungerar på Galaxy Note 10 med MQTT mot ishtar-testlabbet.

## Relaterat

- [[Testmiljo-hulda]] — broker-drift
- [[Utvecklingsmiljö-fakir]] — dev-host
- [[Hardware-Testenheter]] — Note 10