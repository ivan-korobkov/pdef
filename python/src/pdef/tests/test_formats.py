# encoding: utf-8
from datetime import datetime
import unittest
from pdef import descriptors
from pdef.formats import json_format
from pdef_test import inheritance, messages


class TestJsonFormat(unittest.TestCase):
    def _test(self, descriptor, parsed, serialized):
        assert json_format.to_json(parsed, descriptor) == serialized
        assert json_format.from_json(serialized, descriptor) == parsed

        # Nulls.
        assert json_format.to_json(None, descriptor) == 'null'
        assert json_format.from_json('null', descriptor) is None

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

    def test_datetime(self):
        self._test(descriptors.datetime0, datetime(2013, 11, 17, 19, 12), '"2013-11-17T19:12:00Z"')

    def test_enum(self):
        self._test(messages.TestEnum.DESCRIPTOR, messages.TestEnum.THREE, '"three"')
        assert json_format.from_json('"tWo"', messages.TestEnum.DESCRIPTOR) == messages.TestEnum.TWO

    def test_message(self):
        message = self._complex_message()
        self._test(messages.TestComplexMessage.DESCRIPTOR, message, self.MESSAGE_JSON)

    def test_message__polymorphic(self):
        message = self._polymorphic_message()
        self._test(inheritance.Base.DESCRIPTOR, message, self.POLYMORPHIC_JSON)

    def test_message__skip_null_fields(self):
        message = messages.TestMessage(string0='hello')
        assert json_format.to_json(message, messages.TestMessage.DESCRIPTOR) == '{"string0": "hello"}'

    def test_void(self):
        self._test(descriptors.void, None, 'null')

    def _complex_message(self):
        return messages.TestComplexMessage(
            string0="hello",
            bool0=True,
            int0=32,
            short0=16,
            long0=64L,
            float0=1.5,
            double0=2.5,
            datetime0=datetime(1970, 1, 1, 0, 0, 0),
            enum0=messages.TestEnum.THREE,
            list0=[1, 2],
            set0={1, 2},
            map0={1: 1.5},
            message0=messages.TestMessage(
                string0='hello',
                bool0=True,
                int0=16),
            polymorphic=inheritance.MultiLevelSubtype(
                field='field',
                subfield='subfield',
                mfield='mfield'))

    def _polymorphic_message(self):
        return inheritance.MultiLevelSubtype(
            field='field',
            subfield='subfield',
            mfield='mfield')

    MESSAGE_JSON = '{"string0": "hello", ' \
                   '"bool0": true, ' \
                   '"int0": 32, ' \
                   '"short0": 16, ' \
                   '"long0": 64, ' \
                   '"float0": 1.5, ' \
                   '"double0": 2.5, ' \
                   '"datetime0": "1970-01-01T00:00:00Z", ' \
                   '"list0": [1, 2], ' \
                   '"set0": [1, 2], ' \
                   '"map0": {"1": 1.5}, ' \
                   '"enum0": "three", ' \
                   '"message0": {"string0": "hello", "bool0": true, "int0": 16}, ' \
                   '"polymorphic": ' \
                   '{"type": "multilevel_subtype", ' \
                   '"field": "field", "subfield": "subfield", "mfield": "mfield"}}'

    POLYMORPHIC_JSON = '{"type": "multilevel_subtype", ' \
                       '"field": "field", ' \
                       '"subfield": "subfield", ' \
                       '"mfield": "mfield"}'
