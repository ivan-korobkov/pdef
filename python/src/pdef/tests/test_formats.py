# encoding: utf-8
import unittest
from pdef import descriptors
from pdef.formats import json
from pdef_test import inheritance, messages


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
        self._test(messages.TestDataTypes.DESCRIPTOR, message, self.MESSAGE_JSON)

    def test_message__polymorphic(self):
        message = self._polymorphic_message()
        self._test(inheritance.Base.DESCRIPTOR, message, self.POLYMORPHIC_JSON)

    def test_message__skip_null_fields(self):
        message = messages.TestMessage(string0='hello')
        assert json.serialize(message, messages.TestMessage.DESCRIPTOR) == '{"string0": "hello"}'

    def test_void(self):
        self._test(descriptors.void, None, 'null')

    def _complex_message(self):
        return messages.TestDataTypes(
            string0="hello",
            bool0=True,
            short0=16,
            enum0=messages.TestEnum.THREE,
            int0=32,
            long0=64L,
            float0=1.5,
            double0=2.5,
            list0=[1, 2],
            set0={1, 2},
            map0={1: 1.5},
            message0=messages.TestMessage(
                string0='hello',
                bool0=True,
                short0=16),
            polymorphic=inheritance.MultiLevelSubtype(
                field='field',
                subfield='subfield',
                mfield='mfield'))

    def _polymorphic_message(self):
        return inheritance.MultiLevelSubtype(
            field='field',
            subfield='subfield',
            mfield='mfield')

    MESSAGE_JSON = u'{"string0": "hello", ' \
                   u'"bool0": true, ' \
                   u'"short0": 16, ' \
                   u'"int0": 32, ' \
                   u'"long0": 64, ' \
                   u'"float0": 1.5, ' \
                   u'"double0": 2.5, ' \
                   u'"list0": [1, 2], ' \
                   u'"set0": [1, 2], ' \
                   u'"map0": {"1": 1.5}, ' \
                   u'"enum0": "three", ' \
                   u'"message0": {"string0": "hello", "bool0": true, "short0": 16}, ' \
                   u'"polymorphic": ' \
                   u'{"type": "multilevel_subtype", ' \
                   u'"field": "field", "subfield": "subfield", "mfield": "mfield"}}'

    POLYMORPHIC_JSON = u'{"type": "multilevel_subtype", ' \
                       u'"field": "field", ' \
                       u'"subfield": "subfield", ' \
                       u'"mfield": "mfield"}'
