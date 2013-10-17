# encoding: utf-8
import copy
import unittest

from pdef_test.messages import TestDataTypes, TestMessage


class TestPdefMessage(unittest.TestCase):
    JSON = '''{"string0": "hello", "bool0": true}'''

    def _fixture(self):
        return TestMessage(string0="hello", bool0=True)

    def _fixture_dict(self):
        return {'string0': 'hello', 'bool0': True}

    def test_parse_json(self):
        msg = TestMessage.parse_json(self.JSON)
        assert msg == self._fixture()

    def test_parse_dict(self):
        msg = self._fixture()
        d = msg.to_dict()

        msg1 = TestMessage.parse_dict(d)
        assert msg == msg1

    def test_to_json(self):
        msg = self._fixture()
        s = msg.to_json()

        msg1 = TestMessage.parse_json(s)
        assert msg == msg1

    def test_to_dict(self):
        d = self._fixture().to_dict()

        assert d == self._fixture_dict()

    def test_eq(self):
        msg0 = self._fixture()
        msg1 = self._fixture()
        assert msg0 == msg1

        msg1.string0 = 'qwer'
        assert msg0 != msg1

    def test_copy(self):
        msg0 = TestDataTypes(string0='hello', list0=[1,2,3], message0=TestMessage('world'))
        msg1 = copy.copy(msg0)

        assert msg1 is not msg0
        assert msg1 == msg0
        assert msg1.list0 is msg0.list0
        assert msg1.message0 is msg0.message0

    def test_deepcopy(self):
        msg0 = TestDataTypes(string0='hello', list0=[1,2,3], message0=TestMessage('world'))
        msg1 = copy.deepcopy(msg0)

        assert msg1 is not msg0
        assert msg1 == msg0
        assert msg1.list0 is not msg0.list0
        assert msg1.message0 is not msg0.message0
