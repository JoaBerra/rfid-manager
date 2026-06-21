---
title: Fas-101 — MQTT-klientkonfiguration i appen
tags: [mqtt, fas-101, configuration, settings, network, security]
created: 2026-06-14
---

# Fas-101 — MQTT-klientkonfiguration i appen

> **Fas-101** — Fullständig MQTT-konfiguration direkt i RFIDManager-appen.
> USB (`adb reverse`) är endast för utveckling. I produktion kommunicerar telefonen över WiFi till brokern.
> Appen behöver därför en komplett konfigurationsvy i Settings för alla MQTT-parametrar.

---

## Innehållsförteckning

1. [Anslutning](#1-anslutning)
2. [Autentisering](#2-autentisering)
3. [TLS/SSL](#3-tlsssl)
4. [Sparkplug-specifikt](#4-sparkplug-specifikt)
5. [Topics](#5-topics)
6. [Beteende](#6-beteende)
7. [Verifiering och felsökning](#7-verifiering-och-felsökning)

---

## 1. Anslutning

Grundläggande parametrar för att nå brokern över nätverket.

| Parameter | Default | Förklaring |
|-----------|---------|------------|
| **Broker host** | `192.168.50.128` | IP-adress eller hostnamn för MQTT-brokern |
| **Broker port** | `1883` | TCP-port (1883 = okrypterat, 8883 = TLS) |
| **Client ID** | `rfidmanager-<slump>` | Unikt ID så brokern känner igen klienten vid återanslutning |

> **Utökas:** När vi börjar arbeta med denna punkt lägger vi till förklaringar om varför Client ID är viktigt för sessionstillstånd, hur man väljer hostnamn vs IP, och portarnas betydelse.

---

## 2. Autentisering

Parametrar för att verifiera klientens identitet mot brokern.

| Parameter | Default | Förklaring |
|-----------|---------|------------|
| **Username** | (tom) | Användarnamn för MQTT-autentisering |
| **Password** | (tom) | Lösenord för MQTT-autentisering |

> **Utökas:** När vi börjar arbeta med denna punkt lägger vi till förklaringar om hur MQTT-autentisering fungerar, skillnad mot TLS, rekommendationer för lösenordshantering.

---

## 3. TLS/SSL

Kryptering och certifikatshantering.

| Parameter | Default | Förklaring |
|-----------|---------|------------|
| **TLS på/av** | `Av` | Aktivera TLS-kryptering |
| **TLS-version** | `1.2` | TLS-protokollversion (1.2 eller 1.3) |
| **CA-certifikat** | (filväljare) | För att verifiera serverns identitet |
| **Klientcertifikat** | (filväljare) | Krävs vid mutual TLS (mTLS) |
| **Klientnyckel** | (filväljare) | Privat nyckel för klientcertifikatet |
| **Verifiera servernamn** | `På` | Kontrollera att certifikatets CN/SNI matchar host |

> **Utökas:** När vi börjar arbeta med denna punkt lägger vi till förklaringar om TLS-handskakning, skillnad mellan one-way och mutual TLS, hur man genererar självsignerade certifikat för test, var certifikat lagras säkert på Android.

---

## 4. Sparkplug-specifikt

Parametrar för Sparkplug B-namnområdet och payload-strukturen.

| Parameter | Default | Förklaring |
|-----------|---------|------------|
| **Group ID** | `rfidmanager` | Översta nivån i Sparkplug-hierarkin, identifierar system/grupp |
| **Edge Node ID** | (autogen) | Identifierar denna telefon som en edge-node i nätverket |
| **Device ID** | (taggens UID) | Identifierar den specifika enheten (eskortminnet) |
| **Sparkplug-version** | `B` | Protokollversion för meddelandeformatet |

> **Utökas:** När vi börjar arbeta med denna punkt lägger vi till förklaringar om Sparkplug-hierarkin (Group / Edge Node / Device), hur NBIRTH/NDATA/NCMD fungerar, och varför detta är viktigt för industriell interoperabilitet.

---

## 5. Topics

Möjlighet att avvika från standard-topic-strukturen.

| Parameter | Default | Förklaring |
|-----------|---------|------------|
| **Base topic** | `spBv1.0/<Group>/<EdgeNode>` | Rot för alla topics (följer Sparkplug-konvention) |
| **Telemetry topic** | (bas + `/NDATA/<Device>`) | Topic för utgående mätdata |
| **Command topic** | (bas + `/NCMD/<Device>`) | Topic för inkommande kommandon |

> **Utökas:** När vi börjar arbeta med denna punkt lägger vi till förklaringar om Sparkplug B topic-namnområde, wildcards, hur man designar en topic-struktur för flera enheter.

---

## 6. Beteende

Parametrar som styr hur MQTT-klienten uppför sig.

| Parameter | Default | Förklaring |
|-----------|---------|------------|
| **QoS** | `1` | Quality of Service (0 = fire-and-forget, 1 = minst en gång, 2 = exakt en gång) |
| **Retain** | `Av` | Behåll senaste meddelandet på brokern för nya prenumeranter |
| **Keep-alive (s)** | `30` | Intervall för ping för att upprätthålla anslutningen |
| **Clean session** | `På` | Börja med ren session vid anslutning (spara inte tidigare meddelanden) |
| **Auto-reconnect** | `På` | Återanslut automatiskt vid tappad anslutning |
| **Reconnect delay (s)** | `5` | Fördröjning mellan återanslutningsförsök |
| **Max reconnect attempts** | `0` (obegränsat) | Max antal återanslutningsförsök innan vi ger upp |

### Will message (Last Will & Testament)

Meddelande som brokern skickar om appen oväntat kopplar från.

| Parameter | Default | Förklaring |
|-----------|---------|------------|
| **Will topic** | (tom) | Topic för will-meddelandet |
| **Will payload** | (tom) | Innehåll i will-meddelandet (t.ex. `{"status": "offline"}`) |
| **Will QoS** | `1` | QoS-nivå för will-meddelandet |
| **Will retain** | `På` | Behåll will-meddelandet som sista kända status |

> **Utökas:** När vi börjar arbeta med denna punkt lägger vi till förklaringar om QoS-nivåernas trade-offs, vad retain innebär för återanslutande klienter, varför will message är viktigt för driftsövervakning, och hur exponential backoff fungerar för reconnect.

---

## 7. Verifiering och felsökning

Hjälpmedel för att testa att konfigurationen fungerar.

| Funktion | Förklaring |
|----------|------------|
| **Testa anslutning** | Knapp som försöker ansluta till brokern med aktuella inställningar och visar resultat |
| **Anslutningsstatus** | Indikator i appen som visar uppkopplingsstatus i realtid |
| **Sista fel** | Visar senaste anslutningsfelet för felsökning |

> **Utökas:** När vi börjar arbeta med denna punkt lägger vi till förklaringar om vanliga felmeddelanden och deras betydelse, hur man testar anslutningen med externa verktyg.

---

## Relation till Fas-100

- **Fas-100** är den teoretiska fördjupningen: protokollet, topologi, verktyg, säkerhet.
- **Fas-101** är den praktiska implementationen: alla konfigurationsparametrar som skall in i appens Settings-skärm.

Tillsammans ger de en komplett bild — Fas-100 förklarar *varför*, Fas-101 bygger *hur*.

---

## Status

**Skapad:** 2026-06-14  
**Fas:** Planerad (förslag till Kund)  
**Nästa steg:** Godkänn fasomfattning + börja implementera parametrar i Settings-skärmen
