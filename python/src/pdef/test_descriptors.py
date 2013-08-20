# encoding: utf-8
import unittest
from mock import Mock
from pdef import descriptors, test_pd


class TestMessageDescriptor(unittest.TestCase):
    descriptor = test_pd.TestMessage.__descriptor__

    def test_instance(self):
        d = self.descriptor
        msg = d.instance()
        assert isinstance(msg, test_pd.TestMessage)

    def test_subtype__no_subtypes(self):
        d = test_pd.Tree0.__descriptor__
        subtype = d.subtype(test_pd.TreeType.ONE)
        assert subtype is test_pd.Tree1

    def test_parse(self):
        d = self.descriptor
        msg = test_pd.TestMessage()
        msg1 = d.parse_object(msg.to_dict())
        assert msg == msg1

    def test_parse__none(self):
        d = self.descriptor
        assert d.parse_object(None) is None

    def test_serialize(self):
        descriptor = self.descriptor
        msg = test_pd.TestMessage()
        d = descriptor.to_object(msg)
        assert d == {}


class TestFieldDescriptor(unittest.TestCase):
    field = test_pd.TestMessage.__descriptor__.fields[0]

    def test_type(self):
        assert self.field.type is test_pd.TestEnum.__descriptor__

    def test_get_set(self):
        msg = test_pd.TestMessage()
        self.field.set(msg, test_pd.TestEnum.THREE)
        assert msg.anEnum == test_pd.TestEnum.THREE


class TestInterfaceDescriptor(unittest.TestCase):
    descriptor = test_pd.InterfaceTree1.__descriptor__

    def test_base(self):
        assert self.descriptor.base is test_pd.InterfaceTree0

    def test_exc(self):
        assert self.descriptor.exc is test_pd.TestException1

    def test_declared_methods(self):
        assert len(self.descriptor.declared_methods) == 1

    def test_inherited_methods(self):
        assert len(self.descriptor.inherited_methods) == 1

    def test_methods(self):
        assert  len(self.descriptor.methods) == 2


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
        assert self.descriptor.serialize_to_string(True) == 'True'

    def serialize_to_string__none(self):
        assert self.descriptor.serialize_to_string(None) is None


class TestEnumDescriptor(unittest.TestCase):
    descriptor = test_pd.TestEnum.__descriptor__

    def test_parse(self):
        enum = self.descriptor.parse_object('one')
        assert enum == test_pd.TestEnum.ONE

    def test_parse__none(self):
        assert self.descriptor.parse_object(None) is None

    def test_parse_string(self):
        assert self.descriptor.parse_string('TwO') == test_pd.TestEnum.TWO

    def test_parse_string__none(self):
        assert self.descriptor.parse_string(None) is None

    def test_serialize(self):
        assert self.descriptor.to_object(test_pd.TestEnum.THREE) == 'three'

    def test_serialize__none(self):
        assert self.descriptor.to_object(None) is None

    def test_serialize_to_string(self):
        assert self.descriptor.serialize_to_string(test_pd.TestEnum.THREE) == 'three'

    def test_serialize_to_string__none(self):
        assert self.descriptor.serialize_to_string(None) is None


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
