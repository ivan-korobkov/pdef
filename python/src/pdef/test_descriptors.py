# encoding: utf-8
import unittest
from mock import Mock
from pdef import descriptors
from pdef.test import messages_pd, polymorphic_pd, interfaces_pd


class TestMessageDescriptor(unittest.TestCase):
    cls = messages_pd.SimpleMessage
    descriptor = cls.__descriptor__

    def _fixture(self):
        return self.cls(aString='hello', aBool=True, anInt16=123)

    def _fixture_dict(self):
        return {'aString': 'hello', 'aBool': True, 'anInt16': 123}

    def _fixture_json(self):
        return '{"aString": "hello", "aBool": true, "anInt16": 123}'

    def test_instance(self):
        msg = self.descriptor.instance()
        assert isinstance(msg, self.cls)

    def test_subtype(self):
        subtype = self.descriptor.subtype(None)
        assert subtype is self.cls

    def test_to_object(self):
        msg = self._fixture()
        d = self.descriptor.to_object(msg)
        assert d == self._fixture_dict()

    def test_to_object__none(self):
        assert self.descriptor.to_object(None) is None

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
    descriptor = polymorphic_pd.Base.__descriptor__
    def test_subtype(self):
        d = self.descriptor

        assert d.subtype(polymorphic_pd.PolymorphicType.SUBTYPE) is polymorphic_pd.Subtype
        assert d.subtype(polymorphic_pd.PolymorphicType.SUBTYPE2) is polymorphic_pd.Subtype2
        assert d.subtype(polymorphic_pd.PolymorphicType.MULTILEVEL_SUBTYPE) \
            is polymorphic_pd.MultiLevelSubtype

    def test_parse_object(self):
        subtype_d = {'type': 'subtype', 'subfield': 'hello'}
        subtype2_d = {'type': 'subtype2', 'subfield2': 'hello'}
        mlevel_subtype_d = {'type': 'multilevel_subtype', 'mfield': 'hello'}

        d = self.descriptor
        assert d.parse_object(subtype_d) == polymorphic_pd.Subtype(subfield='hello')
        assert d.parse_object(subtype2_d) == polymorphic_pd.Subtype2(subfield2='hello')
        assert d.parse_object(mlevel_subtype_d) == polymorphic_pd.MultiLevelSubtype(mfield='hello')


class TestInterfaceDescriptor(unittest.TestCase):
    descriptor = interfaces_pd.TestInterface.__descriptor__

    def test_exc(self):
        assert self.descriptor.exc is interfaces_pd.TestException

    def test_methods(self):
        assert  len(self.descriptor.methods) == 7


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


class TestPrimitiveDescriptor(unittest.TestCase):
    descriptor = descriptors.bool0

    def parse(self):
        assert self.descriptor.parse_object(True) is True

    def parse__none(self):
        assert self.descriptor.parse_object(None) is None

    def parse__string(self):
        assert self.descriptor.parse_object('TrUe') is True

    def parse_string__none(self):
        assert self.descriptor.parse_string(None) is None

    def serialize(self):
        assert self.descriptor.to_object(True) is True

    def serialize__none(self):
        assert self.descriptor.to_object(None) is None

    def serialize_to_string(self):
        assert self.descriptor.to_string(True) == 'True'

    def serialize_to_string__none(self):
        assert self.descriptor.to_string(None) is None


class TestEnumDescriptor(unittest.TestCase):
    cls = messages_pd.TestEnum
    descriptor = cls.__descriptor__

    def test_parse(self):
        enum = self.descriptor.parse_object('one')
        assert enum == self.cls.ONE

    def test_parse__none(self):
        assert self.descriptor.parse_object(None) is None

    def test_parse_string(self):
        assert self.descriptor.parse_string('TwO') == self.cls.TWO

    def test_parse_string__none(self):
        assert self.descriptor.parse_string(None) is None

    def test_serialize(self):
        assert self.descriptor.to_object(self.cls.THREE) == 'three'

    def test_serialize__none(self):
        assert self.descriptor.to_object(None) is None

    def test_serialize_to_string(self):
        assert self.descriptor.to_string(self.cls.THREE) == 'three'

    def test_serialize_to_string__none(self):
        assert self.descriptor.to_string(None) is None


class TestListDescriptor(unittest.TestCase):
    descriptor = descriptors.list0(descriptors.int32)

    def test_parse(self):
        assert self.descriptor.parse_object(['1', 2]) == [1, 2]

    def test_parse__none(self):
        assert self.descriptor.parse_object(None) is None

    def test_serialize(self):
        assert self.descriptor.to_object([1, 2]) == [1, 2]

    def test_serialize__none(self):
        assert self.descriptor.to_object(None) is None


class TestSetDescriptor(unittest.TestCase):
    descriptor = descriptors.set0(descriptors.int32)

    def test_parse(self):
        assert self.descriptor.parse_object(['1', 2, '2']) == {1, 2}

    def test_parse__none(self):
        assert self.descriptor.parse_object(None) is None

    def test_serialize(self):
        assert self.descriptor.to_object({1, 2}) == {1, 2}

    def test_serialize__none(self):
        assert self.descriptor.to_object(None) is None


class TestMapDescriptor(unittest.TestCase):
    descriptor = descriptors.map0(descriptors.int32, descriptors.int32)

    def test_parse(self):
        assert self.descriptor.parse_object({'1': '2'}) == {1: 2}

    def test_parse__none(self):
        assert self.descriptor.parse_object(None) is None

    def test_serialize(self):
        assert self.descriptor.to_object({1: 2}) == {1: 2}

    def test_serialize__none(self):
        assert self.descriptor.to_object(None) is None
