"""Torrkörning av MQTT-inloggningslogiken (kräver inte paho eller broker).

Kör från dashboard/:  python3 -m unittest discover tests
"""
import os
import sys
import unittest
from unittest import mock

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
for _name in ("paho", "paho.mqtt", "paho.mqtt.client"):
    sys.modules.setdefault(_name, mock.MagicMock())

from app import mqtt_client  # noqa: E402


class ConfigureAuthTest(unittest.TestCase):
    def _run(self, env):
        client = mock.MagicMock()
        with mock.patch.dict(os.environ, env, clear=True):
            result = mqtt_client.configure_auth(client)
        return result, client

    def test_anonymt_utan_username(self):
        result, client = self._run({})
        self.assertFalse(result)
        client.username_pw_set.assert_not_called()

    def test_password_ensamt_ignoreras(self):
        result, client = self._run({"MQTT_PASSWORD": "x"})
        self.assertFalse(result)
        client.username_pw_set.assert_not_called()

    def test_inloggning_med_username(self):
        result, client = self._run({"MQTT_USERNAME": "u", "MQTT_PASSWORD": "p"})
        self.assertTrue(result)
        client.username_pw_set.assert_called_once_with("u", "p")

    def test_lösenord_loggas_inte(self):
        with self.assertLogs("dashboard.mqtt", level="INFO") as cm:
            self._run({"MQTT_USERNAME": "u", "MQTT_PASSWORD": "hemligt123"})
        self.assertNotIn("hemligt123", "\n".join(cm.output))


if __name__ == "__main__":
    unittest.main()
