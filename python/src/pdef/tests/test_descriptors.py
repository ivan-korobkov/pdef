# encoding: utf-8
import unittest
from mock import Mock

from pdef import descriptors
from pdef_test import messages, inheritance, interfaces


class TestMessageDescriptor(unittest.TestCase):
    cls = messages.SimpleMessage
    descriptor = cls.DESCRIPTOR

    def _fixture(self):
        return self.cls(aString='hello', aBool=True, anInt16=123)

    def _fixture_dict(self):
        return {'aString': 'hello', 'aBool': True, 'anInt16': 123}

    def _fixture_json(self):
        return '{"aString": "hello", "aBool": true, "anInt16": 123}'

    def test_subtype(self):
        subtype = self.descriptor.subtype(None)
        assert subtype is self.cls

    def test_to_object(self):
        msg = self._fixture()
        d = self.descriptor.to_object(msg)
        assert d == self._fixture_dict()

    def test_to_object__none(self):
        assert self.descriptor.to_object(None) is None

    def test_to_object__check_type(self):
        msg = self._fixture()
        msg.aString = True

        self.assertRaises(TypeError, self.descriptor.to_object, msg)

    def test_parse_object(self):
        d = self._fixture_dict()
        msg = self.descriptor.parse_object(d)
        assert msg == self._fixture()

    def test_parse_object__none(self):
        assert self.descriptor.parse_object(None) is None

    def test_parse_json(self):
        s = self._fixture_json()
        msg = self.descriptor.parse_json(s)
        assert msg == self._fixture()

    def test_to_json(self):
        msg = self._fixture()
        s = self.descriptor.to_json(msg)
        msg1 = self.descriptor.parse_json(s)
        assert msg == msg1


class TestPolymorphicMessageDescriptor(unittest.TestCase):
    descriptor = inheritance.Base.DESCRIPTOR

    def test_subtype(self):
        d = self.descriptor

        assert d.subtype(inheritance.PolymorphicType.SUBTYPE) is inheritance.Subtype
        assert d.subtype(inheritance.PolymorphicType.SUBTYPE2) is inheritance.Subtype2
        assert d.subtype(inheritance.PolymorphicType.MULTILEVEL_SUBTYPE) \
            is inheritance.MultiLevelSubtype

    def test_parse_object(self):
        subtype_d = {'type': 'subtype', 'subfield': 'hello'}
        subtype2_d = {'type': 'subtype2', 'subfield2': 'hello'}
        mlevel_subtype_d = {'type': 'multilevel_subtype', 'mfield': 'hello'}

        d = self.descriptor
        assert d.parse_object(subtype_d) == inheritance.Subtype(subfield='hello')
        assert d.parse_object(subtype2_d) == inheritance.Subtype2(subfield2='hello')
        assert d.parse_object(mlevel_subtype_d) == inheritance.MultiLevelSubtype(mfield='hello')


class TestFieldDescriptor(unittest.TestCase):
    cls = messages.SimpleMessage
    descriptor = cls.DESCRIPTOR
    field = descriptor.find_field('aString')

    def test_set(self):
        msg = self.cls()
        self.field.set(msg, 'hello')
        assert msg.aString == 'hello'

    def test_set__check_type(self):
        msg = self.cls()
        self.assertRaises(TypeError, self.field.set, msg, 123)

    def test_get(self):
        msg = self.cls(aString='hello')
        assert self.field.get(msg) == 'hello'

    def test_get__check_type(self):
        msg = self.cls(aString=123)
        self.assertRaises(TypeError, self.field.get, msg)


class TestInterfaceDescriptor(unittest.TestCase):
    descriptor = interfaces.TestInterface.DESCRIPTOR

    def test_exc(self):
        assert self.descriptor.exc is interfaces.TestException.DESCRIPTOR

    def test_methods(self):
        assert len(self.descriptor.methods) == 9


class TestMethodDescriptor(unittest.TestCase):
    def test_result(self):
        method = descriptors.method('method', lambda: descriptors.void)
        assert method.result is descriptors.void

    def test_is_remote__datatype(self):
        method = descriptors.method('method', lambda: descriptors.string)
        assert method.is_remote

    def test_is_remote__void(self):
        method = descriptors.method('method', lambda: descriptors.void)
        assert method.is_remote

    def test_is_remote__interface(self):
        method = descriptors.method('method', lambda: descriptors.interface(object))
        assert method.is_remote is False

    def test_invoke(self):
        service = Mock()
        method = descriptors.method('method', lambda: descriptors.void)
        method.invoke(service)
        service.method.assert_called_with()


class TestPrimitiveDescriptors(unittest.TestCase):
    def _test(self, descriptor, string_to_parse, expected, expected_string):
        assert descriptor.parse_object(string_to_parse) == expected
        assert descriptor.parse_object(expected) == expected
        assert descriptor.parse_object(None) is None
        assert descriptor.to_string(None) is None
        assert descriptor.to_string(expected) == expected_string

    def test_bool(self):
        self._test(descriptors.bool0, 'FALSE', False, 'false')
        self._test(descriptors.bool0, 'TrUE', True, 'true')

    def test_int16(self):
        self._test(descriptors.int16, '16', 16, '16')

    def test_int32(self):
        self._test(descriptors.int32, '32', 32, '32')

    def test_int64(self):
        self._test(descriptors.int64, '64', 64, '64')

    def test_float(self):
        self._test(descriptors.float0, '1.5', 1.5, '1.5')

    def test_double(self):
        self._test(descriptors.double0, '2.5', 2.5, '2.5')

    def test_string(self):
        self._test(descriptors.string, 'hello', 'hello', 'hello')


class TestEnumDescriptor(unittest.TestCase):
    descriptor = messages.TestEnum.DESCRIPTOR

    def _test(self, descriptor, objectToParse, expected, expected_object):
        assert descriptor.parse_object(objectToParse) == expected
        assert descriptor.parse_object(None) is None
        assert descriptor.to_object(None) is None
        assert descriptor.to_object(expected) == expected_object

    def test(self):
        self._test(self.descriptor, 'one', messages.TestEnum.ONE, 'one')
        self._test(self.descriptor, 'TWO', messages.TestEnum.TWO, 'two')
        self._test(self.descriptor, 'three', messages.TestEnum.THREE, 'three')

    def test_serialize_to_string(self):
        assert self.descriptor.to_string(messages.TestEnum.THREE) == 'three'

    def test_serialize_to_string__none(self):
        assert self.descriptor.to_string(None) is None


class AbstractDataDescriptorTest(unittest.TestCase):
    def _test(self, descriptor, objectToParse, expected):
        assert descriptor.parse_object(objectToParse) == expected
        assert descriptor.parse_object(None) is None
        assert descriptor.to_object(None) is None
        assert descriptor.to_object(expected) == expected


class TestListDescriptor(AbstractDataDescriptorTest):
    descriptor = descriptors.list0(descriptors.int32)

    def test(self):
        self._test(self.descriptor, ['1', '2', None], [1, 2, None])


class TestSetDescriptor(AbstractDataDescriptorTest):
    descriptor = descriptors.set0(descriptors.int32)

    def test(self):
        self._test(self.descriptor, {'1', '2', None}, {1, 2, None})


class TestMapDescriptor(AbstractDataDescriptorTest):
    descriptor = descriptors.map0(descriptors.int32, descriptors.int32)

    def test(self):
        self._test(self.descriptor, {'1': '2', '3': None}, {1: 2, 3: None})
