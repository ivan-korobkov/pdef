# encoding: utf-8
import unittest
from pdef import test_pd


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
        msg1 = d.parse(msg.to_dict())
        assert msg == msg1

    def test_parse__none(self):
        d = self.descriptor
        assert d.parse(None) is None

    def test_serialize(self):
        descriptor = self.descriptor
        msg = test_pd.TestMessage()
        d = descriptor.serialize(msg)
        assert d == {}


class TestFieldDescriptor(unittest.TestCase):
    field = test_pd.TestMessage.__descriptor__.fields[0]

    def test_type(self):
        assert self.field.descriptor is test_pd.TestEnum.__descriptor__

    def test_is_set(self):
        msg = test_pd.TestMessage()
        assert not self.field.is_set(msg)

        msg.anEnum = test_pd.TestEnum.TWO
        assert self.field.is_set(msg)

    def test_set(self):
        msg = test_pd.TestMessage()
        self.field.set(msg, test_pd.TestEnum.THREE)
        assert msg.anEnum == test_pd.TestEnum.THREE

    def test_clear(self):
        msg = test_pd.TestMessage()
        msg.anEnum = test_pd.TestEnum.THREE
        self.field.clear(msg)

        assert msg.anEnum is None


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
        pass

    def test_is_remote__datatype(self):
        pass

    def test_is_remote__void(self):
        pass

    def test_is_remote__interface(self):
        pass

    def test_invoke(self):
        pass


class TestPrimitiveDescriptor(unittest.TestCase):
    pass


class TestEnumDescriptor(unittest.TestCase):
    pass


class TestListDescriptor(unittest.TestCase):
    pass


class TestSetDescriptor(unittest.TestCase):
    pass


class TestMapDescriptor(unittest.TestCase):
    pass
