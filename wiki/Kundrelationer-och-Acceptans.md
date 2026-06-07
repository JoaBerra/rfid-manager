---
title: Kundrelationer och Acceptans
tags: [kund, uat, acceptans, sign-off, godkännande, projektstyrning, governance, roller]
created: 2026-06-07
---

# Kundrelationer och Acceptans – RFID Manager

> **Syfte**: Skapa en tydlig, tidsstämplad och spårbar dokumentation av relationen till Kund, utförda UAT-tester i kundrollen samt formella godkännanden (sign-off) per fas eller leverans.  
> Detta är **Single Source of Truth** för vad som har accepterats av Kund och under vilka förutsättningar.

Denna sida kompletterar [[Rollfördelning-och-Arbetsätt]] där rollerna **Kund / Product Owner** och **UAT-testare & Slutanvändare** är explicit definierade och ägda av dig.

## Roller gentemot Kund

| Roll                        | Ägare     | Ansvar i relation till Kund                          |
|-----------------------------|-----------|-------------------------------------------------------|
| **Kund / Product Owner**    | Du (Joa)  | Krav, acceptanskriterier, prioritering, UAT           |
| **UAT-testare & Slutanvändare** | Du (Joa) | Manuell acceptanstest på riktig hårdvara i verklig miljö |
| **Projektledare**           | Du (Joa)  | Scope, faser, Go/No-go-beslut, sign-off               |
| **Domänexpert**             | Du (Joa)  | Industriell kontext (eskortminnen, processer, säkerhet) |
| **Programmerare m.fl.**     | Grok      | Tekniskt genomförande + dev-test (stödjer UAT)        |

Se full RACI och Working Agreement i [[Rollfördelning-och-Arbetsätt]].

**Viktigt signalord**: När du säger "Jag agerar kund nu" eller "Detta var ett UAT-test" så är det en explicit rollväxling som dokumenteras här.

## Arbetsätt för UAT och Formella Godkännanden

1. **Förberedelse** — Grok (i rollerna Testare/DevOps/Programmerare) bygger, testar internt, fixar blockerare och dokumenterar i wiki + buggrapporter.
2. **UAT-utförande** — Du kliver in i Kund-rollen och testar på fysisk enhet (Samsung Note 10 etc.). Du noterar vad som fungerar, vad som inte gör det, och ger affärsmässig feedback.
3. **Dokumentation** — Resultat, loggar, referenser till buggrapporter och kod läggs in här eller i länkade sidor.
4. **Sign-off** — Du (som Kund/PL) ger ett formellt godkännande eller "godkänd med öppna punkter". Detta tidsstämplas och signeras i sektionen nedan.
5. **Fas-gate** — Godkännandet blir underlag för beslut om nästa fas eller iteration.

All dokumentation är **append-only** eller versionshanterad via log.md för maximal spårbarhet (viktigt vid second opinion, framtida teammedlemmar eller revision).

## Sign-off Logg

### [2026-06-07] UAT | Fas 2 godkänd av Kund

**Datum:** 2026-06-07  
**Tid / Kontext:** Slutligt UAT-test utfört ~09:33 (Logcat-tid). Godkännande efter full verifiering av end-to-end-flöde samma dag.

**Utfört av:** Kund (Joa) i egenskap av **UAT-testare & Slutanvändare** + **Projektledare**

**Fas / Leverans:**  
**Fas 2** – Lokal persistens av RFID-läsningar + MQTT/Sparkplug-kommunikation (telemetri till extern mottagare)

**Testmiljö:**
- Enhet: Samsung Galaxy Note 10 (SM-N970F/DS, internationell Exynos)
- App: RFID Manager (debug build byggd i Android Studio, targetSdk 36)
- Nätverk: Lokal Wi-Fi, broker på 192.168.50.128:1883 (Docker eclipse-mosquitto)
- Valideringssida: Python subscriber (`test_subscriber_persist.py`) som lyssnar på `rfidmanager/+/telemetry` och persisterar till SQLite

**Omfattning – Vad testades i UAT (Kund-perspektiv):**
- "Persist after write" på NFC Write (läsningar sparas lokalt i appen)
- PersistedReadings-lista med status, metadata och Transmit ↑-knapp
- Transmit-flöde: App → MqttSender → rå TCP MQTT → broker
- Meddelandeformat (Sparkplug-liknande): `type`, `uid`, `timestamp`, `source`, `sparkplug: true`, `data` med `memoryBank`, `address`, `length`, `payload` (hex)
- Topic: `rfidmanager/<uid>/telemetry`
- End-to-end validering: Subscriber tar emot meddelandet korrekt och sparar det i testdatabasen
- Hantering av tidigare blockerare (EPERM på Samsung debug builds)

**Kritiskt validerat testfall (09:33 samma dag):**
- UID: `047B05CA885884`
- memoryBank: 3, address: 4, length: 4, payload: `74655400`
- Logcat (MqttSender):
  - "Attempting MQTT connect/publish..."
  - "Connected to MQTT broker"
  - "Published to rfidmanager/047B05CA885884/telemetry : { ... }"
  - "=== SEND COMPLETE for uid=... ==="
- Subscriber-sida: "Received: ReadEscortMemory from topic ... Persisted to SQLite (Sparkplug-aware)."

**Resultat:**
UAT godkänt. Alla kritiska flöden för Fas 2-plattformen fungerade som förväntat på riktig hårdvara efter nödvändiga korrigeringar (manifest + network security config).

Se full historik, alla tidigare försök, Samsung-inställningar och Logcat i:
[[bugs/2026-06-07-mqtt-socket-epem-samsung-note10]] (inkl. genererad PDF för second opinion)

**Öppna punkter / Kvar i Fas 2 vid godkännande:**
- EAN-streckkodsläsning via kamera (enligt ursprunglig Fas 2-mål)
- ViewModel-refaktor för bättre state-hantering (istället för direkt i Screen)
- Reaktivera riktig Room-databas (KSP) istället för in-memory fallback
- Ytterligare polish, felhantering och UX-förbättringar
- Kryptering: **Arkitekturbeslut** – Produktion skall använda krypterad kommunikation (MQTT over TLS). Under utveckling är okrypterad (tcp://) godkänd.

**Godkännande:**

**UAT godkänd av Kund för Fas 2.**

Fas 2-leveransen (lokal persistens + MQTT/Sparkplug-meddelanden) anses komplett och accepterad i sin kärna. Projektet kan fortsätta med återstående arbetsuppgifter i Fas 2 samt planering av Fas 3.

**Signerat av Kund:** Joa  
**Datum:** 2026-06-07

**Referenser:**
- Buggrapport (kanonisk källa + PDF): `wiki/bugs/2026-06-07-mqtt-socket-epem-samsung-note10.md`
- Rollfördelning: [[Rollfördelning-och-Arbetsätt]]
- Arkitekturbeslut (kryptering): [[App-Architecture]]
- Teknisk översikt: [[Fas2-Implementation-Overview]]
- Kronologisk logg: [[log]]

---

## Mall för framtida UAT / Sign-off

Kopiera och fyll i nedanstående struktur vid varje formellt godkännande.

```markdown
### [YYYY-MM-DD] UAT | <Fas / Leverans> – <Godkänd / Godkänd med anmärkning / Ej godkänd>

**Datum:** ...  
**Tid / Kontext:** ...

**Utfört av:** Kund (...) i egenskap av UAT-testare & Slutanvändare

**Fas / Leverans:** ...

**Testmiljö:** ...

**Omfattning:**  
- ...

**Resultat:** ...

**Öppna punkter vid godkännande:**  
- ...

**Godkännande:**  
UAT godkänd / ... av Kund för ...

**Signerat av Kund:** ___________________________   **Datum:** YYYY-MM-DD

**Referenser:** ...
```

## Rekommendationer för framtida arbete

- Varje ny Fas eller större leverans bör ha ett motsvarande UAT + sign-off här.
- Använd alltid explicit roll ("Som Kund godkänner jag...").
- Länka alltid till teknisk dokumentation + eventuella buggrapporter.
- Uppdatera denna sida + `log.md` + relevant översiktssida (t.ex. Fas2-Implementation-Overview) vid varje sign-off.

---

**Skapad:** 2026-06-07  
**Ägare:** Kund (du har sista ordet)  
**Uppdateras:** Vid varje UAT eller formellt godkännande

Se även:
- [[Rollfördelning-och-Arbetsätt]] (roller och working agreement)
- [[log]] (append-only kronologi)
- [[bugs/README]] (struktur för formella felrapporter)
