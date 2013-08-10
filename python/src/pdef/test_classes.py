# encoding: utf-8
import unittest
from pdef import test_pd


class TestMessage(unittest.TestCase):
    JSON = '''{
        "anEnum": "one",
        "aBool": true,
        "anInt16": 1,
        "anInt32": 2,
        "anInt64": 3,
        "aFloat": 1.5,
        "aDouble": 2.5,
        "aString": "hello",
        "aList": ["a", "b"],
        "aSet": ["a", "a", "b"],
        "aMap": {"key": "value"},
        "aMessage": {},
        "anObject": [1, 2, 3]
    }'''

    def _fixture(self):
        return test_pd.TestMessage(
            anEnum=test_pd.TestEnum.ONE,
            aBool=True,
            anInt16=1,
            anInt32=2,
            anInt64=3,
            aFloat=1.5,
            aDouble=2.5,
            aString='hello',
            aList=['a', 'b'],
            aSet={'a', 'b'},
            aMap={'key': 'value'},
            aMessage=test_pd.TestMessage(),
            anObject=[1, 2, 3])

    def _fixture_dict(self):
        return {
            'anEnum': 'one',
            'aBool': True,
            'anInt16': 1,
            'anInt32': 2,
            'anInt64': 3,
            'aFloat': 1.5,
            'aDouble': 2.5,
            'aString': 'hello',
            'aList': ['a', 'b'],
            'aSet': {'a', 'b'},
            'aMap': {'key': 'value'},
            'aMessage': {},
            'anObject': [1, 2, 3]
        }

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

        msg1.anEnum = test_pd.TestEnum.THREE
        assert msg0 != msg1


class TestMessageInheritance(unittest.TestCase):
    def test_parse__submessage_wo_base_type(self):
        d = {'firstField': True, 'secondField': 'hello', 'forthField': 1.5}
        msg = test_pd.TestSimpleSubmessage.parse_dict(d)
        expected = test_pd.TestSimpleSubmessage(
            firstField=True,
            secondField='hello',
            forthField=1.5)

        assert msg == expected

    def test_parse__submessage_with_base_type(self):
        d = {'type': 'one'}
        msg = test_pd.Tree0.parse_dict(d)
        expected = test_pd.Tree1(type=test_pd.TreeType.ONE)

        print msg.to_dict(), type(msg)
        print expected.to_dict(), type(expected)
        assert msg == expected

    def test_parse__submessage_type_tree(self):
        d = {'type': 'two', 'type1': 'b'}
        msg = test_pd.Tree0.parse_dict(d)
        expected = test_pd.TreeB(type=test_pd.TreeType.TWO, type1=test_pd.TreeType1.B)
        assert msg == expected


class TestEnum(unittest.TestCase):
    def test_parse_json(self):
        s = '"thREE"'
        enum = test_pd.TestEnum.parse_json(s)
        assert enum == test_pd.TestEnum.THREE

    def test_parse_string(self):
        s = 'TWO'
        enum = test_pd.TestEnum.parse_string(s)
        assert enum == test_pd.TestEnum.TWO

    def test_parse_string__none(self):
        enum = test_pd.TestEnum.parse_string(None)
        assert enum is None
