# encoding: utf-8
import copy
import unittest

from pdef_test.messages import SimpleMessage, ComplexMessage


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

    def test_copy(self):
        msg0 = ComplexMessage(aString='hello', aList=[1,2,3], aMessage=SimpleMessage('world'))
        msg1 = copy.copy(msg0)

        assert msg1 is not msg0
        assert msg1 == msg0
        assert msg1.aList is msg0.aList
        assert msg1.aMessage is msg0.aMessage

    def test_deepcopy(self):
        msg0 = ComplexMessage(aString='hello', aList=[1,2,3], aMessage=SimpleMessage('world'))
        msg1 = copy.deepcopy(msg0)

        assert msg1 is not msg0
        assert msg1 == msg0
        assert msg1.aList is not msg0.aList
        assert msg1.aMessage is not msg0.aMessage
