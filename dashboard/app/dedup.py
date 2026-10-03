"""Dubblettskydd för inkommande MQTT-meddelanden (utkorgens at-least-once-leverans).

Android-appen skickar varje post med QoS 1 och kan skicka om samma post om bekräftelsen uteblev
(t.ex. nätverket föll efter att brokern tog emot meddelandet). Därför innehåller varje meddelande
en stabil nyckel och dashboarden ignorerar nycklar den redan sett.

Nyckel i prioritetsordning:
  1. "messageId"                      (appen: "<deviceId>:<postId>")
  2. "deviceId" + "id"                 -> "<deviceId>:<id>"
  3. enbart "id"                       -> "<uid>:<id>" (id räknas per tagg-uid)
  4. inget av ovan (äldre meddelanden) -> ingen nyckel, aldrig dubblett (bakåtkompatibelt)

Minnet är begränsat (LRU, standard 5000 nycklar ≈ några hundra kB). Listan är medvetet
bara i minnet: en omstart av dashboarden glömmer nycklarna, och en omsänd post som
kommer just efter en omstart kan då visas en gång till. Det är en avvägning för att hålla
lösningen liten (ingen fil/databas att underhålla).
"""
import threading
from collections import OrderedDict

DEFAULT_MAX_IDS = 5000


def message_key(payload, uid=""):
    """Returnerar dubblettnyckeln för en payload (dict), eller None om meddelandet saknar id."""
    if not isinstance(payload, dict):
        return None
    message_id = payload.get("messageId")
    if isinstance(message_id, str) and message_id.strip():
        return "m:" + message_id.strip()
    post_id = payload.get("id")
    if isinstance(post_id, bool) or not isinstance(post_id, (int, str)) or str(post_id).strip() == "":
        return None
    device_id = payload.get("deviceId")
    if isinstance(device_id, str) and device_id.strip():
        return f"d:{device_id.strip()}:{str(post_id).strip()}"
    return f"u:{uid}:{str(post_id).strip()}"


class SeenIds:
    """Trådsäker, storleksbegränsad (LRU) mängd av sedda nycklar."""

    def __init__(self, max_size=DEFAULT_MAX_IDS):
        if max_size < 1:
            raise ValueError("max_size måste vara >= 1")
        self._max = max_size
        self._items = OrderedDict()
        self._lock = threading.Lock()

    def check_and_add(self, key):
        """True om nyckeln redan setts (dubblett). Annars registreras den och False returneras.
        key=None (meddelande utan id) är aldrig en dubblett."""
        if key is None:
            return False
        with self._lock:
            if key in self._items:
                self._items.move_to_end(key)
                return True
            self._items[key] = True
            while len(self._items) > self._max:
                self._items.popitem(last=False)
            return False

    def __len__(self):
        with self._lock:
            return len(self._items)

    def clear(self):
        with self._lock:
            self._items.clear()
