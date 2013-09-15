# encoding: utf-8
import unittest

from pdef_tests.messages import SimpleMessage


class TestMessage(unittest.TestCase):
    JSON = '''{"aString": "hello", "aBool": true}'''

    def _fixture(self):
        return SimpleMessage(aString="hello", aBool=True)

    def _fixture_dict(self):
        return {'aString': 'hello', 'aBool': True}

    def test_parse_json(self):
        msg = SimpleMessage.parse_json(self.JSON)
        assert msg == self._fixture()

    def test_parse_dict(self):
        msg = self._fixture()
        d = msg.to_dict()

        msg1 = SimpleMessage.parse_dict(d)
        assert msg == msg1

    def test_to_json(self):
        msg = self._fixture()
        s = msg.to_json()

        msg1 = SimpleMessage.parse_json(s)
        assert msg == msg1

    def test_to_dict(self):
        d = self._fixture().to_dict()

        assert d == self._fixture_dict()

    def test_eq(self):
        msg0 = self._fixture()
        msg1 = self._fixture()
        assert msg0 == msg1

        msg1.aString = 'qwer'
        assert msg0 != msg1
