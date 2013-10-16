# encoding: utf-8
import unittest
from pdef import descriptors
from pdef.formats import native, json
from pdef_test import inheritance, interfaces, messages


class TestJsonFormat(unittest.TestCase):
    def _test(self, descriptor, parsed, serialized):
        assert json.serialize(parsed, descriptor) == serialized
        assert json.parse(serialized, descriptor) == parsed

        # Nulls.
        assert json.serialize(None, descriptor) == 'null'
        assert json.parse('null', descriptor) is None

    def test_boolean(self):
        self._test(descriptors.bool0, True, 'true')
        self._test(descriptors.bool0, False, 'false')

    def test_int16(self):
        self._test(descriptors.int16, -16, '-16')

    def test_int32(self):
        self._test(descriptors.int32, -32, '-32')

    def test_int64(self):
        self._test(descriptors.int64, -64, '-64')

    def test_float(self):
        self._test(descriptors.float0, -1.5, '-1.5')

    def test_double(self):
        self._test(descriptors.double0, -2.5, '-2.5')

    def test_string(self):
        self._test(descriptors.string0, '123', '"123"')
        self._test(descriptors.string0, u'привет', u'"привет"')

    def test_enum(self):
        self._test(messages.TestEnum.DESCRIPTOR, messages.TestEnum.THREE, '"three"')
        assert json.parse('"tWo"', messages.TestEnum.DESCRIPTOR) == messages.TestEnum.TWO

    def test_message(self):
        message = self._complex_message()
        self._test(messages.ComplexMessage.DESCRIPTOR, message, self.MESSAGE_JSON)

    def test_message__polymorphic(self):
        message = self._polymorphic_message()
        self._test(inheritance.Base.DESCRIPTOR, message, self.POLYMORPHIC_JSON)

    def test_message__skip_null_fields(self):
        message = messages.SimpleMessage(aString='hello')
        assert json.serialize(message, messages.SimpleMessage.DESCRIPTOR) == '{"aString": "hello"}'

    def test_void(self):
        self._test(descriptors.void, None, 'null')

    def _complex_message(self):
        return messages.ComplexMessage(
            aString="hello",
            aBool=True,
            anInt16=16,
            anEnum=messages.TestEnum.THREE,
            anInt32=32,
            anInt64=64L,
            aFloat=1.5,
            aDouble=2.5,
            aList=[1, 2],
            aSet={1, 2},
            aMap={1: 1.5},
            aMessage=messages.SimpleMessage(
                aString='hello',
                aBool=True,
                anInt16=16),
            aPolymorphicMessage=inheritance.MultiLevelSubtype(
                field='field',
                subfield='subfield',
                mfield='mfield'))

    def _polymorphic_message(self):
        return inheritance.MultiLevelSubtype(
            field='field',
            subfield='subfield',
            mfield='mfield')

    MESSAGE_JSON = u'{"aString": "hello", ' \
                   u'"aBool": true, ' \
                   u'"anInt16": 16, ' \
                   u'"anInt32": 32, ' \
                   u'"anInt64": 64, ' \
                   u'"aFloat": 1.5, ' \
                   u'"aDouble": 2.5, ' \
                   u'"aList": [1, 2], ' \
                   u'"aSet": [1, 2], ' \
                   u'"aMap": {"1": 1.5}, ' \
                   u'"anEnum": "three", ' \
                   u'"aMessage": {"aString": "hello", "aBool": true, "anInt16": 16}, ' \
                   u'"aPolymorphicMessage": ' \
                   u'{"type": "multilevel_subtype", ' \
                   u'"field": "field", "subfield": "subfield", "mfield": "mfield"}}'

    POLYMORPHIC_JSON = u'{"type": "multilevel_subtype", ' \
                       u'"field": "field", ' \
                       u'"subfield": "subfield", ' \
                       u'"mfield": "mfield"}'
