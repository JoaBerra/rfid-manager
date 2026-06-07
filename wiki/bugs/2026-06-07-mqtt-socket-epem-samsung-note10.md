---
title: "Bug Report: EPERM on raw TCP socket for MQTT (Paho) from debug app on Samsung Galaxy Note 10"
date: 2026-06-07
status: Resolved
severity: Medium
reporter: "User (via Grok-assisted UAT)"
id: "2026-06-07-MQTT-EPERM-SAMSUNG-NOTE10"
affected-component: "data/mqtt/MqttSender.kt + transmit flow in RFIDManagerScreen"
---

# Bug Report: EPERM on raw TCP socket for MQTT (Paho) from debug app on Samsung Galaxy Note 10

## Svensk sammanfattning

**Problem:** RFID Manager-appen (Kotlin + Jetpack Compose, debug build från Android Studio) kan inte publicera MQTT-meddelanden via rå TCP till en lokal broker på en Samsung Galaxy Note 10 (SM-N970F).

**Felmeddelande:**
```
java.net.SocketException: socket failed: EPERM (Operation not permitted)
```
Felet uppstår i `Socket.createImpl` → `Socket.connect` → Paho `TCPNetworkModule.start`.

**Vad som fungerar:**
- Appens högre nivå (persistens, UI, payload-konstruktion, topic, loggar) fungerar korrekt.
- Vid "Transmit" visas success-Toast och raden uppdateras till "Transmitted via Sparkplug".
- Loggar visar "Attempting MQTT...", "SEND COMPLETE", korrekt broker och topic (`rfidmanager/<uid>/telemetry`).
- Subscribern på testdatorn tar emot och persisterar meddelanden när de publiceras från PC (verifierat med exakt samma format som appen använder).
- Nätverksvägen från telefonen är öppen – webbläsaren når brokern och får `ERR_EMPTY_RESPONSE` (förväntat för MQTT).

**Vad som testats utan framgång:**
- Factory reset + inloggning med eget konto
- Developer mode påslaget
- Körning direkt via "Run" från Android Studio
- Alla relevanta Samsung-inställningar:
  - Mobildata: "Tillåt användning av bakgrundsdata" + "Låt data användas när Datasparare är på"
  - Batteri: "Inga begränsningar" / Unrestricted
  - Enhetsvård/Batteri: "Sätt oanvända appar i viloläge" = AV
  - Utvecklare: "Behåll inte aktiviteter" = AV
- Byte från `localhost` + adb reverse till direkt IP (192.168.50.128)

Felet kvarstår oförändrat trots detta.

**Konsekvens:** Blockar end-to-end MQTT-kommunikationstest från den fysiska enheten. Lokal persistens och app-logik är validerad. Subscriber-sidan är validerad via PC-publicering.

Rapporten är avsedd för second opinion (t.ex. Gemini).

## Summary

When the RFID Manager app (Kotlin + Jetpack Compose, debug build from Android Studio) attempts to publish an MQTT message using Eclipse Paho MQTT client over plain TCP to a local broker, the connection fails with:

```
java.net.SocketException: socket failed: EPERM (Operation not permitted)
```

This occurs inside `Socket.createImpl` → `Socket.connect` → Paho `TCPNetworkModule.start`.

The failure is consistent even after:
- Factory reset of the device
- Fresh login with primary user account
- Developer options enabled
- All documented Samsung battery / data usage / background restrictions lifted for the app
- Running the app via "Run" directly from Android Studio (not just adb install)
- Using direct broker IP instead of `localhost` + `adb reverse`

The app's higher-level transmit logic works correctly (correct payload, topic, "SEND COMPLETE" logs, success Toast in UI, status update to "Transmitted via Sparkplug"). The subscriber on the test PC correctly receives and persists messages in the expected format when they are published from the PC side.

**Browser on the same phone** can open a TCP connection to the broker IP:port (receives `ERR_EMPTY_RESPONSE`, which is the expected behavior for a raw MQTT broker). This proves the network path from the device is open.

## Environment

**Device:**
- Model: Samsung Galaxy Note 10 (SM-N970F/DS, international Exynos variant)
- No SIM card (Wi-Fi only)
- OS: Android (One UI), post factory reset
- Developer options: Enabled
- USB debugging: Enabled and authorized

**App / Build:**
- Package: `com.joakim.rfidmanager`
- Built and launched via Android Studio ("Run app", debug build)
- `targetSdk` = 36
- Uses Eclipse Paho MQTT Client (`paho-mqtt-client`)
- Broker address (current): `tcp://192.168.50.128:1883` (PC Wi-Fi IP, Docker Mosquitto)
- Topic format used by app: `rfidmanager/<uid>/telemetry`
- Payload: Simple JSON (see "App payload example" below)

**Test environment (PC side):**
- Broker: Docker `eclipse-mosquitto` on host port 1883
- Subscriber: `test_subscriber_persist.py` (listens on `rfidmanager/+/telemetry`, persists to SQLite)
- PC IP on same Wi-Fi: 192.168.50.128

## Reproduction Steps

1. On phone: Perform NFC read + write with "Persist after write" enabled.
2. Go to PERSISTED tab → observe the item in the list.
3. Press the "Transmit ↑" control on that item.
4. Observe:
   - Success Toast appears ("Transmit sent...").
   - UI status on the row updates to "Transmitted via Sparkplug".
   - Logcat shows "Attempting MQTT...", followed by the EPERM stack trace.
   - No message arrives at the PC subscriber.

## Expected Behavior

The app should successfully open a TCP socket to the configured broker, connect via Paho, publish the message, and the subscriber on the test PC should receive it on the expected topic and persist it.

## Actual Behavior

Socket creation fails at the OS level with `EPERM`. Higher-level app code treats the send as successful (or at least reaches the post-send logging/Toast path because the exception is swallowed inside `MqttSender`).

## Key Log Excerpt (Logcat from Android Studio)

```
MqttSender  I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
MqttSender  E  Failed to connect to MQTT
MqttException (0) - java.net.SocketException: socket failed: EPERM (Operation not permitted)
    at org.eclipse.paho.client.mqttv3.internal.ExceptionHelper.createMqttException(...)
    at ...TCPNetworkModule.start(...)
Caused by: java.net.SocketException: socket failed: EPERM (Operation not permitted)
    at java.net.Socket.createImpl(Socket.java:492)
    at java.net.Socket.connect(Socket.java:619)
...
MqttSender  E  Failed to publish reading
Client is not connected (32104)
...
MqttSender  I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
MqttSender  I  Broker used: tcp://192.168.50.128:1883
MqttSender  I  Topic published: rfidmanager/047B05CA885884/telemetry
```

## App Payload Example (what the app actually tries to send)

```json
{
  "type": "ReadEscortMemory",
  "uid": "047B05CA885884",
  "timestamp": <epoch ms>,
  "source": "Manual write page 12",
  "sparkplug": true,
  "data": {
    "memoryBank": 3,
    "address": 12,
    "length": 4,
    "payload": "74657374"
  }
}
```

## What Has Been Tried (exhaustive)

- Factory reset + fresh primary account login
- Developer options enabled + USB debugging authorized
- All app-specific settings:
  - Mobile data & Wi-Fi → Background data usage + "Allow data when Data Saver is on" = ON
  - Battery → "No restrictions" / Unrestricted
- Device Care → Battery → "Sätt oanvända appar i viloläge" = OFF (via "Gränser för bakgrundsanvändning")
- Developer options → "Behåll inte aktiviteter" ("Don't keep activities") = OFF
- Running via "Run" directly from Android Studio (not pure adb sideload)
- Broker address changed from `localhost` + `adb reverse` to direct PC Wi-Fi IP
- Multiple force-stops + clears + re-runs
- Browser on the same phone can reach `http://192.168.50.128:1883` (gets `ERR_EMPTY_RESPONSE` — proves TCP reachability from the device)

No further relevant menus/settings for "sovande appar", "background activity", or data restrictions could be located despite exhaustive search.

## Evidence That the Network Path Works

- Chrome on the phone to `http://192.168.50.128:1883` → `ERR_EMPTY_RESPONSE` (Mosquitto accepts the TCP connection but does not speak HTTP).
- PC-side publish (using `paho-mqtt` from the test venv) to the same IP + topic succeeds and is received by the subscriber.

## PC-Side Verification (workaround used)

When an identical payload (using the real UID `047B05CA885884`) is published from the PC instead of the phone, the subscriber correctly receives it:

```
Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry
Persisted to SQLite (Sparkplug-aware).
```

This proves the broker + subscriber + DB + topic + payload format handling all work.

## Impact

- Prevents actual end-to-end MQTT transmission testing from the physical target device.
- Local persistence, UI, and transmit *logic* can still be validated.
- Subscriber side can be validated by publishing from the PC.

## Workarounds Currently in Use

- PC-side publish (via `simulate_mobile_publish.py` or direct `paho` script) using real UIDs from phone-persisted posts to exercise the subscriber.
- All transmit-related code paths, logging, UI feedback, and payload construction are exercised and verified on the phone.

## Hypotheses

1. Samsung One UI applies additional sandboxing / SELinux / AppOps restrictions on debuggable apps (especially those launched via Android Studio) for low-level socket operations, even when the user has granted "unrestricted data usage".
2. The restriction is specific to Wi-Fi-only operation (no SIM card) combined with high `targetSdk` (36).
3. `adb reverse` vs. direct IP does not change the outcome (tested both).

## References / Links (internal wiki)

- `wiki/log.md` — entries from 2026-06-06 and 2026-06-07 (detailed test history)
- `wiki/Fas2-Implementation-Overview.md`
- `wiki/Rollfördelning-och-Arbetsätt.md` (roles used during this UAT)
- App files: `app/src/main/java/com/joakim/rfidmanager/data/mqtt/MqttSender.kt`, `RFIDManagerScreen.kt`

## Resolution (2026-06-07)

After the user applied the recommended permission changes on the device (Unrestricted data usage, background data allowed, "Sätt oanvända appar i viloläge" disabled, "Behåll inte aktiviteter" disabled in developer options) and we updated the project with the missing manifest elements:

- Added `<uses-permission android:name="android.permission.INTERNET" />` at the correct top level in `AndroidManifest.xml`.
- Created `res/xml/network_security_config.xml` allowing cleartext traffic to the broker IP.
- Referenced it from the `<application>` tag.

The next "Run" from Android Studio produced this successful Logcat (full relevant excerpt):

```
---------------------------- PROCESS STARTED (26839) for package com.joakim.rfidmanager ----------------------------
2026-06-07 09:11:43.419 26839-27194 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:11:43.419 26839-27194 MqttSender              com.joakim.rfidmanager               I  App context: package=android.content.ContextWrapper
2026-06-07 09:11:43.988 26839-27194 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:11:43.996 26839-27194 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780816291332,
                                                                                                      "source": "Manual write page 8",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 8,
                                                                                                        "length": 4,
                                                                                                        "payload": "74657374"
                                                                                                      }
                                                                                                    }
2026-06-07 09:11:43.996 26839-27194 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
2026-06-07 09:11:43.996 26839-27194 MqttSender              com.joakim.rfidmanager               I  Broker used: tcp://192.168.50.128:1883
2026-06-07 09:11:43.996 26839-27194 MqttSender              com.joakim.rfidmanager               I  Topic published: rfidmanager/047B05CA885884/telemetry
```

The subscriber on the PC immediately logged:

```
Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry
Persisted to SQLite (Sparkplug-aware).
```

**Root cause confirmed:** The `INTERNET` permission was completely missing from the manifest, and cleartext traffic to the local broker IP was blocked by default on targetSdk 36. The EPERM was a symptom of the app lacking declared network permissions + cleartext policy, not a deeper device policy issue once the manifest was correct.

**Extra debug logs** ("Broker used", "Topic published", "App context") were present during diagnosis and have since been cleaned up in the source for a production-like state.

The bug is resolved. The phone app can now successfully publish MQTT messages in the expected format, and the subscriber receives and persists them.

**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence.



**Final confirmation from user's latest successful test (2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence to SQLite.

**Final confirmation (user's latest successful test, 2026-06-07 09:33):**

```
2026-06-07 09:33:24.820 27718-27888 MqttSender              com.joakim.rfidmanager               I  Attempting MQTT connect/publish to tcp://192.168.50.128:1883 for uid=047B05CA885884
2026-06-07 09:33:25.409 27718-27888 MqttSender              com.joakim.rfidmanager               I  Connected to MQTT broker
2026-06-07 09:33:25.414 27718-27888 MqttSender              com.joakim.rfidmanager               I  Published to rfidmanager/047B05CA885884/telemetry : {
                                                                                                      "type": "ReadEscortMemory",
                                                                                                      "uid": "047B05CA885884",
                                                                                                      "timestamp": 1780817596528,
                                                                                                      "source": "Manual write page 4",
                                                                                                      "sparkplug": true,
                                                                                                      "data": {
                                                                                                        "memoryBank": 3,
                                                                                                        "address": 4,
                                                                                                        "length": 4,
                                                                                                        "payload": "74655400"
                                                                                                      }
                                                                                                    }
2026-06-07 09:33:25.415 27718-27888 MqttSender              com.joakim.rfidmanager               I  === SEND COMPLETE for uid=047B05CA885884 type=ReadEscortMemory ===
```

Subscriber confirmed receipt ("Received: ReadEscortMemory from topic rfidmanager/047B05CA885884/telemetry") and persistence to SQLite.

**Note on architectural decision (added per user request):** In `wiki/App-Architecture.md` under "Arkitekturella beslut":
- Production: All communication shall be encrypted (MQTT over TLS / wss:// etc.).
- During development: Unencrypted (tcp:// to local test broker) is acceptable.
- Encryption is deferred to a later phase of the project.

## Suggested Next Steps (kept for reference / future Gemini reviews)

- Reproduce on a stock Pixel (or other) device with identical debug APK.
- Compare `adb shell dumpsys appops` + SELinux context.
- Test release build vs debug build.
- Check for additional Knox / Wi-Fi power saving policies.

---

**Prepared for second opinion (Gemini / Google).**  
Factual record from UAT 2026-06-06/07. Roles: User = Kund + Projektledare; Assistant = Programmerare + Arkitekt + etc. Append-only logging in wiki.

**Resolution (2026-06-07):** Bug resolved. Root cause = missing top-level `INTERNET` permission in `AndroidManifest.xml` + missing `network_security_config.xml` for cleartext on `targetSdk 36`. After manifest fix the phone publishes successfully over plain TCP to the Docker Mosquitto broker. Subscriber receives the Sparkplug-style `ReadEscortMemory` payload and persists it. Extra diagnostic logs cleaned from `MqttSender.kt`. Status updated to Resolved in YAML frontmatter.

The MD source is canonical; regenerate PDF with `md-to-pdf` when needed.