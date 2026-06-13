---
title: "Connectivity — no detail view on message tap"
date: 2026-06-13
status: Resolved
severity: Medium
reporter: "Kund (Joa) via UAT"
id: "BUG-001"
affected-component: "ui/screens/MqttStatusScreen.kt"
kanban-board: "Kanban.md"
kanban-column: "Blockerat"
---

# Bug: Connectivity — detaljvy visas inte vid tryck på meddelande

**ID:** BUG-001  
**Rapporterad:** 2026-06-13 av Kund (Joa) via UAT  
**Status:** Open  
**Severity:** Medium  

---

## Sammanfattning (svenska)

I Connectivity-vyn står det i användarmanualen att man ska kunna trycka på ett meddelande för att se fullständig detalj (inklusive hela JSON-payload). Detta fungerar inte — korten har ingen click-handler.

## Steps to Reproduce

1. Öppna appen och gå till **Connectivity**-vyn
2. Tryck på valfritt meddelande i listan
3. **Förväntat:** En dialog eller sheet visas med fullständig information om meddelandet (UID, type, timestamp, source, dataPreview, memoryBank, address, length, payload, sparkplugJson)
4. **Faktiskt:** Inget händer — korten är inte klickbara

## Environment

- **Device:** Samsung Galaxy Note 10 (SM-N970F/DS)
- **Android:** 12
- **App version:** Debug build 2026-06-13
- **Component:** `ui/screens/MqttStatusScreen.kt`

## Affected Code

`MqttStatusScreen.kt:119-171` — `Card`-komponenten i LazyColumn har ingen `Modifier.clickable` eller annan interaktion. Korten är rena display-kort.

## Root Cause

Funktionen "tryck för att se detalj" nämns i användarmanualen men har aldrig implementerats i koden. `onClick` saknas på `Card`.

## Proposed Fix

Lägg till:
1. `var selectedReading by remember { mutableStateOf<PersistedReading?>(null) }` för att hålla state för vilket kort som är valt
2. En `AlertDialog` som visas när `selectedReading != null`
3. `Modifier.clickable { selectedReading = reading }` på varje `Card`
4. Dialogen visar alla fält: uidOrCode, type, timestamp, source, dataPreview, memoryBank, address, length, payload, sparkplugJson, correlationId, transmitted-status

## Workaround

Ingen — detaljinformationen är inte tillgänglig via UI:t.

## Resolution (2026-06-13)

**Fix deployed:** Varje meddelandekort i Connectivity-vyn har nu `Modifier.clickable` som öppnar en `AlertDialog` med all metadata:
- uidOrCode, type, timestamp (formaterad), source, dataPreview, transmitted-status
- memoryBank, address, length, payload, correlationId, sparkplugJson (full JSON)

**Files changed:**
- `ui/screens/MqttStatusScreen.kt` — la till `selectedReading` state, `clickable` modifier på Card, `AlertDialog` för detaljer, `DetailRow`-komponent

**Build:** `assembleDebug` — BUILD SUCCESSFUL  
**Deployed:** `adb install` — Success

**Status:** Resolved — UAT godkänd av Kund 2026-06-13.
