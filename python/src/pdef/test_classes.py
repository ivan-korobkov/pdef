# encoding: utf-8
import unittest
from pdef import test_pd


class TestMessage(unittest.TestCase):
    JSON = '''{"a": "one", "b": "two"}'''

    def _fixture(self):
        return test_pd.TestMessage(a="one", b="two")

    def _fixture_dict(self):
        return {'a': 'one', 'b': 'two'}

    def test_parse_json(self):
        msg = test_pd.TestMessage.parse_json(TestMessage.JSON)
        assert msg == self._fixture()

    def test_parse_dict(self):
        msg = self._fixture()
        d = msg.to_dict()

        msg1 = test_pd.TestMessage.parse_dict(d)
        assert msg == msg1

    def test_to_json(self):
        msg = self._fixture()
        s = msg.to_json()

        msg1 = test_pd.TestMessage.parse_json(s)
        assert msg == msg1

    def test_to_dict(self):
        d = self._fixture().to_dict()

        assert d == self._fixture_dict()

    def test_merge_dict(self):
        msg = self._fixture()
        msg1 = test_pd.TestMessage()
        msg1.merge_dict(self._fixture_dict())

        assert msg == msg1

    def test_eq(self):
        msg0 = self._fixture()
        msg1 = self._fixture()
        assert msg0 == msg1

        msg1.a = 'qwer'
        assert msg0 != msg1
