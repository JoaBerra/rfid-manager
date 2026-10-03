"""Tester för dubblettskyddet (utkorgens omsändningar). Kräver inte paho eller broker.

Kör från dashboard/:  python3 -m unittest discover tests
"""
import json
import os
import sys
import unittest
from unittest import mock

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
for _name in ("paho", "paho.mqtt", "paho.mqtt.client"):
    sys.modules.setdefault(_name, mock.MagicMock())

from app import mqtt_client  # noqa: E402
from app.dedup import SeenIds, message_key  # noqa: E402


def _msg(payload, topic="rfidmanager/04A1/telemetry"):
    m = mock.MagicMock()
    m.topic = topic
    m.payload = json.dumps(payload).encode() if not isinstance(payload, bytes) else payload
    return m


class MessageKeyTest(unittest.TestCase):
    def test_messageId_har_företräde(self):
        self.assertEqual(message_key({"messageId": "abcd:7", "id": 7, "deviceId": "zzz"}, "U"), "m:abcd:7")

    def test_deviceId_och_id(self):
        self.assertEqual(message_key({"id": 7, "deviceId": "abcd"}, "U"), "d:abcd:7")

    def test_bara_id_räknas_per_uid(self):
        self.assertEqual(message_key({"id": 7}, "U1"), "u:U1:7")
        self.assertNotEqual(message_key({"id": 7}, "U1"), message_key({"id": 7}, "U2"))

    def test_utan_id_ingen_nyckel(self):
        self.assertIsNone(message_key({"uid": "x", "type": "ReadEscortMemory"}, "x"))

    def test_ogiltiga_id_ignoreras(self):
        for bad in (None, True, "", "  ", [], {}, 1.5):
            self.assertIsNone(message_key({"id": bad}, "U"), repr(bad))
        self.assertIsNone(message_key(["inte", "ett", "objekt"], "U"))


class SeenIdsTest(unittest.TestCase):
    def test_första_gången_inte_dubblett_sedan_dubblett(self):
        s = SeenIds(10)
        self.assertFalse(s.check_and_add("a"))
        self.assertTrue(s.check_and_add("a"))
        self.assertFalse(s.check_and_add("b"))

    def test_none_är_aldrig_dubblett_och_lagras_inte(self):
        s = SeenIds(10)
        self.assertFalse(s.check_and_add(None))
        self.assertFalse(s.check_and_add(None))
        self.assertEqual(len(s), 0)

    def test_begränsad_storlek_äldsta_glöms(self):
        s = SeenIds(3)
        for k in "abcd":
            s.check_and_add(k)
        self.assertEqual(len(s), 3)
        self.assertFalse(s.check_and_add("a"))  # a glömdes → ses som ny
        self.assertTrue(s.check_and_add("d"))

    def test_nyligen_sedd_dubblett_förlängs(self):
        s = SeenIds(3)
        for k in "abc":
            s.check_and_add(k)
        self.assertTrue(s.check_and_add("a"))  # a blir nyast
        s.check_and_add("d")                  # b (äldst) åker ut
        self.assertTrue(s.check_and_add("a"))
        self.assertFalse(s.check_and_add("b"))

    def test_ogiltig_storlek(self):
        with self.assertRaises(ValueError):
            SeenIds(0)


class OnMessageDedupTest(unittest.TestCase):
    def setUp(self):
        mqtt_client.messages.clear()
        mqtt_client.seen_ids.clear()
        mqtt_client.total_count = 0
        mqtt_client.duplicate_count = 0
        mqtt_client.unique_uids.clear()
        mqtt_client.set_notification_queue(None)

    def _base(self, **extra):
        p = {"type": "ReadEscortMemory", "uid": "04A1", "timestamp": 1700000000000, "source": "NFC",
             "sparkplug": True, "data": {"payload": "DEADBEEF"}}
        p.update(extra)
        return p

    def test_samma_messageId_två_gånger_visas_en_gång(self):
        p = self._base(id=5, deviceId="abcd", messageId="abcd:5")
        mqtt_client.on_message(None, None, _msg(p))
        mqtt_client.on_message(None, None, _msg(p))
        self.assertEqual(len(mqtt_client.get_messages()), 1)
        stats = mqtt_client.get_stats()
        self.assertEqual(stats["total"], 1)
        self.assertEqual(stats["duplicates"], 1)
        self.assertEqual(mqtt_client.get_messages()[0]["message_id"], "abcd:5")

    def test_tre_olika_poster_kommer_fram_en_gång_vardera_trots_omsändning(self):
        posts = [self._base(uid=f"T{i}", id=i, deviceId="abcd", messageId=f"abcd:{i}") for i in (1, 2, 3)]
        for p in posts + [posts[1], posts[0], posts[2], posts[2]]:  # omsändningar blandat
            mqtt_client.on_message(None, None, _msg(p, f"rfidmanager/{p['uid']}/telemetry"))
        self.assertEqual(mqtt_client.get_stats()["total"], 3)
        self.assertEqual(mqtt_client.get_stats()["duplicates"], 4)
        self.assertEqual(sorted(m["uid"] for m in mqtt_client.get_messages()), ["T1", "T2", "T3"])

    def test_samma_id_från_olika_enheter_är_inte_dubblett(self):
        mqtt_client.on_message(None, None, _msg(self._base(id=1, deviceId="aaaa", messageId="aaaa:1")))
        mqtt_client.on_message(None, None, _msg(self._base(id=1, deviceId="bbbb", messageId="bbbb:1")))
        self.assertEqual(mqtt_client.get_stats()["total"], 2)

    def test_äldre_meddelanden_utan_id_räknas_alltid_bakåtkompatibelt(self):
        p = self._base()
        mqtt_client.on_message(None, None, _msg(p))
        mqtt_client.on_message(None, None, _msg(p))
        self.assertEqual(mqtt_client.get_stats()["total"], 2)
        self.assertEqual(mqtt_client.get_stats()["duplicates"], 0)
        self.assertEqual(mqtt_client.get_messages()[0]["message_id"], "")

    def test_dubblett_skickas_inte_till_notiskön(self):
        import queue
        q = queue.Queue()
        mqtt_client.set_notification_queue(q)
        p = self._base(id=9, deviceId="abcd", messageId="abcd:9")
        mqtt_client.on_message(None, None, _msg(p))
        mqtt_client.on_message(None, None, _msg(p))
        self.assertEqual(q.qsize(), 1)

    def test_icke_json_påverkar_inte(self):
        mqtt_client.on_message(None, None, _msg(b"inte json"))
        self.assertEqual(mqtt_client.get_stats()["total"], 0)


if __name__ == "__main__":
    unittest.main()
